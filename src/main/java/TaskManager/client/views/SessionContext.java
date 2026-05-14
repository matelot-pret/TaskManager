package TaskManager.client.views;

import TaskManager.client.websocket.WebSocketClient;
import TaskManager.shared.models.Collaborator;

/**
 * Holds the shared state of the current session :
 * the authenticated collaborator and the active WebSocket connection.
 * Passed to every view that needs to send messages or know who is connected.
 */
public class SessionContext {

    private final WebSocketClient wsClient;
    private Collaborator currentCollaborator;

    /**
     * Create a new SessionContext with the given WebSocket client.
     * @param wsClient the active WebSocket client connected to the server
     * @pre wsClient must be connected before being passed here
     */
    public SessionContext(WebSocketClient wsClient) {
        this.wsClient = wsClient;
    }

    /**
     * Set the collaborator who just authenticated.
     * @param collaborator the authenticated collaborator
     */
    public void setCurrentCollaborator(Collaborator collaborator) {
        this.currentCollaborator = collaborator;
    }

    /**
     * @return the collaborator currently authenticated in this session
     */
    public Collaborator getCurrentCollaborator() {
        return currentCollaborator;
    }

    /**
     * @return the WebSocket client shared across all views
     */
    public WebSocketClient getWsClient() {
        return wsClient;
    }
}