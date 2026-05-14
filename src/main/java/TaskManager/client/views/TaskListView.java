package TaskManager.client.views;

import TaskManager.client.style.*;
import TaskManager.shared.models.TaskState;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.util.function.Consumer;

public class TaskListView {

    private final VBox root;
    private final VBox taskListBox;
    private final Label titleLabel;
    private final SessionContext sessionContext;
    private final Consumer<Integer> onTaskClick;

    /** Current state filter — null means show all (except CANCELED) */
    private String stateFilter = null;

    /** Last received task list from server, used to re-apply filter without re-fetching */
    private JsonNode lastTasksData = null;

    /**
     * Build the task list view.
     * Registers WebSocket listeners immediately so broadcasts are always received.
     * Data is fetched only when refresh() is called.
     * @param sessionContext the shared session context
     * @param onTaskClick callback invoked with the taskId when a task row is clicked
     */
    public TaskListView(SessionContext sessionContext, Consumer<Integer> onTaskClick) {
        this.sessionContext = sessionContext;
        this.onTaskClick    = onTaskClick;

        root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: " + Theme.BG_PRIMARY + ";");

        titleLabel = new Label("TÂCHES QUOTIDIENNES");
        titleLabel.setStyle(Theme.LABEL_SECTION);

        taskListBox = new VBox(8);

        ScrollPane scroll = new ScrollPane(taskListBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;" +
                "-fx-border-color: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().addAll(titleLabel, scroll);

        registerWsListeners();
    }

    // -------------------------------------------------------------------------
    // API publique
    // -------------------------------------------------------------------------

    /**
     * Request fresh task data from the server.
     * Called by MainView when this view becomes visible.
     */
    public void refresh() {
        stateFilter = null;
        titleLabel.setText("TÂCHES QUOTIDIENNES");
        requestTasks();
    }

    /**
     * Filter the displayed tasks by state without re-fetching from server.
     * If no data has been loaded yet, fetches first.
     * @param stateName the TaskState name to filter on (ex: "PROGRESSING")
     */
    public void filterByState(String stateName) {
        stateFilter = stateName;
        String label = stateLabelFor(stateName);
        titleLabel.setText("TÂCHES — " + label.toUpperCase());
        if (lastTasksData != null)
            applyFilter();
        else
            requestTasks();
    }

    // -------------------------------------------------------------------------
    // Chargement
    // -------------------------------------------------------------------------

    private void requestTasks() {
        try {
            sessionContext.getWsClient().send("{\"action\":\"GET_TASKS\"}");
        } catch (Exception e) {
            System.err.println("[TaskList] Erreur requête : " + e.getMessage());
        }
    }

    /**
     * Register WebSocket listeners.
     * TASKS_DATA   : store the full list and apply current filter.
     * UPDATE_TASK / CREATE_TASK / DELETE_TASK : re-fetch the list.
     */
    private void registerWsListeners() {
        sessionContext.getWsClient().on("TASKS_DATA", json -> {
            lastTasksData = json;
            Platform.runLater(this::applyFilter);
        });

        sessionContext.getWsClient().on("UPDATE_TASK",  json -> Platform.runLater(this::requestTasks));
        sessionContext.getWsClient().on("ASSIGN_TASK",  json -> Platform.runLater(this::requestTasks));
        sessionContext.getWsClient().on("CREATE_TASK",  json -> Platform.runLater(this::requestTasks));
        sessionContext.getWsClient().on("DELETE_TASK",  json -> Platform.runLater(this::requestTasks));
    }

    /**
     * Apply the current stateFilter to the last received task list and rebuild the UI.
     * If stateFilter is null, shows all tasks except CANCELED.
     */
    private void applyFilter() {
        if (lastTasksData == null) return;
        taskListBox.getChildren().clear();

        lastTasksData.get("tasks").forEach(taskNode -> {
            String stateName = taskNode.get("state").asText();
            if (stateName.equals(TaskState.CANCELED.name())) return;
            if (stateFilter != null && !stateName.equals(stateFilter)) return;
            taskListBox.getChildren().add(buildTaskRow(taskNode));
        });

        if (taskListBox.getChildren().isEmpty())
            taskListBox.getChildren().add(emptyLabel("Aucune tâche à afficher."));
    }

    // -------------------------------------------------------------------------
    // Construction UI
    // -------------------------------------------------------------------------

    /**
     * Build a clickable task row with description, worker, deadline and state pill.
     * @param taskNode the JSON node representing the task
     * @return the built VBox row
     */
    private VBox buildTaskRow(JsonNode taskNode) {
        int taskId      = taskNode.get("id").asInt();
        String desc     = taskNode.get("description").asText();
        String state    = taskNode.get("state").asText();
        String echeance = taskNode.get("echeance").asText();
        boolean isLate  = state.equals(TaskState.LATE.name());

        Label descLabel = new Label(desc);
        descLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: "
                + Theme.TEXT_PRIMARY + ";");
        HBox.setHgrow(descLabel, Priority.ALWAYS);

        Label statePill = buildStatePill(state);

        HBox topRow = new HBox(8, descLabel, statePill);
        topRow.setAlignment(Pos.CENTER_LEFT);

        String workerText = "—";
        if (taskNode.has("currentWorker") && !taskNode.get("currentWorker").isNull())
            workerText = taskNode.get("currentWorker").get("firstName").asText() + " travaille dessus";

        Label workerLabel = new Label(workerText);
        workerLabel.setStyle(Theme.LABEL_SUBTITLE);
        HBox.setHgrow(workerLabel, Priority.ALWAYS);

        Label dateLabel = new Label(echeance);
        dateLabel.setStyle(isLate ? Theme.LABEL_DANGER : Theme.LABEL_SUBTITLE);

        HBox bottomRow = new HBox(12, workerLabel, dateLabel);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        VBox row = new VBox(4, topRow, bottomRow);
        row.setStyle(
                "-fx-background-color: " + Theme.BG_PRIMARY + ";" +
                        "-fx-border-color: " + Theme.BORDER + ";" +
                        "-fx-border-radius: 8; -fx-background-radius: 8;" +
                        "-fx-padding: 8 10 8 10; -fx-cursor: hand;");

        row.setOnMouseEntered(e -> row.setStyle(
                "-fx-background-color: " + Theme.BG_SECONDARY + ";" +
                        "-fx-border-color: " + Theme.BORDER + ";" +
                        "-fx-border-radius: 8; -fx-background-radius: 8;" +
                        "-fx-padding: 8 10 8 10; -fx-cursor: hand;"));
        row.setOnMouseExited(e -> row.setStyle(
                "-fx-background-color: " + Theme.BG_PRIMARY + ";" +
                        "-fx-border-color: " + Theme.BORDER + ";" +
                        "-fx-border-radius: 8; -fx-background-radius: 8;" +
                        "-fx-padding: 8 10 8 10; -fx-cursor: hand;"));
        row.setOnMouseClicked(e -> {
            if (onTaskClick != null)
                onTaskClick.accept(taskId);
        });

        return row;
    }

