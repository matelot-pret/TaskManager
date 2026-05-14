package TaskManager.client.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * WebSocket client using the JDK built-in java.net.http.WebSocket API.
 * Supports multiple listeners per action — each call to on() adds a listener
 * without replacing existing ones. This is critical because multiple views
 * register listeners for the same action (ex: TASKS_DATA in TaskListView AND MyTasksView).
 */
public class WebSocketClient {

    private static final String SERVER_URL = "ws://localhost:8080/ws";

    private WebSocket webSocket;
    private final HttpClient httpClient     = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Map of action -> list of callbacks.
     * Multiple views can register for the same action without overwriting each other.
     */
    private final Map<String, List<Consumer<JsonNode>>> listeners = new ConcurrentHashMap<>();

    /**
     * Buffer to accumulate WebSocket message fragments.
     * The JDK WebSocket API may split large messages into multiple fragments.
     * We accumulate until last=true, then parse the full message.
     */
    private final StringBuilder messageBuffer = new StringBuilder();

    /**
     * Open the WebSocket connection to the server.
     * Blocks until the connection is fully established (.join()).
     * @throws Exception if the connection could not be established
     * @post webSocket is open and ready to send/receive messages
     */
    public void connect() throws Exception {
        webSocket = httpClient.newWebSocketBuilder()
                .buildAsync(URI.create(SERVER_URL), new WebSocket.Listener() {

                    @Override
                    public void onOpen(WebSocket ws) {
                        System.out.println("[WS Client] Connecté au serveur.");
                        WebSocket.Listener.super.onOpen(ws);
                    }

                    /**
                     * Called when a text fragment is received.
                     * Accumulates fragments until last=true, then parses and dispatches
                     * to ALL registered listeners for the action.
                     * @param ws the WebSocket
                     * @param data the received fragment
                     * @param last true if this is the last fragment
                     */
                    @Override
                    public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                        messageBuffer.append(data);
                        if (last) {
                            String fullMessage = messageBuffer.toString();
                            messageBuffer.setLength(0);
                            try {
                                JsonNode json   = objectMapper.readTree(fullMessage);
                                String action   = json.get("action").asText();
                                List<Consumer<JsonNode>> list = listeners.get(action);
                                if (list != null)
                                    list.forEach(listener -> listener.accept(json));
                                else
                                    System.out.println("[WS Client] Aucun listener pour : " + action);
                            } catch (Exception e) {
                                System.err.println("[WS Client] Erreur parsing : " + e.getMessage());
                            }
                        }
                        return WebSocket.Listener.super.onText(ws, data, last);
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
                        System.out.println("[WS Client] Connexion fermée : " + reason);
                        return WebSocket.Listener.super.onClose(ws, statusCode, reason);
                    }

                    @Override
                    public void onError(WebSocket ws, Throwable error) {
                        System.err.println("[WS Client] Erreur : " + error.getMessage());
                    }
                })
                .join();
    }

    /**
     * Register a callback for a specific action.
     * Multiple callbacks can be registered for the same action — they are all invoked.
     * The callback runs on the WebSocket thread — use Platform.runLater() for UI updates.
     * @param action the action string to listen for (ex: "TASKS_DATA")
     * @param listener the callback receiving the parsed JSON node
     */
    public void on(String action, Consumer<JsonNode> listener) {
        listeners.computeIfAbsent(action, k -> new ArrayList<>()).add(listener);
    }

    /**
     * Unregister a specific callback for a given action.
     * Must be called when a view is destroyed to avoid listener accumulation.
     * @param action the action string
     * @param listener the exact listener instance to remove
     */
    public void off(String action, Consumer<JsonNode> listener) {
        List<Consumer<JsonNode>> list = listeners.get(action);
        if (list != null)
            list.remove(listener);
    }

    /**
     * Unregister all callbacks for a given set of actions at once.
     * Convenience method for views that need to clean up multiple listeners.
     * @param actions the action strings to clear all listeners for
     */
    public void offAll(String... actions) {
        for (String action : actions) {
            List<Consumer<JsonNode>> list = listeners.get(action);
            if (list != null)
                list.clear();
        }
    }

    /**
     * Send a raw JSON string to the server.
     * @param json the JSON string to send
     * @throws IOException if the WebSocket is not open
     */
    public void send(String json) throws IOException {
        if (webSocket != null && !webSocket.isOutputClosed())
            webSocket.sendText(json, true);
        else
            throw new IOException("WebSocket non connecté.");
    }

    /**
     * Send a LOGIN message.
     * @param login the collaborator's login
     * @throws IOException if the WebSocket is not open
     */
    public void sendLogin(String login) throws IOException {
        send("{\"action\":\"LOGIN\",\"login\":\"" + login + "\"}");
    }

    /**
     * Send a REGISTER message.
     * @param login the new collaborator's login
     * @param firstName the first name
     * @param lastName the last name
     * @throws IOException if the WebSocket is not open
     */
    public void sendRegister(String login, String firstName, String lastName) throws IOException {
        send("{\"action\":\"REGISTER\","
                + "\"login\":\"" + login + "\","
                + "\"firstName\":\"" + firstName + "\","
                + "\"lastName\":\"" + lastName + "\"}");
    }

    /** Close the WebSocket connection gracefully. */
    public void disconnect() {
        if (webSocket != null && !webSocket.isOutputClosed())
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
    }

    /** @return true if the WebSocket is open and ready */
    public boolean isConnected() {
        return webSocket != null && !webSocket.isOutputClosed();
    }
}