package TaskManager.client.views;

import TaskManager.client.style.Theme;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class SidebarView {

    private final VBox root;

    private final Button dashboardBtn;
    private final Button taskListBtn;
    private final Button myTasksBtn;
    private final Button createTaskBtn;

    private Runnable onDashboardClick;
    private Runnable onTaskListClick;
    private Runnable onMyTasksClick;
    private Runnable onCreateTaskClick;

    /**
     * Build the sidebar with the connected collaborator's info and navigation buttons.
     * The sidebar is instantiated once in MainView and never replaced.
     * @param sessionContext the shared session context
     */
    public SidebarView(SessionContext sessionContext) {
        root = new VBox();
        root.setStyle(Theme.SIDEBAR);

        Label loginLabel = new Label("@" + sessionContext.getCurrentCollaborator().getLogin());
        loginLabel.setStyle(Theme.LABEL_SUBTITLE);

        Label nameLabel = new Label(
                sessionContext.getCurrentCollaborator().getFirstName() + " "
                        + sessionContext.getCurrentCollaborator().getLastName());
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: "
                + Theme.TEXT_PRIMARY + ";");

        VBox userBox = new VBox(2, loginLabel, nameLabel);
        userBox.setPadding(new Insets(10, 12, 10, 12));
        userBox.setStyle("-fx-border-color: transparent transparent "
                + Theme.BORDER + " transparent; -fx-border-width: 0 0 0.5 0;");

        dashboardBtn = buildNavBtn("Dashboard");
        taskListBtn  = buildNavBtn("Tâches quotidiennes");
        myTasksBtn   = buildNavBtn("Mes tâches");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        createTaskBtn = buildNavBtn("+ Créer une tâche");
        resetCreateBtn();

        dashboardBtn.setOnAction(e  -> { if (onDashboardClick  != null) onDashboardClick.run();  });
        taskListBtn.setOnAction(e   -> { if (onTaskListClick   != null) onTaskListClick.run();   });
        myTasksBtn.setOnAction(e    -> { if (onMyTasksClick    != null) onMyTasksClick.run();    });
        createTaskBtn.setOnAction(e -> { if (onCreateTaskClick != null) onCreateTaskClick.run(); });

        root.getChildren().addAll(userBox, dashboardBtn, taskListBtn, myTasksBtn, spacer, createTaskBtn);
    }

    // -------------------------------------------------------------------------
    // État actif
    // -------------------------------------------------------------------------

    /** Highlight dashboard button, reset others. */
    public void setActiveDashboard() {
        dashboardBtn.setStyle(Theme.SIDEBAR_ITEM_ACTIVE);
        taskListBtn.setStyle(Theme.SIDEBAR_ITEM);
        myTasksBtn.setStyle(Theme.SIDEBAR_ITEM);
        resetCreateBtn();
    }

    /** Highlight task list button, reset others. */
    public void setActiveTaskList() {
        taskListBtn.setStyle(Theme.SIDEBAR_ITEM_ACTIVE);
        dashboardBtn.setStyle(Theme.SIDEBAR_ITEM);
        myTasksBtn.setStyle(Theme.SIDEBAR_ITEM);
        resetCreateBtn();
    }

    /** Highlight my tasks button, reset others. */
    public void setActiveMyTasks() {
        myTasksBtn.setStyle(Theme.SIDEBAR_ITEM_ACTIVE);
        dashboardBtn.setStyle(Theme.SIDEBAR_ITEM);
        taskListBtn.setStyle(Theme.SIDEBAR_ITEM);
        resetCreateBtn();
    }

    /** Highlight create task button, reset others. */
    public void setActiveCreateTask() {
        createTaskBtn.setStyle(Theme.SIDEBAR_ITEM_ACTIVE);
        dashboardBtn.setStyle(Theme.SIDEBAR_ITEM);
        taskListBtn.setStyle(Theme.SIDEBAR_ITEM);
        myTasksBtn.setStyle(Theme.SIDEBAR_ITEM);
    }

    // -------------------------------------------------------------------------
    // Callbacks
    // -------------------------------------------------------------------------

    public void setOnDashboardClick(Runnable h)  { this.onDashboardClick  = h; }
    public void setOnTaskListClick(Runnable h)   { this.onTaskListClick   = h; }
    public void setOnMyTasksClick(Runnable h)    { this.onMyTasksClick    = h; }
    public void setOnCreateTaskClick(Runnable h) { this.onCreateTaskClick = h; }

    public VBox getRoot() { return root; }

    // -------------------------------------------------------------------------
    // Utilitaires
    // -------------------------------------------------------------------------

    /**
     * Build a navigation button styled as a sidebar item.
     * @param label the button text
     * @return the configured Button
     */
    private Button buildNavBtn(String label) {
        Button btn = new Button(label);
        btn.setStyle(Theme.SIDEBAR_ITEM);
        btn.setMaxWidth(Double.MAX_VALUE);
        return btn;
    }

    /**
     * Reset the create task button to its default primary-colored non-active style.
     */
    private void resetCreateBtn() {
        createTaskBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + Theme.PRIMARY + ";" +
                        "-fx-font-size: 12px;" +
                        "-fx-padding: 7 12 7 12;" +
                        "-fx-cursor: hand;" +
                        "-fx-alignment: center-left;");
    }
}