    private Label buildStatePill(String stateName) {
        String bg, fg;
        switch (stateName) {
            case "PROGRESSING" -> { bg = Theme.PRIMARY_LIGHT; fg = Theme.PRIMARY;  }
            case "CLOSED"      -> { bg = "#EAF3DE";           fg = Theme.SUCCESS;  }
            case "PAUSED"      -> { bg = "#FAEEDA";           fg = Theme.WARNING;  }
            case "LATE"        -> { bg = Theme.DANGER_LIGHT;  fg = Theme.DANGER;   }
            case "BLOCKED"     -> { bg = "#EEEDFE";           fg = "#533AB7";      }
            default            -> { bg = Theme.BG_SECONDARY;  fg = Theme.MUTED;    }
        }
        Label pill = new Label(stateLabelFor(stateName));
        pill.setStyle(
                "-fx-background-color: " + bg + ";" +
                        "-fx-text-fill: " + fg + ";" +
                        "-fx-background-radius: 99; -fx-padding: 2 7 2 7; -fx-font-size: 10px;");
        return pill;
    }

    /**
     * Return a human-readable French label for a TaskState name.
     * @param stateName the TaskState enum name
     * @return the French label
     */
    private String stateLabelFor(String stateName) {
        return switch (stateName) {
            case "UNOPENED"    -> "Non entamé";
            case "PROGRESSING" -> "En cours";
            case "CLOSED"      -> "Clôturé";
            case "PAUSED"      -> "En pause";
            case "LATE"        -> "En retard";
            case "BLOCKED"     -> "Bloqué";
            case "CANCELED"    -> "Annulé";
            default            -> stateName;
        };
    }

    private Label emptyLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle(Theme.LABEL_SUBTITLE);
        return lbl;
    }

    public VBox getRoot() { return root; }
}