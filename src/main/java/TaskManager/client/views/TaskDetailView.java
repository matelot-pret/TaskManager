package TaskManager.client.views;

import TaskManager.client.style.Theme;
import TaskManager.shared.models.TaskState;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class TaskDetailView {

    private final VBox root;
    private final SessionContext sessionContext;
    private final int taskId;
    private final Runnable onBack;

    private Label descLabel;
    private Label echeanceLabel;
    private Label creatorLabel;
    private Label stateLabel;
    private ComboBox<String> workerCombo;

    private Button startBtn;
    private Button pauseBtn;
    private Button closeBtn;
    private Button blockBtn;
    private Button cancelBtn;

    private JsonNode currentTask;

    /**
     * Build the task detail view for a specific task.
     * Button states reflect both the task state and the connected collaborator's role.
     * @param sessionContext the shared session context
     * @param taskId the id of the task to display
     * @param onBack callback invoked when the user clicks "Retour"
     */
    public TaskDetailView(SessionContext sessionContext, int taskId, Runnable onBack) {
        this.sessionContext = sessionContext;
        this.taskId = taskId;
        this.onBack = onBack;

        root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: " + Theme.BG_PRIMARY + ";");

        buildLayout();
        registerWsListeners();
        requestTaskData();
    }

    // -------------------------------------------------------------------------
    // Construction UI
    // -------------------------------------------------------------------------

    private void buildLayout() {
        Button backBtn = new Button("← Retour");
        backBtn.setStyle(Theme.SIDEBAR_ITEM);
        addHover(backBtn, Theme.SIDEBAR_ITEM,
                Theme.SIDEBAR_ITEM + "-fx-text-fill: " + Theme.PRIMARY + ";");
        backBtn.setOnAction(e -> {
            unregister();
            if (onBack != null) onBack.run();
        });

        descLabel     = new Label("");
        descLabel.setStyle(Theme.LABEL_TITLE);
        echeanceLabel = new Label("");
        echeanceLabel.setStyle(Theme.LABEL_SUBTITLE);
        creatorLabel  = new Label("");
        creatorLabel.setStyle(Theme.LABEL_SUBTITLE);
        stateLabel    = new Label("");
        stateLabel.setStyle(Theme.LABEL_SUBTITLE);

        Label workerLabel = new Label("Collaborateur assigné");
        workerLabel.setStyle(Theme.LABEL_SUBTITLE);
        workerCombo = new ComboBox<>();
        workerCombo.setMaxWidth(Double.MAX_VALUE);
        workerCombo.setOnAction(e -> handleAssign());

        Label actionLabel = new Label("Actions");
        actionLabel.setStyle(Theme.LABEL_SUBTITLE);

        startBtn  = new Button("Commencer");
        pauseBtn  = new Button("Mettre en pause");
        closeBtn  = new Button("Clôturer");
        blockBtn  = new Button("Bloquer");
        cancelBtn = new Button("Annuler");

        startBtn.setOnAction(e  -> sendUpdateTask(TaskState.PROGRESSING, sessionContext.getCurrentCollaborator().getId()));
        pauseBtn.setOnAction(e  -> sendUpdateTask(TaskState.PAUSED, null));
        closeBtn.setOnAction(e  -> sendUpdateTask(TaskState.CLOSED, null));
        blockBtn.setOnAction(e  -> sendUpdateTask(TaskState.BLOCKED, null));
        cancelBtn.setOnAction(e -> sendUpdateTask(TaskState.CANCELED, null));

        HBox actionRow = new HBox(8, startBtn, pauseBtn, closeBtn, blockBtn, cancelBtn);

        root.getChildren().addAll(
                backBtn, descLabel, echeanceLabel, creatorLabel, stateLabel,
                new VBox(4, workerLabel, workerCombo),
                new VBox(8, actionLabel, actionRow)
        );
    }

    // -------------------------------------------------------------------------
    // Chargement
    // -------------------------------------------------------------------------

    private void requestTaskData() {
        try {
            sessionContext.getWsClient().send(
                    "{\"action\":\"GET_TASK\",\"taskId\":" + taskId + "}");
        } catch (Exception e) {
            System.err.println("[TaskDetail] Erreur requête : " + e.getMessage());
        }
    }

    /**
     * Register WebSocket listeners for this view.
     * Calls offAll() first to clean up any listeners left by a previous TaskDetailView instance.
     * This prevents listener accumulation when the user opens multiple task details
     * without always clicking "Retour".
     */
    private void registerWsListeners() {
        sessionContext.getWsClient().offAll("TASK_DATA", "COLLABORATORS_DATA", "UPDATE_TASK");
        sessionContext.getWsClient().on("TASK_DATA", json ->
                Platform.runLater(() -> {
                    currentTask = json.get("task");
                    populateTask(currentTask);
                    updateButtonStates(currentTask);
                })
        );

        sessionContext.getWsClient().on("COLLABORATORS_DATA", json ->
                Platform.runLater(() -> populateWorkerCombo(json))
        );

        sessionContext.getWsClient().on("UPDATE_TASK", json -> {
            if (json.has("taskId") && json.get("taskId").asInt() == taskId)
                Platform.runLater(this::requestTaskData);
        });
    }

    // -------------------------------------------------------------------------
    // Peuplement
    // -------------------------------------------------------------------------

    private void populateTask(JsonNode task) {
        descLabel.setText(task.get("description").asText());

        boolean isLate = task.get("state").asText().equals(TaskState.LATE.name());
        echeanceLabel.setText("Échéance : " + task.get("echeance").asText());
        echeanceLabel.setStyle(isLate ? Theme.LABEL_DANGER : Theme.LABEL_SUBTITLE);

        creatorLabel.setText("Créée par : "
                + task.get("creator").get("firstName").asText()
                + " " + task.get("creator").get("lastName").asText());
        stateLabel.setText("Statut : " + stateLabelFor(task.get("state").asText()));

        try {
            sessionContext.getWsClient().send("{\"action\":\"GET_COLLABORATORS\"}");
        } catch (Exception e) {
            System.err.println("[TaskDetail] Erreur GET_COLLABORATORS : " + e.getMessage());
        }
    }

    /** Map of display name -> collaborator id, used to send the correct id on assign */
    private final java.util.Map<String, Integer> collaboratorIds = new java.util.HashMap<>();

    private void populateWorkerCombo(JsonNode json) {
        String currentWorkerName = null;
        if (currentTask != null && currentTask.has("currentWorker")
                && !currentTask.get("currentWorker").isNull()) {
            currentWorkerName = currentTask.get("currentWorker").get("firstName").asText()
                    + " " + currentTask.get("currentWorker").get("lastName").asText();
        }

        workerCombo.getItems().clear();
        collaboratorIds.clear();
        workerCombo.getItems().add("— Aucun —");

        for (JsonNode c : json.get("collaborators")) {
            String name = c.get("firstName").asText() + " " + c.get("lastName").asText();
            collaboratorIds.put(name, c.get("id").asInt());
            workerCombo.getItems().add(name);
            if (name.equals(currentWorkerName))
                workerCombo.getSelectionModel().select(name);
        }

        if (workerCombo.getSelectionModel().isEmpty())
            workerCombo.getSelectionModel().selectFirst();

        if (currentTask != null)
            updateComboState(currentTask);
    }

    // -------------------------------------------------------------------------
    // Règles métier des boutons
    // -------------------------------------------------------------------------

    /**
     * Update button states based on task state and connected collaborator's role.
     *
     * Rules :
     * - Terminal (CLOSED/CANCELED) : tout désactivé
     * - PROGRESSING, je suis currentWorker : pause ✓ | close ✓ | block ✓ | start ✗ | cancel ✗
     * - PROGRESSING, je ne suis PAS currentWorker : tout désactivé
     * - Sans currentWorker (UNOPENED/PAUSED/LATE/BLOCKED) :
     *     - start ✓ pour tout le monde
     *     - cancel ✓ seulement pour le créateur
     *     - pause/close/block ✗
     *
     * @param task the JSON node representing the task
     */
    private void updateButtonStates(JsonNode task) {
        String state      = task.get("state").asText();
        int myId          = sessionContext.getCurrentCollaborator().getId();
        int creatorId     = task.get("creator").get("id").asInt();
        boolean hasWorker = task.has("currentWorker") && !task.get("currentWorker").isNull();
        int workerId      = hasWorker ? task.get("currentWorker").get("id").asInt() : -1;
        boolean iAmWorker   = hasWorker && workerId == myId;
        boolean iAmCreator  = creatorId == myId;
        boolean isTerminal  = state.equals(TaskState.CLOSED.name())
                || state.equals(TaskState.CANCELED.name());
        boolean isLateWithWorker = state.equals(TaskState.LATE.name()) && hasWorker;
        boolean isProgressing = state.equals(TaskState.PROGRESSING.name());

        if (isTerminal) {
            setBtn(startBtn,  false);
            setBtn(pauseBtn,  false);
            setBtn(closeBtn,  false);
            setBtn(blockBtn,  false);
            setBtn(cancelBtn, false);
            workerCombo.setDisable(true);
            return;
        }

        if (isProgressing || isLateWithWorker) {
            // Seul le currentWorker peut agir
            setBtn(startBtn,  false);
            setBtn(pauseBtn,  iAmWorker);
            setBtn(closeBtn,  iAmWorker);
            setBtn(blockBtn,  iAmWorker);
            setBtn(cancelBtn, false);
            workerCombo.setDisable(true);
            return;
        }

        // UNOPENED / PAUSED / LATE / BLOCKED — personne ne travaille dessus
        setBtn(startBtn,  true);
        setBtn(pauseBtn,  false);
        setBtn(closeBtn,  false);
        setBtn(blockBtn,  false);
        setBtn(cancelBtn, iAmCreator);
        workerCombo.setDisable(false);
    }

    /**
     * Enable or disable the worker ComboBox based on task state.
     * @param task the JSON node representing the task
     */
    private void updateComboState(JsonNode task) {
        String state      = task.get("state").asText();
        boolean hasWorker = task.has("currentWorker") && !task.get("currentWorker").isNull();
        boolean isTerminal = state.equals(TaskState.CLOSED.name())
                || state.equals(TaskState.CANCELED.name());
        workerCombo.setDisable(isTerminal || state.equals(TaskState.PROGRESSING.name()));
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------

    /**
     * Send an UPDATE_TASK message to the server.
     * Uses ObjectMapper to build valid JSON — avoids malformed strings.
     * @param newState the new state to apply
     * @param currentWorkerId the collaborator id to set as currentWorker, or null
     */
    private void sendUpdateTask(TaskState newState, Integer currentWorkerId) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.node.ObjectNode payload =
                    mapper.createObjectNode();
            payload.put("action", "UPDATE_TASK");
            payload.put("taskId", taskId);
            payload.put("stateId", newState.getValue());
            if (currentWorkerId != null)
                payload.put("currentWorkerId", currentWorkerId);
            else
                payload.putNull("currentWorkerId");
            sessionContext.getWsClient().send(mapper.writeValueAsString(payload));
        } catch (Exception e) {
            System.err.println("[TaskDetail] Erreur update : " + e.getMessage());
        }
    }

    /**
     * Send an ASSIGN_TASK message when the user selects a collaborator in the ComboBox.
     * Sends the collaborator id (not the name) to avoid server-side lookup by name.
     * Assigning does not change the task state.
     */
    private void handleAssign() {
        if (currentTask == null) return;
        String selected = workerCombo.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.node.ObjectNode payload =
                    mapper.createObjectNode();
            payload.put("action", "ASSIGN_TASK");
            payload.put("taskId", taskId);

            if (selected.equals("— Aucun —")) {
                payload.putNull("collaboratorId");
            } else {
                Integer collabId = collaboratorIds.get(selected);
                if (collabId == null) {
                    System.err.println("[TaskDetail] Id introuvable pour : " + selected);
                    return;
                }
                payload.put("collaboratorId", collabId);
            }
            sessionContext.getWsClient().send(mapper.writeValueAsString(payload));
        } catch (Exception e) {
            System.err.println("[TaskDetail] Erreur assignation : " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Utilitaires
    // -------------------------------------------------------------------------

    /**
     * Unregister all listeners registered by this view.
     * Must be called before navigating away to prevent listener accumulation.
     * Called by MainView before replacing this view with another TaskDetailView,
     * and by the "Retour" button when the user navigates back.
     */
    public void unregister() {
        sessionContext.getWsClient().offAll("TASK_DATA", "COLLABORATORS_DATA", "UPDATE_TASK");
    }

    /**
     * Set a button enabled/disabled and apply the matching style.
     * @param btn the button to update
     * @param enabled true = outline style, false = disabled style
     */
    private void setBtn(Button btn, boolean enabled) {
        btn.setDisable(!enabled);
        btn.setStyle(enabled ? Theme.BTN_OUTLINE : Theme.BTN_DISABLED);
    }

    /**
     * Add hover effect on a button — only fires when the button is not disabled.
     * @param btn the button
     * @param normal the normal style
     * @param hovered the hovered style
     */
    private void addHover(Button btn, String normal, String hovered) {
        btn.setOnMouseEntered(e -> { if (!btn.isDisabled()) btn.setStyle(hovered); });
        btn.setOnMouseExited(e  -> {
            if (!btn.isDisabled()) btn.setStyle(normal);
            else btn.setStyle(Theme.BTN_DISABLED);
        });
    }

    private String stateLabelFor(String s) {
        return switch (s) {
            case "UNOPENED"    -> "Non entamé";
            case "PROGRESSING" -> "En cours";
            case "CLOSED"      -> "Clôturé";
            case "PAUSED"      -> "En pause";
            case "LATE"        -> "En retard";
            case "BLOCKED"     -> "Bloqué";
            case "CANCELED"    -> "Annulé";
            default            -> s;
        };
    }

    public VBox getRoot() { return root; }
}