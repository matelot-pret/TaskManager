package TaskManager.client.views;

import TaskManager.client.style.Theme;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;

public class MainView {

    private final BorderPane root;
    private final SidebarView sidebarView;
    private final SessionContext sessionContext;

    private final DashboardView dashboardView;
    private final TaskListView taskListView;
    private final MyTasksView myTasksView;
    private final CreateTaskView createTaskView;

    /**
     * Build the main container and instantiate ALL child views immediately.
     * Every view registers its WebSocket listeners in its constructor — they must
     * all exist before any server message arrives, otherwise messages are lost.
     * Only the center content changes when the user navigates.
     * @param sessionContext the shared session context passed to all child views
     */
    public MainView(SessionContext sessionContext) {
        this.sessionContext = sessionContext;

        root = new BorderPane();
        root.setStyle("-fx-background-color: " + Theme.BG_SECONDARY + ";");

        sidebarView = new SidebarView(sessionContext);
        root.setLeft(sidebarView.getRoot());

        // All views instantiated here — WS listeners registered immediately
        dashboardView  = new DashboardView(sessionContext);
        taskListView   = new TaskListView(sessionContext, this::showTaskDetailFromList);
        myTasksView    = new MyTasksView(sessionContext, this::showTaskDetailFromMyTasks);
        createTaskView = new CreateTaskView(sessionContext, () -> {
            sidebarView.setActiveTaskList();
            showTaskList();
        });

        // Dashboard callbacks
        dashboardView.setOnTaskClick(this::showTaskDetailFromDashboard);
        dashboardView.setOnStatusFilterClick(stateName -> {
            sidebarView.setActiveTaskList();
            taskListView.filterByState(stateName);
            root.setCenter(taskListView.getRoot());
        });

        wireSidebarNavigation();
        showDashboard();
    }

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------

    /**
     * Wire sidebar buttons to their respective show methods.
     */
    private void wireSidebarNavigation() {
        sidebarView.setOnDashboardClick(() -> {
            sidebarView.setActiveDashboard();
            showDashboard();
        });
        sidebarView.setOnTaskListClick(() -> {
            sidebarView.setActiveTaskList();
            showTaskList();
        });
        sidebarView.setOnMyTasksClick(() -> {
            sidebarView.setActiveMyTasks();
            showMyTasks();
        });
        sidebarView.setOnCreateTaskClick(() -> {
            sidebarView.setActiveCreateTask();
            showCreateTask();
        });
    }

    private void showDashboard() {
        ScrollPane scroll = new ScrollPane(dashboardView.getRoot());
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;" +
                "-fx-border-color: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        root.setCenter(scroll);
        dashboardView.refresh();
        sidebarView.setActiveDashboard();
    }

    private void showTaskList() {
        root.setCenter(taskListView.getRoot());
        taskListView.refresh();
        sidebarView.setActiveTaskList();
    }

    private void showMyTasks() {
        root.setCenter(myTasksView.getRoot());
        myTasksView.refresh();
        sidebarView.setActiveMyTasks();
    }

    private void showCreateTask() {
        root.setCenter(createTaskView.getRoot());
        sidebarView.setActiveCreateTask();
    }

    /** Currently displayed TaskDetailView — kept to call unregister() before replacement. */
    private TaskDetailView currentDetailView = null;

    /**
     * Show the detail view for a specific task.
     * Unregisters the previous TaskDetailView's listeners before creating the new one,
     * preventing listener accumulation when the user opens multiple task details.
     * @param taskId the id of the task to display
     * @param onBack the callback to invoke when the user clicks "Retour"
     */
    public void showTaskDetail(int taskId, Runnable onBack) {
        if (currentDetailView != null)
            currentDetailView.unregister();
        currentDetailView = new TaskDetailView(sessionContext, taskId, onBack);
        root.setCenter(currentDetailView.getRoot());
    }

    /** Show task detail from the task list — Retour goes back to task list. */
    private void showTaskDetailFromList(int taskId) {
        showTaskDetail(taskId, () -> {
            sidebarView.setActiveTaskList();
            showTaskList();
        });
    }

    /** Show task detail from my tasks — Retour goes back to my tasks. */
    private void showTaskDetailFromMyTasks(int taskId) {
        showTaskDetail(taskId, () -> {
            sidebarView.setActiveMyTasks();
            showMyTasks();
        });
    }

    /** Show task detail from dashboard — Retour goes back to dashboard. */
    private void showTaskDetailFromDashboard(int taskId) {
        showTaskDetail(taskId, () -> {
            sidebarView.setActiveDashboard();
            showDashboard();
        });
    }

    public BorderPane getRoot() { return root; }
}