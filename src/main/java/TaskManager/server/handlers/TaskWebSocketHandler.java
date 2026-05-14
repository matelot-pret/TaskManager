package TaskManager.server.handlers;

import TaskManager.server.services.TaskService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TaskWebSocketHandler extends TextWebSocketHandler {

    private final CollaboratorHandler collaboratorHandler;
    private final TaskHandler taskHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Map of all active sessions : sessionId -> WebSocketSession.
     * ConcurrentHashMap because multiple threads can access it simultaneously.
     */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * Map of sessionId -> collaboratorId.
     * Populated on LOGIN/REGISTER, used on disconnect to pause running tasks.
     */
    private final Map<String, Integer> sessionCollaborator = new ConcurrentHashMap<>();

    private final TaskService taskService;

    public TaskWebSocketHandler(CollaboratorHandler collaboratorHandler,
                                TaskHandler taskHandler,
                                TaskService taskService) {
        this.collaboratorHandler = collaboratorHandler;
        this.taskHandler = taskHandler;
        this.taskService = taskService;
    }

    /**
     * Called when a new client connects.
     * Registers the session in the active sessions map.
     * @param session the session of the newly connected client
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        System.out.println("[WS] Client connecté : " + session.getId());
    }

    /**
     * Called when a client disconnects.
     * Removes the session from the active sessions map.
     * @param session the session of the disconnected client
     * @param status the close status
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        Integer collaboratorId = sessionCollaborator.remove(session.getId());
        if (collaboratorId != null) {
            try {
                taskService.pauseAllTasksForCollaborator(collaboratorId);
                System.out.println("[WS] Tâches mises en pause pour collaborateur " + collaboratorId);
            } catch (Exception e) {
                System.err.println("[WS] Erreur pause tâches : " + e.getMessage());
            }
        }
        System.out.println("[WS] Client déconnecté : " + session.getId());
    }

    /**
     * Entry point for all messages received from clients.
     * Reads the "action" field and routes to the appropriate handler.
     * Does not contain any business logic.
     * @param session the session of the client who sent the message
     * @param message the raw text message received
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode json = objectMapper.readTree(message.getPayload());
        String action = json.get("action").asText();

        switch (action) {
            case "LOGIN",
                 "REGISTER",
                 "GET_COLLABORATORS"                          -> collaboratorHandler.handle(session, action, json);
            case "GET_TASKS",
                 "GET_TASK",
                 "GET_DASHBOARD",
                 "CREATE_TASK",
                 "UPDATE_TASK",
                 "DELETE_TASK",
                 "ASSIGN_TASK",
                 "ASSIGN_BY_NAME"                            -> taskHandler.handle(session, action, json);
            default                                          -> sendError(session, "Action inconnue : " + action);
        }
    }

    /**
     * Register the collaborator associated with a session.
     * Called by CollaboratorHandler after a successful LOGIN or REGISTER.
     * Used to pause running tasks when the collaborator disconnects.
     * @param sessionId the WebSocket session id
     * @param collaboratorId the id of the authenticated collaborator
     */
    public void registerCollaborator(String sessionId, int collaboratorId) {
        sessionCollaborator.put(sessionId, collaboratorId);
    }

    /**
     * Broadcast a JSON message to all connected sessions except the sender.
     * Each send is synchronized on the session object to prevent concurrent write attempts
     * on the same WebSocket session from multiple threads (causes TEXT_PARTIAL_WRITING error).
     * @param message the JSON message to broadcast
     * @param excludeSessionId the session id of the sender to exclude, or null to broadcast to all
     */
    public void broadcast(ObjectNode message, String excludeSessionId) {
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (IOException e) {
            System.err.println("[WS] Erreur sérialisation broadcast : " + e.getMessage());
            return;
        }
        sessions.forEach((id, session) -> {
            if ((excludeSessionId == null || !id.equals(excludeSessionId)) && session.isOpen()) {
                synchronized (session) {
                    try {
                        session.sendMessage(new TextMessage(json));
                    } catch (IOException e) {
                        System.err.println("[WS] Erreur broadcast vers " + id + " : " + e.getMessage());
                    }
                }
            }
        });
    }

    /**
     * Send an error message to a specific session.
     * Synchronized on the session to prevent concurrent writes.
     * @param session the target session
     * @param message the error description
     */
    private void sendError(WebSocketSession session, String message) throws IOException {
        ObjectNode error = objectMapper.createObjectNode();
        error.put("action", "ERROR");
        error.put("success", false);
        error.put("message", message);
        synchronized (session) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
        }
    }
}