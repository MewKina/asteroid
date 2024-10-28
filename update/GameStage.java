package se233.Asteroids_Project.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import se233.Asteroids_Project.controller.GameController;
import se233.Asteroids_Project.model.GameState;

public class GameStage extends Pane {
    private Canvas canvas;
    private final GraphicsContext gc;
    private final GameController gameController;
    private final int stageWidth;
    private final int stageHeight;

    public GameStage(int width, int height) {
        this.stageWidth = width;
        this.stageHeight = height;

        // Set the pane's size
        setPrefSize(stageWidth, stageHeight);
        setMinSize(stageWidth, stageHeight);
        setMaxSize(stageWidth, stageHeight);

        // Create the canvas
        canvas = new Canvas(stageWidth, stageHeight);
        gc = canvas.getGraphicsContext2D();


        try {
            Image backgroundImage = new Image(getClass().getResourceAsStream("/se233/Asteroids_Project/asset/BG_space.png")); // Replace with your image path
            BackgroundImage background = new BackgroundImage(
                    backgroundImage,
                    BackgroundRepeat.NO_REPEAT,  // Don't repeat the image
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.DEFAULT,
                    new BackgroundSize(
                            stageWidth,
                            stageHeight,
                            false,
                            false,
                            false,
                            true
                    )
            );
            setBackground(new Background(background));
        } catch (Exception e) {
            // if load fail
            setStyle("-fx-background-color: #0a0033;");
            e.printStackTrace();
        }

        // Add canvas to the pane
        getChildren().add(canvas);

        // Initialize game controller
        gameController = new GameController(this);

        // Set up key event handlers
        setFocusTraversable(true);
        requestFocus();

        // Handle key events
        setOnKeyPressed(event -> gameController.handleKeyPress(event.getCode()));
        setOnKeyReleased(event -> gameController.handleKeyRelease(event.getCode()));
        setOnMouseMoved(event -> gameController.handleMouseMoved(event));
        // Start the game
        gameController.startGameLoop();
    }

    public GraphicsContext getGraphicsContext() {
        return gc;
    }

    public double getStageWidth() {
        return stageWidth;
    }

    public double getStageHeight() {
        return stageHeight;
    }
}



