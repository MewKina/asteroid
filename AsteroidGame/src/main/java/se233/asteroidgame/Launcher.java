package se233.asteroidgame;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class Launcher extends Application {
    public static void main(String[] args) {
        launch(args);
    }
    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("mainView.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);

        primaryStage.setScene(scene);
        primaryStage.setTitle("Asteroid Game");
        primaryStage.show();
    }
}
