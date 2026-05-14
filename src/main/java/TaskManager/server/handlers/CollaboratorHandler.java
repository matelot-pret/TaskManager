package TaskManager.server.handlers;

import TaskManager.server.services.CollaboratorService;
import TaskManager.shared.exceptions.AlreadyExistsException;
import TaskManager.shared.models.Collaborator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ArrayNode;

@Component
public class CollaboratorHandler {

    private final CollaboratorService collaboratorService;
    private final TaskManager.server.handlers.TaskWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CollaboratorHandler(CollaboratorService collaboratorService,
                               @Lazy TaskManager.server.handlers.TaskWebSocketHandler webSocketHandler) {
        this.collaboratorService = collaboratorService;
        this.webSocketHandler    = webSocketHandler;
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
            case "LOGIN"             -> handleLogin(session, json);
            case "REGISTER"          -> handleRegister(session, json);
            case "GET_COLLABORATORS" -> handleGetCollaborators(session);
        }
    }

    /**
     * Handle a LOGIN action.
     * Looks up the collaborator by login and sends back their data if found.
     * Expects  : { "action": "LOGIN", "login": "p.samou" }
     * Responds : { "action": "LOGIN", "success": true/false, "message": "...", ...collaborator fields }
     * @param session the session of the requesting client
     * @param json the parsed JSON message
     * @throws IOException if the response could not be sent
     */
    private void handleLogin(WebSocketSession session, JsonNode json) throws IOException {
        String login = json.get("login").asText();
        try {
            Collaborator collaborator = collaboratorService.login(login);
            if (collaborator == null) {
                sendResponse(session, buildError("LOGIN", "Aucun collaborateur trouvé avec ce login."));
            } else {
                webSocketHandler.registerCollaborator(session.getId(), collaborator.getId());
                sendResponse(session, buildCollaboratorResponse("LOGIN", true, "Connexion réussie.", collaborator));
            }
        } catch (SQLException e) {
            sendResponse(session, buildError("LOGIN", "Erreur base de données : " + e.getMessage()));
        }
    }

    /**
     * Handle a REGISTER action.
     * Creates a new collaborator and sends back their data with the generated id.
     * Expects  : { "action": "REGISTER", "login": "p.samou", "firstName": "Patrick", "lastName": "Samou" }
     * Responds : { "action": "REGISTER", "success": true/false, "message": "...", ...collaborator fields }
     * @param session the session of the requesting client
     * @param json the parsed JSON message
     * @throws IOException if the response could not be sent
     */
    private void handleRegister(WebSocketSession session, JsonNode json) throws IOException {
        String login     = json.get("login").asText();
        String firstName = json.get("firstName").asText();
        String lastName  = json.get("lastName").asText();
        try {
            Collaborator collaborator = collaboratorService.register(login, firstName, lastName);
            webSocketHandler.registerCollaborator(session.getId(), collaborator.getId());
            sendResponse(session, buildCollaboratorResponse("REGISTER", true, "Enregistrement réussi.", collaborator));
        } catch (AlreadyExistsException e) {
            sendResponse(session, buildError("REGISTER", "Ce login est déjà utilisé."));
        } catch (SQLException e) {
            sendResponse(session, buildError("REGISTER", "Erreur base de données : " + e.getMessage()));
        }
    }

    /**
     * Handle a GET_COLLABORATORS action.
     * Returns all collaborators in the database.
     * Expects  : { "action": "GET_COLLABORATORS" }
     * Responds : { "action": "COLLABORATORS_DATA", "collaborators": [...] }
     * @param session the session of the requesting client
     * @throws IOException if the response could not be sent
     */
    private void handleGetCollaborators(WebSocketSession session) throws IOException {
        try {
            Set<Collaborator> collaborators = collaboratorService.findAll();
            ObjectNode response = objectMapper.createObjectNode();
            response.put("action", "COLLABORATORS_DATA");
            ArrayNode array = objectMapper.createArrayNode();
            for (Collaborator c : collaborators) {
                ObjectNode node = objectMapper.createObjectNode();
                node.put("id", c.getId());
                node.put("login", c.getLogin());
                node.put("firstName", c.getFirstName());
                node.put("lastName", c.getLastName());
                array.add(node);
            }
            response.set("collaborators", array);
            sendResponse(session, response);
        } catch (SQLException e) {
            sendResponse(session, buildError("GET_COLLABORATORS", "Erreur base de données : " + e.getMessage()));
        }
    }

    /**
     * Build a JSON response containing the collaborator's data.
     * @param action the action this response corresponds to
     * @param success whether the action succeeded
     * @param message a human-readable status message
     * @param collaborator the collaborator whose data to include
     * @return the built ObjectNode
     */
    private ObjectNode buildCollaboratorResponse(String action, boolean success,
                                                 String message, Collaborator collaborator) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("action", action);
        response.put("success", success);
        response.put("message", message);
        response.put("id", collaborator.getId());
        response.put("login", collaborator.getLogin());
        response.put("firstName", collaborator.getFirstName());
        response.put("lastName", collaborator.getLastName());
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