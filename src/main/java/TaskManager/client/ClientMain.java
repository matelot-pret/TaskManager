package TaskManager.client;

import TaskManager.client.views.FrameHomeView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

public class ClientMain extends Application {

    public void start(Stage stage){
        FrameHomeView homeView = new FrameHomeView();

        Scene scene = new Scene(homeView.getRoot(), 900, 600);

        stage.setTitle("Gestionnaire de tâches");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}