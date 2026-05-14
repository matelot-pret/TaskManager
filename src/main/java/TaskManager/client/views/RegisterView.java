package TaskManager.client.views;

import TaskManager.client.style.*;
import TaskManager.shared.models.Collaborator;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

import java.util.function.Consumer;

public class RegisterView {

    private final VBox root;
    private final TextField firstNameField;
    private final TextField lastNameField;
    private final TextField loginField;
    private final Label errorLabel;

    private final SessionContext sessionContext;

    private Runnable onBackToLogin;
    private Consumer<Collaborator> onRegisterSuccess;

    /**
     * Build the registration view and wire its internal interactions.
     * @param sessionContext the shared session context used to send the register message
     *                       and register the WebSocket listener
     */
    public RegisterView(SessionContext sessionContext) {
        this.sessionContext = sessionContext;

        root = new VBox(16);
        root.setAlignment(Pos.CENTER);
        root.setMaxWidth(320);

        Label title = new Label("Créer un compte");
        title.setStyle(Theme.LABEL_TITLE);

        Label firstNameLabel = new Label("Prénom");
        firstNameLabel.setStyle(Theme.LABEL_SUBTITLE);
        firstNameField = new TextField();
        firstNameField.setPromptText("Patrick");
        firstNameField.setStyle(Theme.INPUT);
        firstNameField.setMaxWidth(Double.MAX_VALUE);

        Label lastNameLabel = new Label("Nom");
        lastNameLabel.setStyle(Theme.LABEL_SUBTITLE);
        lastNameField = new TextField();
        lastNameField.setPromptText("Samou");
        lastNameField.setStyle(Theme.INPUT);
        lastNameField.setMaxWidth(Double.MAX_VALUE);

        Label loginLabel = new Label("Nom d'utilisateur");
        loginLabel.setStyle(Theme.LABEL_SUBTITLE);
        loginField = new TextField();
        loginField.setPromptText("p.samou");
        loginField.setStyle(Theme.INPUT);
        loginField.setMaxWidth(Double.MAX_VALUE);

        errorLabel = new Label("");
        errorLabel.setStyle(Theme.LABEL_DANGER);
        errorLabel.setVisible(false);

        Button registerBtn = new Button("S'enregistrer");
        registerBtn.setStyle(Theme.BTN_PRIMARY);
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setOnAction(e -> handleSubmit());

        Label backLabel = new Label("Déjà enregistré ? ");
        backLabel.setStyle(Theme.LABEL_SUBTITLE);

        Label backLink = new Label("Se connecter");
        backLink.setStyle(
                "-fx-text-fill: " + Theme.PRIMARY + ";" +
                        "-fx-underline: true;" +
                        "-fx-font-size: 12px;" +
                        "-fx-cursor: hand;"
        );
        backLink.setOnMouseClicked(e -> {
            if (onBackToLogin != null)
                onBackToLogin.run();
        });

        TextFlow backFlow = new TextFlow(backLabel, backLink);
        backFlow.setTextAlignment(TextAlignment.CENTER);

        VBox firstNameBox = new VBox(4, firstNameLabel, firstNameField);
        VBox lastNameBox  = new VBox(4, lastNameLabel, lastNameField);
        VBox loginBox     = new VBox(4, loginLabel, loginField);

        root.getChildren().addAll(
                title, firstNameBox, lastNameBox, loginBox,
                errorLabel, registerBtn, backFlow
        );

        registerWsListener();
    }

    // -------------------------------------------------------------------------
    // Interactions internes
    // -------------------------------------------------------------------------

    /**
     * Validate the input and send the register message to the server.
     * Called when the user clicks "S'enregistrer".
     */
    private void handleSubmit() {
        String firstName = firstNameField.getText().trim();
        String lastName  = lastNameField.getText().trim();
        String login     = loginField.getText().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || login.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }
        try {
            sessionContext.getWsClient().sendRegister(login, firstName, lastName);
        } catch (Exception e) {
            showError("Erreur lors de l'envoi : " + e.getMessage());
        }
    }

    /**
     * Register the WebSocket listener for the REGISTER action.
     * On success, sets the collaborator in the session context and notifies the parent.
     * On failure, displays the error message from the server.
     */
    private void registerWsListener() {
        sessionContext.getWsClient().on("REGISTER", json -> {
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
                    if (onRegisterSuccess != null)
                        onRegisterSuccess.accept(collaborator);
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
     * Display an error message below the fields.
     * Safe to call from any thread.
     * @param message the error message to display
     */
    public void showError(String message) {
        Platform.runLater(() -> {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        });
    }

    /** Clear all fields and hide the error message. */
    public void reset() {
        Platform.runLater(() -> {
            firstNameField.clear();
            lastNameField.clear();
            loginField.clear();
            errorLabel.setVisible(false);
        });
    }

    /**
     * Set the callback invoked when the user clicks "Se connecter".
     * @param handler the callback to invoke, runs on the JavaFX Application Thread
     */
    public void setOnBackToLogin(Runnable handler) { this.onBackToLogin = handler; }

    /**
     * Set the callback invoked when the registration succeeds.
     * @param handler the callback receiving the newly registered Collaborator
     */
    public void setOnRegisterSuccess(Consumer<Collaborator> handler) { this.onRegisterSuccess = handler; }

    public VBox getRoot() { return root; }
}