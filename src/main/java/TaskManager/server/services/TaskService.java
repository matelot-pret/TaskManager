package TaskManager.server.services;

import TaskManager.server.cache.TaskCache;
import TaskManager.shared.DAOs.DAOCollaborator;
import TaskManager.shared.DAOs.DAOTask;
import TaskManager.shared.DAOs.Database;
import TaskManager.shared.exceptions.AlreadyExistsException;
import TaskManager.shared.models.Collaborator;
import TaskManager.shared.models.Task;
import TaskManager.shared.models.TaskState;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class TaskService {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final DAOTask daoTask                 = new DAOTask();
    private final DAOCollaborator daoCollaborator = new DAOCollaborator();
    private final TaskCache cache;

    public TaskService(TaskCache cache) {
        this.cache = cache;
    }

    /**
     * Find a task by id.
     * Checks cache first — falls back to database on cache miss.
     * @param taskId the task id
     * @return the Task, or null if not found
     * @throws SQLException if the database could not be reached
     */
    public Task findTask(int taskId) throws SQLException {
        Task cached = cache.getById(taskId);
        if (cached != null) return cached;
        Task task = daoTask.find(taskId);
        if (task != null) cache.put(task);
        return task;
    }

    /**
     * Retrieve all tasks.
     * Checks cache first — falls back to database on cache miss.
     * @return a Set of all Task objects
     * @throws SQLException if the database could not be reached
     */
    public Set<Task> findAllTasks() throws SQLException {
        Set<Task> cached = cache.getAll();
        if (cached != null) return cached;
        Set<Task> tasks = daoTask.findAll();
        cache.putAll(tasks);
        return tasks;
    }

    /**
     * Create a new task.
     * The DAO create() call (INSERT + upsertElapsedTime) is wrapped in a transaction —
     * if any step fails the entire operation is rolled back.
     * Invalidates the full task list cache after successful creation.
     * @param description the task description
     * @param echeanceStr the deadline as "yyyy-MM-dd'T'HH:mm"
     * @param creatorId the id of the creator collaborator
     * @return the created Task with its generated id
     * @throws AlreadyExistsException if an identical task already exists
     * @throws SQLException if the database could not be reached
     * @pre creatorId must correspond to an existing Collaborator
     * @post a Task is inserted in the database and the cache is invalidated
     */
    public Task createTask(String description, String echeanceStr, int creatorId)
            throws AlreadyExistsException, SQLException {
        Collaborator creator = daoCollaborator.find(creatorId);
        if (creator == null)
            throw new NoSuchElementException("[ERROR] No collaborator with id " + creatorId);

        LocalDateTime echeance = LocalDateTime.parse(echeanceStr, FORMATTER);
        Task task = new Task(description, echeance, creator);

        Connection connection = Database.getConnection();
        connection.setAutoCommit(false);
        try {
            daoTask.create(task);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }

        cache.invalidateAll();
        return task;
    }

    /**
     * Update the state and current worker of a task.
     * Handles startTime and elapsedTime automatically :
     * - PROGRESSING : sets startTime = now, sets currentWorker
     * - Other states : computes elapsed minutes from startTime,
     *   adds to collaboratorElapsedTime map, clears startTime and currentWorker
     * The DAO update() call is wrapped in a transaction.
     * Invalidates the cache for the modified task after update.
     *
     * @param taskId the task id
     * @param stateId the new state id
     * @param currentWorkerId the collaborator id to set as currentWorker, or null
     * @throws SQLException if the database could not be reached
     * @throws NoSuchElementException if task or collaborator not found
     * @pre taskId must correspond to an existing Task
     * @post task is updated in database, elapsed time computed if applicable, cache invalidated
     */
    public void updateTaskState(int taskId, int stateId, Integer currentWorkerId)
            throws SQLException, NoSuchElementException {
        Task task = daoTask.find(taskId);
        if (task == null)
            throw new NoSuchElementException("[ERROR] No task with id " + taskId);

        TaskState newState = TaskState.fromValue(stateId);

        if (newState == TaskState.PROGRESSING) {
            if (currentWorkerId == null)
                throw new NoSuchElementException("[ERROR] currentWorkerId required for PROGRESSING");
            Collaborator worker = daoCollaborator.find(currentWorkerId);
            if (worker == null)
                throw new NoSuchElementException("[ERROR] No collaborator with id " + currentWorkerId);
            task.setState(newState);
            task.setCurrentWorker(worker);
            task.setStartTime(LocalDateTime.now());
        } else {
            computeAndSaveElapsedTime(task);
            task.setState(newState);
            task.setStartTime(null);
            task.setCurrentWorker(null);
        }

        Connection connection = Database.getConnection();
        connection.setAutoCommit(false);
        try {
            daoTask.update(task);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }

        cache.invalidate(taskId);
    }

    /**
     * Pause all tasks currently worked on by a specific collaborator.
     * Called when a collaborator disconnects to save their elapsed time.
     * Each pause triggers updateTaskState which computes elapsed time.
     * Invalidates the full cache after all tasks are paused.
     * @param collaboratorId the id of the disconnecting collaborator
     * @throws SQLException if the database could not be reached
     * @post all PROGRESSING tasks of this collaborator are set to PAUSED,
     *       elapsed time computed and saved, startTime and currentWorker cleared
     */
    public void pauseAllTasksForCollaborator(int collaboratorId) throws SQLException {
        Set<Task> allTasks = daoTask.findAll();
        for (Task task : allTasks) {
            if (task.getCurrentWorker() != null
                    && task.getCurrentWorker().getId() == collaboratorId
                    && task.getState() == TaskState.PROGRESSING) {
                try {
                    updateTaskState(task.getId(), TaskState.PAUSED.getValue(), null);
                } catch (NoSuchElementException e) {
                    System.err.println("[TaskService] Erreur pause tâche "
                            + task.getId() + " : " + e.getMessage());
                }
            }
        }
        cache.invalidateAll();
    }

    /**
     * Delete a task.
     * Invalidates the cache after deletion.
     * @param taskId the task id
     * @throws SQLException if the database could not be reached
     * @throws NoSuchElementException if no task found with this id
     */
    public void deleteTask(int taskId) throws SQLException, NoSuchElementException {
        Task task = daoTask.find(taskId);
        if (task == null)
            throw new NoSuchElementException("[ERROR] No task with id " + taskId);
        daoTask.delete(task);
        cache.invalidate(taskId);
    }

    // -------------------------------------------------------------------------
    // Calcul du temps
    // -------------------------------------------------------------------------

    /**
     * Compute elapsed time for the current worker and add it to the task's map.
     * Called before any state transition away from PROGRESSING (pause, close, block, cancel).
     * Formula : elapsedMinutes = now - startTime
     * The result is added to the existing elapsed time for this collaborator on this task.
     *
     * @param task the task being transitioned away from PROGRESSING
     * @pre task.getStartTime() != null and task.getCurrentWorker() != null
     * @post task.getCollaboratorElapsedTime() is updated with the new elapsed time
     */
    private void computeAndSaveElapsedTime(Task task) {
        if (task.getStartTime() == null || task.getCurrentWorker() == null)
            return;

        long elapsedSeconds = ChronoUnit.SECONDS.between(
                task.getStartTime(), LocalDateTime.now());

        // Minimum 1 minute si au moins 1 seconde a été travaillée
        long elapsedMinutes = elapsedSeconds > 0
                ? Math.max(1L, (long) Math.ceil(elapsedSeconds / 60.0))
                : 0L;

        Collaborator worker = task.getCurrentWorker();
        long previous       = task.getCollaboratorElapsedTime().getOrDefault(worker, 0L);
        task.getCollaboratorElapsedTime().put(worker, previous + elapsedMinutes);

        System.out.println("[Timer] " + worker.getFirstName() + " " + worker.getLastName()
                + " a travaillé " + elapsedSeconds + "s → " + elapsedMinutes + " min"
                + " sur tâche " + task.getId()
                + " (total : " + (previous + elapsedMinutes) + " min)");
    }
}