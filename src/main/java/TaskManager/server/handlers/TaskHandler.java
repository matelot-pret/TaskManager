package TaskManager.server.handlers;

import TaskManager.server.handlers.TaskWebSocketHandler;
import TaskManager.server.services.CollaboratorService;
import TaskManager.server.services.TaskService;
import TaskManager.shared.exceptions.AlreadyExistsException;
import TaskManager.shared.models.Collaborator;
import TaskManager.shared.models.Task;
import TaskManager.shared.models.TaskState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

@Component
public class TaskHandler {

    private final TaskService taskService;
    private final CollaboratorService collaboratorService;
    private final TaskWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    public TaskHandler(TaskService taskService,
                       CollaboratorService collaboratorService,
                       @Lazy TaskWebSocketHandler webSocketHandler) {
        this.taskService = taskService;
        this.collaboratorService = collaboratorService;
        this.webSocketHandler = webSocketHandler;
    }

    /**
     * Route the action to the appropriate handler method.
     * @param session the session of the client who sent the message
     * @param action the action string read from the JSON message
     * @param json the full parsed JSON message
     * @throws IOException if the response could not be sent
     */
    public void handle(WebSocketSession session, String action, JsonNode json) throws IOException {
        switch (action) {
            case "GET_TASKS"      -> handleGetTasks(session);
            case "GET_TASK"       -> handleGetTask(session, json);
            case "GET_DASHBOARD"  -> handleGetDashboard(session);
            case "CREATE_TASK"    -> handleCreateTask(session, json);
            case "UPDATE_TASK"    -> handleUpdateTask(session, json);
            case "DELETE_TASK"    -> handleDeleteTask(session, json);
            case "ASSIGN_TASK",
                 "ASSIGN_BY_NAME" -> handleAssignTask(session, json);
        }
    }

    // -------------------------------------------------------------------------
    // Requêtes de lecture
    // -------------------------------------------------------------------------

    /**
     * Handle a GET_TASKS action.
     * Returns all tasks except canceled ones.
     * Expects  : { "action": "GET_TASKS" }
     * Responds : { "action": "TASKS_DATA", "tasks": [...] }
     * @param session the session of the requesting client
     * @throws IOException if the response could not be sent
     */
    private void handleGetTasks(WebSocketSession session) throws IOException {
        try {
            Set<Task> tasks = taskService.findAllTasks();
            ObjectNode response = objectMapper.createObjectNode();
            response.put("action", "TASKS_DATA");
            response.set("tasks", buildTasksArray(tasks));
            sendResponse(session, response);
        } catch (SQLException e) {
            sendResponse(session, buildError("GET_TASKS", "Erreur base de données : " + e.getMessage()));
        }
    }

    /**
     * Handle a GET_TASK action.
     * Returns the full data of a single task.
     * Expects  : { "action": "GET_TASK", "taskId": N }
     * Responds : { "action": "TASK_DATA", "task": {...} }
     * @param session the session of the requesting client
     * @param json the parsed JSON message
     * @throws IOException if the response could not be sent
     */
    private void handleGetTask(WebSocketSession session, JsonNode json) throws IOException {
        try {
            int taskId = json.get("taskId").asInt();
            Task task = taskService.findTask(taskId);
            if (task == null) {
                sendResponse(session, buildError("GET_TASK", "Tâche introuvable."));
                return;
            }
            ObjectNode response = objectMapper.createObjectNode();
            response.put("action", "TASK_DATA");
            response.set("task", buildTaskNode(task));
            sendResponse(session, response);
        } catch (SQLException e) {
            sendResponse(session, buildError("GET_TASK", "Erreur base de données : " + e.getMessage()));
        }
    }

