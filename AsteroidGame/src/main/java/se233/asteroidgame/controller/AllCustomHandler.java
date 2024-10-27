package se233.asteroidgame.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

public class AllCustomHandler {
    @FXML private VBox menuPane;
    @FXML private VBox playPane;
    @FXML private Button startButton;
    @FXML private Button quitButton;

    @FXML
    private void initialize() {
        // Set up mouse hover effects for the Start button
        startButton.setOnMouseEntered(e -> startButton.setText("> " + startButton.getText()));
        startButton.setOnMouseExited(e -> startButton.setText(startButton.getText().replace("> ", "")));

        // Set up mouse hover effects for the Quit button
        quitButton.setOnMouseEntered(e -> quitButton.setText("> " + quitButton.getText()));
        quitButton.setOnMouseExited(e -> quitButton.setText(quitButton.getText().replace("> ", "")));
    }

    @FXML
    private void startGame(ActionEvent event) {
        menuPane.setVisible(false);
        playPane.setVisible(true);
    }

    @FXML
    private void quitGame(ActionEvent event) {
        System.exit(0);
    }
}
