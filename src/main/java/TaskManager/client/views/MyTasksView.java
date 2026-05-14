package TaskManager.client.views;

import TaskManager.client.style.Theme;
import TaskManager.shared.models.TaskState;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.util.function.Consumer;

public class MyTasksView {

    private final VBox root;
    private final VBox createdBox;
    private final VBox currentBox;
    private final SessionContext sessionContext;
    private final Consumer<Integer> onTaskClick;

    /**
     * Build the "Mes tâches" view with two sections :
     * - Tasks created by the connected collaborator (all states including CANCELED)
     * - Tasks currently being worked on by the connected collaborator
     * A task can appear in both sections.
     * @param sessionContext the shared session context
     * @param onTaskClick callback invoked with the taskId when a row is clicked
     */
    public MyTasksView(SessionContext sessionContext, Consumer<Integer> onTaskClick) {
        this.sessionContext = sessionContext;
        this.onTaskClick    = onTaskClick;

        root = new VBox(24);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: " + Theme.BG_PRIMARY + ";");

        createdBox = new VBox(8);
        currentBox = new VBox(8);

        ScrollPane createdScroll = buildScrollPane(createdBox);
        ScrollPane currentScroll = buildScrollPane(currentBox);

        root.getChildren().addAll(
                buildSection("Tâches créées par moi", createdScroll),
                buildSection("Tâches en cours (je travaille dessus)", currentScroll)
        );

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
        try {
            sessionContext.getWsClient().send("{\"action\":\"GET_TASKS\"}");
        } catch (Exception e) {
            System.err.println("[MyTasks] Erreur requête : " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Chargement
    // -------------------------------------------------------------------------

    /**
     * Register WebSocket listeners.
     * TASKS_DATA          : rebuild both sections from the full task list.
     * UPDATE/CREATE/DELETE_TASK : re-fetch.
     */
    private void registerWsListeners() {
        sessionContext.getWsClient().on("TASKS_DATA", json ->
                Platform.runLater(() -> populate(json))
        );
        sessionContext.getWsClient().on("UPDATE_TASK", json -> Platform.runLater(this::refresh));
        sessionContext.getWsClient().on("CREATE_TASK", json -> Platform.runLater(this::refresh));
        sessionContext.getWsClient().on("DELETE_TASK", json -> Platform.runLater(this::refresh));
    }

    /**
     * Populate both sections from the received task list.
     * Created section : all tasks where creator.id == myId (all states).
     * Current section : all tasks where currentWorker.id == myId.
     * @param json the JSON node containing a "tasks" array
     */
    private void populate(JsonNode json) {
        int myId = sessionContext.getCurrentCollaborator().getId();
        createdBox.getChildren().clear();
        currentBox.getChildren().clear();

        boolean hasCreated = false;
        boolean hasCurrent = false;

        for (JsonNode taskNode : json.get("tasks")) {
            int creatorId = taskNode.get("creator").get("id").asInt();
            boolean hasWorker = taskNode.has("currentWorker")
                    && !taskNode.get("currentWorker").isNull();
            int workerId = hasWorker ? taskNode.get("currentWorker").get("id").asInt() : -1;

            if (creatorId == myId) {
                createdBox.getChildren().add(buildTaskRow(taskNode));
                hasCreated = true;
            }
            if (workerId == myId) {
                currentBox.getChildren().add(buildTaskRow(taskNode));
                hasCurrent = true;
            }
        }

        if (!hasCreated) createdBox.getChildren().add(emptyLabel("Aucune tâche créée."));
        if (!hasCurrent) currentBox.getChildren().add(emptyLabel("Vous ne travaillez sur aucune tâche."));
    }

    // -------------------------------------------------------------------------
    // Construction UI
    // -------------------------------------------------------------------------

    /**
     * Build a clickable task row identical to TaskListView format.
     * Shows description, state pill, worker name and deadline.
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

        HBox bottomRow = new HBox(workerLabel, dateLabel);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        VBox row = new VBox(4, topRow, bottomRow);
        row.setStyle(rowStyle(false));
        row.setOnMouseEntered(e -> row.setStyle(rowStyle(true)));
        row.setOnMouseExited(e  -> row.setStyle(rowStyle(false)));
        row.setOnMouseClicked(e -> { if (onTaskClick != null) onTaskClick.accept(taskId); });

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
            case "CANCELED"    -> { bg = "#F1EFE8";           fg = Theme.MUTED;    }
            default            -> { bg = Theme.BG_SECONDARY;  fg = Theme.MUTED;    }
        }
        Label pill = new Label(stateLabelFor(stateName));
        pill.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + ";" +
                "-fx-background-radius: 99; -fx-padding: 2 7 2 7; -fx-font-size: 10px;");
        return pill;
    }

    private VBox buildSection(String title, javafx.scene.Node content) {
        Label t = new Label(title.toUpperCase());
        t.setStyle(Theme.LABEL_SECTION);
        VBox section = new VBox(8, t, content);
        VBox.setVgrow(content, Priority.ALWAYS);
        return section;
    }

    private ScrollPane buildScrollPane(VBox content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(250);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;" +
                "-fx-border-color: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        return scroll;
    }

    private String rowStyle(boolean hovered) {
        String bg = hovered ? Theme.BG_SECONDARY : Theme.BG_PRIMARY;
        return "-fx-background-color: " + bg + ";"
                + "-fx-border-color: " + Theme.BORDER + ";"
                + "-fx-border-radius: 8; -fx-background-radius: 8;"
                + "-fx-padding: 8 10 8 10; -fx-cursor: hand;";
    }

    private Label emptyLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle(Theme.LABEL_SUBTITLE);
        return lbl;
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