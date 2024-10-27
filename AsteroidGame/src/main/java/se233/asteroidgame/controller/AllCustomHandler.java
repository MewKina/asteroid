package se233.asteroidgame.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

public class AllCustomHandler {
    @FXML private VBox menuPane;
    @FXML private VBox playPane;

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
