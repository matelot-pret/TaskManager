package TaskManager.client.views;

import TaskManager.client.style.*;
import TaskManager.shared.models.Collaborator;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

import java.util.function.Consumer;

public class LoginView {

    private final VBox root;
    private final TextField loginField;
    private final Label errorLabel;

    private final SessionContext sessionContext;

    private Runnable onRegisterClick;
    private Consumer<Collaborator> onLoginSuccess;

    /**
     * Build the login view and wire its internal interactions.
     * @param sessionContext the shared session context used to send the login message
     *                       and register the WebSocket listener
     */
    public LoginView(SessionContext sessionContext) {
        this.sessionContext = sessionContext;

        root = new VBox(16);
        root.setAlignment(Pos.CENTER);
        root.setMaxWidth(320);

        Label title = new Label("Gestionnaire de tâches");
        title.setStyle(Theme.LABEL_TITLE);

        Label subtitle = new Label("Connectez-vous pour continuer");
        subtitle.setStyle(Theme.LABEL_SUBTITLE);

        VBox titleBox = new VBox(4, title, subtitle);
        titleBox.setAlignment(Pos.CENTER);

        Label loginLabel = new Label("Nom d'utilisateur");
        loginLabel.setStyle(Theme.LABEL_SUBTITLE);

        loginField = new TextField();
        loginField.setPromptText("p.samou");
        loginField.setStyle(Theme.INPUT);
        loginField.setMaxWidth(Double.MAX_VALUE);

        errorLabel = new Label("");
        errorLabel.setStyle(Theme.LABEL_DANGER);
        errorLabel.setVisible(false);

        Button loginBtn = new Button("Se connecter");
        loginBtn.setStyle(Theme.BTN_PRIMARY);
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setOnAction(e -> handleSubmit());

        Label notRegistered = new Label("Pas encore enregistré ? ");
        notRegistered.setStyle(Theme.LABEL_SUBTITLE);

        Label registerLink = new Label("Enregistrez-vous");
        registerLink.setStyle(
                "-fx-text-fill: " + Theme.PRIMARY + ";" +
                        "-fx-underline: true;" +
                        "-fx-font-size: 12px;" +
                        "-fx-cursor: hand;"
        );
        registerLink.setOnMouseClicked(e -> {
            if (onRegisterClick != null)
                onRegisterClick.run();
        });

        TextFlow registerFlow = new TextFlow(notRegistered, registerLink);
        registerFlow.setTextAlignment(TextAlignment.CENTER);

        VBox fieldBox = new VBox(4, loginLabel, loginField);
        fieldBox.setMaxWidth(Double.MAX_VALUE);

        root.getChildren().addAll(titleBox, fieldBox, errorLabel, loginBtn, registerFlow);

        registerWsListener();
    }

    // -------------------------------------------------------------------------
    // Interactions internes
    // -------------------------------------------------------------------------

    /**
     * Validate the input and send the login message to the server.
     * Called when the user clicks "Se connecter".
     */
    private void handleSubmit() {
        String login = loginField.getText().trim();
        if (login.isEmpty()) {
            showError("Veuillez entrer un nom d'utilisateur.");
            return;
        }
        try {
            sessionContext.getWsClient().sendLogin(login);
        } catch (Exception e) {
            showError("Erreur lors de l'envoi : " + e.getMessage());
        }
    }

    /**
     * Register the WebSocket listener for the LOGIN action.
     * On success, sets the collaborator in the session context and notifies the parent.
     * On failure, displays the error message from the server.
     */
    private void registerWsListener() {
        sessionContext.getWsClient().on("LOGIN", json -> {
            boolean success = json.get("success").asBoolean();
            if (success) {
                Collaborator collaborator = new Collaborator(
                        json.get("id").asInt(),
                        json.get("login").asText(),
                        json.get("firstName").asText(),
                        json.get("lastName").asText()
                );
                sessionContext.setCurrentCollaborator(collaborator);
                Platform.runLater(() -> {
                    if (onLoginSuccess != null)
                        onLoginSuccess.accept(collaborator);
                });
            } else {
                showError(json.get("message").asText());
            }
        });
    }

    // -------------------------------------------------------------------------
    // API publique
    // -------------------------------------------------------------------------

    /**
     * Display an error message below the input field.
     * Safe to call from any thread.
     * @param message the error message to display
     */
    public void showError(String message) {
        Platform.runLater(() -> {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        });
    }

    /** Clear the input field and hide the error message. */
    public void reset() {
        Platform.runLater(() -> {
            loginField.clear();
            errorLabel.setVisible(false);
        });
    }

    /**
     * Set the callback invoked when the user clicks "Enregistrez-vous".
     * @param handler the callback to invoke, runs on the JavaFX Application Thread
     */
    public void setOnRegisterClick(Runnable handler) { this.onRegisterClick = handler; }

    /**
     * Set the callback invoked when the login succeeds.
     * @param handler the callback receiving the authenticated Collaborator
     */
    public void setOnLoginSuccess(Consumer<Collaborator> handler) { this.onLoginSuccess = handler; }

    public VBox getRoot() { return root; }
}