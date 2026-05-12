package client.views;


import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class FrameHomeView {

    private VBox root;

    public FrameHomeView() {
        root = new VBox();
        root.getChildren().add(new Label("Gestionnaire de tâches"));
    }

    public Parent getRoot() {
        return root;
    }
}
