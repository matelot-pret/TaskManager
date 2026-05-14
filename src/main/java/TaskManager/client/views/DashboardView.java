package TaskManager.client.views;

import TaskManager.client.style.Theme;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

import java.util.function.Consumer;

public class DashboardView {

    private final VBox root;
    private final VBox lateTasksBox;
    private final GridPane statusGrid;
    private final VBox collabBox;
    private final SessionContext sessionContext;

    private Consumer<Integer> onTaskClick;
    private Consumer<String> onStatusFilterClick;

    /**
     * Build the dashboard view with three sections.
     * Listeners are registered immediately; data is fetched only when refresh() is called.
     * @param sessionContext the shared session context
     */
    public DashboardView(SessionContext sessionContext) {
        this.sessionContext = sessionContext;

        root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: " + Theme.BG_PRIMARY + ";");

        lateTasksBox = new VBox(0);
        statusGrid   = new GridPane();
        statusGrid.setHgap(8);
        statusGrid.setVgap(8);
        collabBox = new VBox(0);

        root.getChildren().addAll(
                buildSection("Tâches en retard",      buildScrollPane(lateTasksBox, 180)),
                buildSection("Tâches par statut",     statusGrid),
                buildSection("Temps par collaborateur", buildScrollPane(collabBox, 200))
        );

        registerWsListeners();
    }

    // -------------------------------------------------------------------------
    // API publique
    // -------------------------------------------------------------------------

    /** Request fresh data — called by MainView when this view becomes visible. */
    public void refresh() { requestDashboardData(); }

    public void setOnTaskClick(Consumer<Integer> handler)         { this.onTaskClick = handler; }
    public void setOnStatusFilterClick(Consumer<String> handler)  { this.onStatusFilterClick = handler; }

    // -------------------------------------------------------------------------
    // Chargement
    // -------------------------------------------------------------------------

    private void requestDashboardData() {
        try {
            sessionContext.getWsClient().send("{\"action\":\"GET_DASHBOARD\"}");
        } catch (Exception e) {
            System.err.println("[Dashboard] Erreur requête : " + e.getMessage());
        }
    }

    /**
     * Register WebSocket listeners.
     * DASHBOARD_DATA  : refresh all sections.
     * UPDATE/CREATE/DELETE_TASK : re-request data.
     */
    private void registerWsListeners() {
        sessionContext.getWsClient().on("DASHBOARD_DATA", json ->
                Platform.runLater(() -> {
                    refreshLateTasks(json);
                    refreshStatusCounts(json);
                    refreshCollaborators(json);
                })
        );
        sessionContext.getWsClient().on("UPDATE_TASK", json -> Platform.runLater(this::requestDashboardData));
        sessionContext.getWsClient().on("CREATE_TASK", json -> Platform.runLater(this::requestDashboardData));
        sessionContext.getWsClient().on("DELETE_TASK", json -> Platform.runLater(this::requestDashboardData));
    }

    // -------------------------------------------------------------------------
    // Sections
    // -------------------------------------------------------------------------

