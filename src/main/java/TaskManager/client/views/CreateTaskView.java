package TaskManager.client.views;

import TaskManager.client.style.Theme;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class CreateTaskView {

    private final VBox root;
    private final TextField descField;
    private final DatePicker datePicker;
    private final TextField heureField;
    private final ComboBox<String> workerCombo;
    private final Label errorLabel;

    private final SessionContext sessionContext;
    private final Runnable onTaskCreated;

    /**
     * Build the task creation form.
     * The deadline is entered as a date (DatePicker) + time (TextField HH:mm).
     * @param sessionContext the shared session context
     * @param onTaskCreated callback invoked after a successful creation
     */
    public CreateTaskView(SessionContext sessionContext, Runnable onTaskCreated) {
        this.sessionContext = sessionContext;
        this.onTaskCreated  = onTaskCreated;

        root = new VBox(14);
        root.setPadding(new Insets(20));
        root.setMaxWidth(480);
        root.setStyle("-fx-background-color: " + Theme.BG_PRIMARY + ";");

        Label title = new Label("Nouvelle tâche");
        title.setStyle(Theme.LABEL_TITLE);

        Label descLabel = new Label("Description");
        descLabel.setStyle(Theme.LABEL_SUBTITLE);
        descField = new TextField();
        descField.setPromptText("Ex : Livraison rapport Q2");
        descField.setStyle(Theme.INPUT);
        descField.setMaxWidth(Double.MAX_VALUE);

        Label dateLabel = new Label("Échéance (date + heure)");
        dateLabel.setStyle(Theme.LABEL_SUBTITLE);

        datePicker = new DatePicker();
        HBox.setHgrow(datePicker, Priority.ALWAYS);
        datePicker.setMaxWidth(Double.MAX_VALUE);

        heureField = new TextField();
        heureField.setPromptText("HH:mm");
        heureField.setStyle(Theme.INPUT);
        heureField.setMaxWidth(80);

        HBox dateRow = new HBox(8, datePicker, heureField);

        Label workerLabel = new Label("Assigner à (optionnel)");
        workerLabel.setStyle(Theme.LABEL_SUBTITLE);
        workerCombo = new ComboBox<>();
        workerCombo.setMaxWidth(Double.MAX_VALUE);
        workerCombo.getItems().add("— Aucun —");
        workerCombo.getSelectionModel().selectFirst();

        errorLabel = new Label("");
        errorLabel.setStyle(Theme.LABEL_DANGER);
        errorLabel.setVisible(false);

        Button createBtn = new Button("Créer la tâche");
        createBtn.setStyle(Theme.BTN_PRIMARY);
        createBtn.setMaxWidth(Double.MAX_VALUE);
        createBtn.setOnAction(e -> handleSubmit());

        root.getChildren().addAll(
                title,
                new VBox(4, descLabel, descField),
                new VBox(4, dateLabel, dateRow),
                new VBox(4, workerLabel, workerCombo),
                errorLabel,
                createBtn
        );

        registerWsListeners();
        requestCollaborators();
    }

    // -------------------------------------------------------------------------
    // Chargement
    // -------------------------------------------------------------------------

    private void requestCollaborators() {
        try {
            sessionContext.getWsClient().send("{\"action\":\"GET_COLLABORATORS\"}");
        } catch (Exception e) {
            System.err.println("[CreateTask] Erreur requête collaborateurs : " + e.getMessage());
        }
    }

    /**
     * Register WebSocket listeners.
     * COLLABORATORS_DATA : populate the worker ComboBox.
     * CREATE_TASK        : navigate back on success, show error on failure.
     */
    private void registerWsListeners() {
        sessionContext.getWsClient().on("COLLABORATORS_DATA", json ->
                Platform.runLater(() ->
                        json.get("collaborators").forEach(c -> {
                            String name = c.get("firstName").asText() + " " + c.get("lastName").asText();
                            workerCombo.getItems().add(name);
                        })
                )
        );

        sessionContext.getWsClient().on("CREATE_TASK", json -> {
            boolean success = json.get("success").asBoolean();
            if (success)
                Platform.runLater(() -> { if (onTaskCreated != null) onTaskCreated.run(); });
            else
                Platform.runLater(() -> showError(json.get("message").asText()));
        });
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------

    /**
     * Validate the form and send a CREATE_TASK message to the server.
     * The echeance is built from the DatePicker and the heure field (HH:mm).
     * Format sent to server : "yyyy-MM-dd'T'HH:mm"
     */
    private void handleSubmit() {
        String desc  = descField.getText().trim();
        String heure = heureField.getText().trim();

        if (desc.isEmpty()) {
            showError("Veuillez entrer une description.");
            return;
        }
        if (datePicker.getValue() == null) {
            showError("Veuillez choisir une date d'échéance.");
            return;
        }
        if (!heure.matches("^([01]\\d|2[0-3]):[0-5]\\d$")) {
            showError("Heure invalide. Format attendu : HH:mm (ex: 09:00)");
            return;
        }

        String echeance = datePicker.getValue().toString() + "T" + heure;
        int creatorId   = sessionContext.getCurrentCollaborator().getId();

        try {
            sessionContext.getWsClient().send(
                    "{\"action\":\"CREATE_TASK\","
                            + "\"description\":\"" + desc + "\","
                            + "\"echeance\":\"" + echeance + "\","
                            + "\"creatorId\":" + creatorId + "}");
        } catch (Exception e) {
            showError("Erreur lors de l'envoi : " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Utilitaires
    // -------------------------------------------------------------------------

    /**
     * Display an error message below the form.
     * Safe to call from any thread.
     * @param message the error to display
     */
    private void showError(String message) {
        Platform.runLater(() -> {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        });
    }

    public VBox getRoot() { return root; }
}