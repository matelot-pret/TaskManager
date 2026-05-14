package TaskManager.client.views;

import TaskManager.client.style.*;
import TaskManager.client.websocket.WebSocketClient;
import TaskManager.shared.models.Collaborator;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class FrameHomeView {

    private final BorderPane root;
    private final SessionContext sessionContext;

    private final LoginView loginView;
    private final RegisterView registerView;

    /**
     * Build the root container, connect the WebSocket client,
     * create the views with the shared session context,
     * and wire the navigation callbacks between them.
     */
    public FrameHomeView() {
        root = new BorderPane();
        root.setStyle("-fx-background-color: " + Theme.BG_SECONDARY + ";");

        WebSocketClient wsClient = new WebSocketClient();
        sessionContext = new SessionContext(wsClient);

        loginView    = new LoginView(sessionContext);
        registerView = new RegisterView(sessionContext);

        wireNavigation();
        connectWebSocket();
        showLogin();
    }

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------

    /**
     * Wire the navigation callbacks between views.
     * FrameHomeView only decides which view to show — no business logic here.
     */
    private void wireNavigation() {
        loginView.setOnRegisterClick(this::showRegister);
        loginView.setOnLoginSuccess(this::showMain);

        registerView.setOnBackToLogin(this::showLogin);
        registerView.setOnRegisterSuccess(this::showMain);
    }

    private void showLogin() {
        loginView.reset();
        root.setCenter(centered(loginView.getRoot()));
    }

    private void showRegister() {
        registerView.reset();
        root.setCenter(centered(registerView.getRoot()));
    }

    private void showMain(Collaborator collaborator) {
        MainView mainView = new MainView(sessionContext);
        root.setCenter(mainView.getRoot());
    }

    // -------------------------------------------------------------------------
    // WebSocket
    // -------------------------------------------------------------------------

    /**
     * Start the WebSocket connection in a background thread.
     * On failure, display an error on the login view.
     */
    private void connectWebSocket() {
        new Thread(() -> {
            try {
                sessionContext.getWsClient().connect();
            } catch (Exception e) {
                Platform.runLater(() ->
                        loginView.showError("Impossible de se connecter au serveur.")
                );
            }
        }, "ws-connect").start();
    }

    // -------------------------------------------------------------------------
    // Utilitaire
    // -------------------------------------------------------------------------

    /**
     * Wrap a node in a centered VBox that fills the available space.
     * @param node the node to center
     * @return the wrapping VBox
     */
    private VBox centered(javafx.scene.Node node) {
        VBox wrapper = new VBox(node);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(new Insets(32));
        return wrapper;
    }

    public BorderPane getRoot() { return root; }
}