    /**
     * Refresh late tasks using a two-column GridPane (task | deadline).
     * Each row is clickable.
     * @param json contains "lateTasks" array with id, description, echeance
     */
    private void refreshLateTasks(JsonNode json) {
        lateTasksBox.getChildren().clear();

        // En-tête
        GridPane header = buildTwoColHeader("Tâche", "Échéance");
        lateTasksBox.getChildren().add(header);

        boolean[] hasRows = {false};
        json.get("lateTasks").forEach(taskNode -> {
            hasRows[0] = true;
            int taskId = taskNode.has("id") ? taskNode.get("id").asInt() : -1;

            GridPane row = new GridPane();
            row.setPadding(new Insets(6, 0, 6, 0));
            row.setStyle("-fx-border-color: transparent transparent " + Theme.BORDER +
                    " transparent; -fx-border-width: 0 0 0.5 0; -fx-cursor: hand;");
            setupTwoColConstraints(row);

            Label desc = new Label(taskNode.get("description").asText());
            desc.setStyle("-fx-font-size: 12px; -fx-text-fill: " + Theme.TEXT_PRIMARY + ";");
            desc.setWrapText(false);

            Label date = new Label(taskNode.get("echeance").asText());
            date.setStyle("-fx-font-size: 12px; -fx-text-fill: " + Theme.DANGER + ";");
            date.setAlignment(Pos.CENTER_LEFT);

            row.add(desc, 0, 0);
            row.add(date, 1, 0);

            row.setOnMouseEntered(e -> row.setStyle(
                    "-fx-background-color: " + Theme.BG_SECONDARY + "; -fx-cursor: hand;" +
                            "-fx-border-color: transparent transparent " + Theme.BORDER + " transparent;" +
                            "-fx-border-width: 0 0 0.5 0;"));
            row.setOnMouseExited(e -> row.setStyle(
                    "-fx-border-color: transparent transparent " + Theme.BORDER + " transparent;" +
                            "-fx-border-width: 0 0 0.5 0; -fx-cursor: hand;"));
            row.setOnMouseClicked(e -> {
                if (onTaskClick != null && taskId != -1) onTaskClick.accept(taskId);
            });

            lateTasksBox.getChildren().add(row);
        });

        if (!hasRows[0]) lateTasksBox.getChildren().add(emptyLabel("Aucune tâche en retard"));
    }

    /**
     * Refresh status counts — each badge is clickable to filter the task list.
     * @param json contains "statusCounts" object
     */
    private void refreshStatusCounts(JsonNode json) {
        statusGrid.getChildren().clear();

        String[] states = {"UNOPENED","PROGRESSING","CLOSED","PAUSED","LATE","BLOCKED","CANCELED"};
        String[] labels = {"Non entamé","En cours","Clôturé","En pause","En retard","Bloqué","Annulé"};
        String[] colors = {Theme.MUTED, Theme.PRIMARY, Theme.SUCCESS, Theme.WARNING,
                Theme.DANGER, "#533AB7", "#993556"};

        int col = 0, row = 0;
        for (int i = 0; i < states.length; i++) {
            int count = json.get("statusCounts").has(states[i])
                    ? json.get("statusCounts").get(states[i]).asInt() : 0;
            String stateName = states[i];
            HBox badge = buildStatusBadge(colors[i], labels[i], count);
            badge.setOnMouseEntered(e -> badge.setStyle(badge.getStyle()
                    .replace(Theme.BG_SECONDARY, Theme.BORDER)));
            badge.setOnMouseExited(e -> badge.setStyle(
                    "-fx-background-color: " + Theme.BG_SECONDARY + ";" +
                            "-fx-border-color: " + Theme.BORDER + ";" +
                            "-fx-border-radius: 8; -fx-background-radius: 8;" +
                            "-fx-padding: 5 8 5 8; -fx-cursor: hand;"));
            badge.setOnMouseClicked(e -> {
                if (onStatusFilterClick != null) onStatusFilterClick.accept(stateName);
            });
            statusGrid.add(badge, col, row);
            col++;
            if (col == 2) { col = 0; row++; }
        }
    }

