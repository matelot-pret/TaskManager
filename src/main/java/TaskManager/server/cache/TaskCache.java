package TaskManager.server.cache;

import TaskManager.shared.models.Task;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache for Task objects.
 * Avoids redundant database queries for frequently accessed data.
 *
 * Invalidation strategy : every write operation (create, update, delete)
 * must call invalidate() or invalidateAll() to keep the cache consistent.
 * This is intentionally simple — no TTL, no eviction policy.
 *
 * Thread-safe : uses ConcurrentHashMap.
 */
@Component
public class TaskCache {

    /** Cache of taskId -> Task */
    private final Map<Integer, Task> taskById = new ConcurrentHashMap<>();

    /** Cached result of findAll() — null means not yet cached */
    private volatile Set<Task> allTasks = null;

    // -------------------------------------------------------------------------
    // Lecture
    // -------------------------------------------------------------------------

    /**
     * Get a task from cache by id.
     * @param id the task id
     * @return the cached Task, or null if not in cache
     */
    public Task getById(int id) {
        return taskById.get(id);
    }

    /**
     * Get the cached full task list.
     * @return the cached Set of all tasks, or null if not yet cached
     */
    public Set<Task> getAll() {
        return allTasks;
    }

    // -------------------------------------------------------------------------
    // Écriture
    // -------------------------------------------------------------------------

    /**
     * Store a task in cache.
     * @param task the Task to cache
     */
    public void put(Task task) {
        taskById.put(task.getId(), task);
    }

    /**
     * Store the full task list in cache.
     * @param tasks the Set of all tasks to cache
     */
    public void putAll(Set<Task> tasks) {
        allTasks = tasks;
        tasks.forEach(t -> taskById.put(t.getId(), t));
    }

    // -------------------------------------------------------------------------
    // Invalidation
    // -------------------------------------------------------------------------

    /**
     * Invalidate the cache entry for a specific task.
     * Also clears the allTasks cache since it is now stale.
     * Must be called after any create, update or delete operation.
     * @param taskId the id of the task that was modified
     */
    public void invalidate(int taskId) {
        taskById.remove(taskId);
        allTasks = null;
    }

    /**
     * Invalidate the entire cache.
     * Called when a batch operation affects multiple tasks.
     */
    public void invalidateAll() {
        taskById.clear();
        allTasks = null;
    }
}