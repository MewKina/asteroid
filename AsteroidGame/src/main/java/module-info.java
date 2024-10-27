module se233.asteroidgame {
    requires javafx.controls;
    requires javafx.fxml;


    opens se233.asteroidgame to javafx.fxml;
    exports se233.asteroidgame;
    exports se233.asteroidgame.controller;
    opens se233.asteroidgame.controller to javafx.fxml;
}