    /**
     * Refresh collaborator times using a two-column GridPane (name | time).
     * Only shows collaborators with recorded time.
     * @param json contains "collaborators" array
     */
    private void refreshCollaborators(JsonNode json) {
        collabBox.getChildren().clear();

        GridPane header = buildTwoColHeader("Collaborateur", "Temps total");
        collabBox.getChildren().add(header);

        boolean[] hasRows = {false};
        json.get("collaborators").forEach(collabNode -> {
            long minutes = collabNode.get("totalMinutes").asLong();
            if (minutes == 0) return;
            hasRows[0] = true;

            GridPane row = new GridPane();
            row.setPadding(new Insets(6, 0, 6, 0));
            row.setStyle("-fx-border-color: transparent transparent " + Theme.BORDER +
                    " transparent; -fx-border-width: 0 0 0.5 0;");
            setupTwoColConstraints(row);

            Label name = new Label(collabNode.get("firstName").asText()
                    + " " + collabNode.get("lastName").asText());
            name.setStyle("-fx-font-size: 12px; -fx-text-fill: " + Theme.TEXT_PRIMARY + ";");

            Label time = new Label(formatMinutes(minutes));
            time.setStyle("-fx-font-size: 12px; -fx-text-fill: " + Theme.TEXT_SECONDARY + ";");
            time.setAlignment(Pos.CENTER_LEFT);

            row.add(name, 0, 0);
            row.add(time, 1, 0);
            collabBox.getChildren().add(row);
        });

        if (!hasRows[0]) collabBox.getChildren().add(emptyLabel("Aucun temps enregistré"));
    }

    // -------------------------------------------------------------------------
    // Utilitaires UI
    // -------------------------------------------------------------------------

    /**
     * Build a two-column GridPane header row with bold labels.
     * @param col1 label for the first column
     * @param col2 label for the second column
     * @return the configured GridPane
     */
    private GridPane buildTwoColHeader(String col1, String col2) {
        GridPane header = new GridPane();
        header.setPadding(new Insets(0, 0, 4, 0));
        header.setStyle("-fx-border-color: transparent transparent " + Theme.BORDER +
                " transparent; -fx-border-width: 0 0 1 0;");
        setupTwoColConstraints(header);

        Label l1 = new Label(col1.toUpperCase());
        l1.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + Theme.TEXT_SECONDARY + ";");
        Label l2 = new Label(col2.toUpperCase());
        l2.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + Theme.TEXT_SECONDARY + ";");

        header.add(l1, 0, 0);
        header.add(l2, 1, 0);
        return header;
    }

    /**
     * Configure a GridPane with two columns : 70% for col0, 30% for col1.
     * @param grid the GridPane to configure
     */
    private void setupTwoColConstraints(GridPane grid) {
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(70);
        c1.setHgrow(Priority.ALWAYS);

        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(30);
        c2.setHgrow(Priority.NEVER);

        grid.getColumnConstraints().setAll(c1, c2);
        grid.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(grid, Priority.ALWAYS);
        VBox.setVgrow(grid, Priority.NEVER);
    }

    private VBox buildSection(String title, javafx.scene.Node content) {
        Label t = new Label(title.toUpperCase());
        t.setStyle(Theme.LABEL_SECTION);
        return new VBox(8, t, content);
    }

    private ScrollPane buildScrollPane(VBox content, double maxHeight) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(maxHeight);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;" +
                "-fx-border-color: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        return scroll;
    }

    private HBox buildStatusBadge(String color, String label, int count) {
        Circle dot = new Circle(5);
        dot.setStyle("-fx-fill: " + color + ";");

        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: " + Theme.TEXT_SECONDARY + ";");
        HBox.setHgrow(lbl, Priority.ALWAYS);

        Label cnt = new Label(String.valueOf(count));
        cnt.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + Theme.TEXT_PRIMARY + ";");

        HBox badge = new HBox(6, dot, lbl, cnt);
        badge.setStyle(
                "-fx-background-color: " + Theme.BG_SECONDARY + ";" +
                        "-fx-border-color: " + Theme.BORDER + ";" +
                        "-fx-border-radius: 8; -fx-background-radius: 8;" +
                        "-fx-padding: 5 8 5 8; -fx-cursor: hand;");
        return badge;
    }

    private String formatMinutes(long totalMinutes) {
        return String.format("%02dh%02dm", totalMinutes / 60, totalMinutes % 60);
    }

    private Label emptyLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle(Theme.LABEL_SUBTITLE);
        return lbl;
    }

    public VBox getRoot() { return root; }
}