    /**
     * Handle a GET_DASHBOARD action.
     * Returns late tasks, task counts by status, and total elapsed time per collaborator.
     * Expects  : { "action": "GET_DASHBOARD" }
     * Responds : { "action": "DASHBOARD_DATA",
     *              "lateTasks": [...],
     *              "statusCounts": { "UNOPENED": N, ... },
     *              "collaborators": [...] }
     * @param session the session of the requesting client
     * @throws IOException if the response could not be sent
     */
    private void handleGetDashboard(WebSocketSession session) throws IOException {
        try {
            Set<Task> allTasks    = taskService.findAllTasks();
            Set<Collaborator> all = collaboratorService.findAll();

            ObjectNode response = objectMapper.createObjectNode();
            response.put("action", "DASHBOARD_DATA");

            // Tâches en retard — échéance dépassée et non clôturée/annulée
            ArrayNode lateTasks = objectMapper.createArrayNode();
            LocalDateTime now = LocalDateTime.now();
            for (Task t : allTasks) {
                boolean isLate = t.getEcheance().isBefore(now)
                        && t.getState() != TaskState.CLOSED
                        && t.getState() != TaskState.CANCELED;
                if (isLate) {
                    ObjectNode node = objectMapper.createObjectNode();
                    node.put("id", t.getId());
                    node.put("description", t.getDescription());
                    node.put("echeance", t.getEcheance().format(FORMATTER));
                    lateTasks.add(node);
                }
            }
            response.set("lateTasks", lateTasks);

            // Comptage par statut
            ObjectNode statusCounts = objectMapper.createObjectNode();
            for (TaskState state : TaskState.values())
                statusCounts.put(state.name(), 0);
            for (Task t : allTasks) {
                String key = t.getState().name();
                statusCounts.put(key, statusCounts.get(key).asInt() + 1);
            }
            response.set("statusCounts", statusCounts);

            // Temps total par collaborateur
            ArrayNode collabArray = objectMapper.createArrayNode();
            for (Collaborator c : all) {
                long totalMinutes = 0;
                for (Task t : allTasks) {
                    Long minutes = t.getCollaboratorElapsedTime().get(c);
                    if (minutes != null) totalMinutes += minutes;
                }
                ObjectNode node = objectMapper.createObjectNode();
                node.put("id", c.getId());
                node.put("firstName", c.getFirstName());
                node.put("lastName", c.getLastName());
                node.put("totalMinutes", totalMinutes);
                collabArray.add(node);
            }
            response.set("collaborators", collabArray);

            sendResponse(session, response);
        } catch (SQLException e) {
            sendResponse(session, buildError("GET_DASHBOARD", "Erreur base de données : " + e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // Actions d'écriture
    // -------------------------------------------------------------------------

    /**
     * Handle a CREATE_TASK action.
     * Creates a new task and broadcasts it to all other connected clients.
     * Expects  : { "action": "CREATE_TASK", "description": "...",
     *              "echeance": "2026-05-14T09:00", "creatorId": N }
     * Responds : { "action": "CREATE_TASK", "success": true/false, "taskId": N }
     * Broadcasts to others : same response
     * @param session the session of the requesting client
     * @param json the parsed JSON message
     * @throws IOException if the response could not be sent
     */
    private void handleCreateTask(WebSocketSession session, JsonNode json) throws IOException {
        try {
            Task task = taskService.createTask(
                    json.get("description").asText(),
                    json.get("echeance").asText(),
                    json.get("creatorId").asInt()
            );
            ObjectNode response = buildTaskResponse("CREATE_TASK", true, "Tâche créée.", task.getId());
            sendResponse(session, response);
            webSocketHandler.broadcast(response, session.getId());
        } catch (AlreadyExistsException e) {
            sendResponse(session, buildError("CREATE_TASK", "Cette tâche existe déjà."));
        } catch (SQLException e) {
            sendResponse(session, buildError("CREATE_TASK", "Erreur base de données : " + e.getMessage()));
        }
    }

    /**
     * Handle an UPDATE_TASK action.
     * Updates the state and current worker of a task and broadcasts the change.
     * Expects  : { "action": "UPDATE_TASK", "taskId": N, "stateId": N,
     *              "currentWorkerId": N or null }
     * Responds : { "action": "UPDATE_TASK", "success": true/false, "taskId": N }
     * Broadcasts to others : same response
     * @param session the session of the requesting client
     * @param json the parsed JSON message
     * @throws IOException if the response could not be sent
     */
    private void handleUpdateTask(WebSocketSession session, JsonNode json) throws IOException {
        try {
            int taskId = json.get("taskId").asInt();
            int stateId = json.get("stateId").asInt();
            Integer currentWorkerId = json.has("currentWorkerId") && !json.get("currentWorkerId").isNull()
                    ? json.get("currentWorkerId").asInt() : null;

            taskService.updateTaskState(taskId, stateId, currentWorkerId);
            ObjectNode response = buildTaskResponse("UPDATE_TASK", true, "Tâche mise à jour.", taskId);
            sendResponse(session, response);
            webSocketHandler.broadcast(response, session.getId());
        } catch (SQLException | NoSuchElementException e) {
            sendResponse(session, buildError("UPDATE_TASK", e.getMessage()));
        }
    }

    /**
     * Handle a DELETE_TASK action.
     * Deletes a task and broadcasts the deletion to all other clients.
     * Expects  : { "action": "DELETE_TASK", "taskId": N }
     * Responds : { "action": "DELETE_TASK", "success": true/false, "taskId": N }
     * Broadcasts to others : same response
     * @param session the session of the requesting client
     * @param json the parsed JSON message
     * @throws IOException if the response could not be sent
     */
    private void handleDeleteTask(WebSocketSession session, JsonNode json) throws IOException {
        try {
            int taskId = json.get("taskId").asInt();
            taskService.deleteTask(taskId);
            ObjectNode response = buildTaskResponse("DELETE_TASK", true, "Tâche supprimée.", taskId);
            sendResponse(session, response);
            webSocketHandler.broadcast(response, session.getId());
        } catch (SQLException | NoSuchElementException e) {
            sendResponse(session, buildError("DELETE_TASK", e.getMessage()));
        }
    }

    /**
     * Handle an ASSIGN_TASK action.
     * Assigns a collaborator to a task by updating its currentWorker.
     * Expects  : { "action": "ASSIGN_TASK", "taskId": N, "collaboratorId": N or null }
     * Responds : { "action": "UPDATE_TASK", "success": true/false, "taskId": N }
     * Broadcasts to others : same response
     * @param session the session of the requesting client
     * @param json the parsed JSON message
     * @throws IOException if the response could not be sent
     */
    private void handleAssignTask(WebSocketSession session, JsonNode json) throws IOException {
        try {
            int taskId = json.get("taskId").asInt();
            Integer collaboratorId = json.has("collaboratorId") && !json.get("collaboratorId").isNull()
                    ? json.get("collaboratorId").asInt() : null;

            Task task = taskService.findTask(taskId);
            if (task == null) {
                sendResponse(session, buildError("ASSIGN_TASK", "Tâche introuvable."));
                return;
            }
            taskService.updateTaskState(taskId, task.getState().getValue(), collaboratorId);
            ObjectNode response = buildTaskResponse("UPDATE_TASK", true, "Collaborateur assigné.", taskId);
            sendResponse(session, response);
            webSocketHandler.broadcast(response, session.getId());
        } catch (SQLException | NoSuchElementException e) {
            sendResponse(session, buildError("ASSIGN_TASK", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // Construction JSON
    // -------------------------------------------------------------------------

    /**
     * Build a JSON array of tasks.
     * @param tasks the set of tasks to serialize
     * @return the built ArrayNode
     */
    private ArrayNode buildTasksArray(Set<Task> tasks) {
        ArrayNode array = objectMapper.createArrayNode();
        for (Task t : tasks)
            array.add(buildTaskNode(t));
        return array;
    }

    /**
     * Build a JSON node representing a single task with all its fields.
     * @param task the task to serialize
     * @return the built ObjectNode
     */
    private ObjectNode buildTaskNode(Task task) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("id", task.getId());
        node.put("description", task.getDescription());
        node.put("echeance", task.getEcheance().format(FORMATTER));
        node.put("state", task.getState().name());
        node.put("stateId", task.getState().getValue());

        ObjectNode creator = objectMapper.createObjectNode();
        creator.put("id", task.getCreator().getId());
        creator.put("firstName", task.getCreator().getFirstName());
        creator.put("lastName", task.getCreator().getLastName());
        node.set("creator", creator);

        if (task.getCurrentWorker() != null) {
            ObjectNode worker = objectMapper.createObjectNode();
            worker.put("id", task.getCurrentWorker().getId());
            worker.put("firstName", task.getCurrentWorker().getFirstName());
            worker.put("lastName", task.getCurrentWorker().getLastName());
            node.set("currentWorker", worker);
        } else {
            node.putNull("currentWorker");
        }

        if (task.getStartTime() != null)
            node.put("startTime", task.getStartTime().toString());
        else
            node.putNull("startTime");

        return node;
    }

    /**
     * Build a standard task response JSON node.
     * @param action the action this response corresponds to
     * @param success whether the action succeeded
     * @param message a human-readable status message
     * @param taskId the id of the task concerned
     * @return the built ObjectNode
     */
    private ObjectNode buildTaskResponse(String action, boolean success, String message, int taskId) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("action", action);
        response.put("success", success);
        response.put("message", message);
        response.put("taskId", taskId);
        return response;
    }

    /**
     * Build a standard error response JSON node.
     * @param action the action that triggered the error
     * @param message the error description
     * @return the built ObjectNode
     */
    private ObjectNode buildError(String action, String message) {
        ObjectNode error = objectMapper.createObjectNode();
        error.put("action", action);
        error.put("success", false);
        error.put("message", message);
        return error;
    }

    /**
     * Send a JSON response to a specific session.
     * Synchronized on the session to prevent concurrent write attempts from multiple threads.
     * @param session the target session
     * @param json the JSON node to serialize and send
     * @throws IOException if the message could not be sent
     */
    private void sendResponse(WebSocketSession session, ObjectNode json) throws IOException {
        synchronized (session) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(json)));
        }
    }
}