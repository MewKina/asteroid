package se233.Asteroids_Project.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.*;

import se233.Asteroids_Project.controller.GameController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GameStage extends Pane {
    // Logger for tracking events in this class
    private static final Logger logger = LogManager.getLogger(GameStage.class);

    // Canvas for drawing game elements
    private Canvas canvas;
    private GraphicsContext gc;
    private GameController gameController;

    // Dimensions of the game stage
    private final int stageWidth;
    private final int stageHeight;

    public GameStage(int width, int height) {
        this.stageWidth = width;
        this.stageHeight = height;

        // Set the Pane size to match the stage dimensions
        setPrefSize(stageWidth, stageHeight);
        setMinSize(stageWidth, stageHeight);
        setMaxSize(stageWidth, stageHeight);

        // Initialize the canvas with specified dimensions
        canvas = new Canvas(stageWidth, stageHeight);
        gc = canvas.getGraphicsContext2D();  // Get the GraphicsContext for drawing

        // Load and set the background image
        try {
            Image backgroundImage = new Image(getClass().getResourceAsStream("/se233/Asteroids_Project/asset/BG_Space.png")); // Path to the background image
            BackgroundImage background = new BackgroundImage(
                    backgroundImage,
                    BackgroundRepeat.NO_REPEAT,  // No repetition of the background image
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
            setBackground(new Background(background));  // Set the loaded background to the pane
        } catch (Exception e) {
            logger.error("Loading background image failed", e);
            setStyle("-fx-background-color: #0a0029;");  // Default background color if image fails to load
            e.printStackTrace();
        }

        // Add the canvas to the pane
        getChildren().add(canvas);

        // Initialize the game controller
        gameController = new GameController(this);

        // Make the pane focusable for receiving keyboard events
        setFocusTraversable(true);
        requestFocus();

        // Set up key event handlers for player controls
        setOnKeyPressed(event -> gameController.handleKeyPress(event.getCode()));
        setOnKeyReleased(event -> gameController.handleKeyRelease(event.getCode()));
        setOnMouseMoved(event -> gameController.handleMouseMoved(event));
        setOnMouseClicked(event -> gameController.handleMouseClick(event));

        // Start the game loop
        gameController.startGameLoop();
    }

    // Getter for the GraphicsContext used for drawing on the canvas
    public GraphicsContext getGraphicsContext() {
        return gc;
    }

    // Get the width of the game stage
    public double getStageWidth() {
        return stageWidth;
    }

    // Get the height of the game stage
    public double getStageHeight() {
        return stageHeight;
    }
}
