package se233.Asteroids_Project.controller;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import se233.Asteroids_Project.model.*;
import se233.Asteroids_Project.model.Animation.ExplosionEffect;
import se233.Asteroids_Project.model.Entities.Asteroid;
import se233.Asteroids_Project.model.PlayerAsset.Player;
import se233.Asteroids_Project.model.PlayerAsset.Projectile;
import se233.Asteroids_Project.view.GameStage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class GameController {
    private static final Logger logger = LogManager.getLogger(GameController.class);

    private final GameStage gameStage;
    private AnimationTimer gameLoop;
    private boolean isRunning;
    private final Image lifeIcon;

    // Game objects
    private Player player;
    private List<Asteroid> asteroids;
    private List<Projectile> projectiles;
    private List<ExplosionEffect> explosionEffects;

    // Game state
    private GameState gameState = GameState.MAINMENU;
    private int level;
    private int score;
    private double spawnTimer;
    private static final double SPAWN_INTERVAL = 3.0;

    // Menu animation
    private double textAlpha = 1.0;
    private double textAlphaChange = -0.02;

    // Bomb ability display
    private static final Color BOMB_READY_COLOR = Color.GREEN;
    private static final Color BOMB_COOLDOWN_COLOR = Color.RED;


    public GameController(GameStage gameStage) {
        this.gameStage = gameStage;
        this.isRunning = false;
        this.lifeIcon = new Image(getClass().getResourceAsStream("/se233/Asteroids_Project/asset/player_ship.png"));
        initializeGame();
    }

    private void initializeGame() {
        // Create player at center of screen
        double centerX = gameStage.getStageWidth() / 2;
        double centerY = gameStage.getStageHeight() / 2;
        player = new Player(centerX, centerY, gameStage.getStageWidth(), gameStage.getStageHeight());
        logger.info("Player initialized at ({}, {})", centerX, centerY);

        // Initialize game objects
        asteroids = new ArrayList<>();
        explosionEffects = new ArrayList<>();
        projectiles = new ArrayList<>();
        level = 1;
        score = 0;
        spawnTimer = SPAWN_INTERVAL;

        gameLoop = new AnimationTimer() {
            private long lastUpdate = 0;
            @Override
            public void handle(long now) {
                // Limit updates to ~60 FPS
                if (lastUpdate == 0 || now - lastUpdate >= 16_666_666) {
                    updateGame();
                    renderGame();
                    lastUpdate = now;
                }
            }
        };


        // Spawn some asteroids for menu background
        spawnAsteroids(3);
    }

    public void rotatePlayerToCursor(double mouseX, double mouseY) {
        if (player != null && player.isAlive()) { // Ensure player is initialized and alive
            player.rotateToCursor(mouseX, mouseY); // Update player rotation
        }
    }

    // You can add a method to handle mouse events
    public void handleMouseMoved(MouseEvent event) {
        rotatePlayerToCursor(event.getX(), event.getY());
    }

    // Getter for player
    public Player getPlayer() {
        return player;
    }

    private void updateGame() {
        switch (gameState) {
            case MAINMENU:
                updateMenu();
                break;
            case PLAYGROUND:
                updatePlaying();
                break;
            case GAMEOVER:
                updateGameOver();
                break;
        }
    }

    private void updateMenu() {
        // Update asteroids for background movement
        for (Asteroid asteroid : asteroids) {
            asteroid.update();
        }

        // Update text fade effect
        textAlpha += textAlphaChange;
        if (textAlpha <= 0 || textAlpha >= 1) {
            textAlphaChange *= -1;
        }
    }

    private void updatePlaying() {
        if (!player.isAlive()) {
            gameState = GameState.GAMEOVER;
            return;
        }

        // Update player
        player.update();

        // Update explosions
        Iterator<ExplosionEffect> explosionIterator = explosionEffects.iterator();
        while (explosionIterator.hasNext()) {
            ExplosionEffect explosionEffect = explosionIterator.next();
            explosionEffect.update();
            if (explosionEffect.isFinished()) {
                explosionIterator.remove();
            }
        }

        // Update projectiles
        Iterator<Projectile> projectileIterator = projectiles.iterator();
        while (projectileIterator.hasNext()) {
            Projectile projectile = projectileIterator.next();
            projectile.update();
            if (projectile.isExpired()) {
                projectileIterator.remove();
                continue;
            }

            // Check collisions with asteroids
            for (Asteroid asteroid : asteroids) {
                if (CollisionCheck.checkCollision(projectile, asteroid)) {
                    projectileIterator.remove();
                    handleAsteroidDestruction(asteroid);
                    break;
                }
            }
        }

        // Update asteroids
        Iterator<Asteroid> asteroidIterator = asteroids.iterator();
        while (asteroidIterator.hasNext()) {
            Asteroid asteroid = asteroidIterator.next();
            asteroid.update();
            if (asteroid.isMarkedForDestruction()) {
                asteroidIterator.remove();
            }
        }

        // Handle all collisions
        CollisionCheck.handleCollisions(player, asteroids, projectiles);

        // Update spawn timer
        spawnTimer -= 0.016;
        if (spawnTimer <= 0) {
            spawnAsteroids(1);
            spawnTimer = SPAWN_INTERVAL;
        }
    }

    private void updateGameOver() {
        // Update text fade effect
        textAlpha += textAlphaChange;
        if (textAlpha <= 0 || textAlpha >= 1) {
            textAlphaChange *= -1;
        }
    }

    private void renderGame() {
        var gc = gameStage.getGraphicsContext();
        gc.clearRect(0, 0, gameStage.getStageWidth(), gameStage.getStageHeight());

        switch (gameState) {
            case MAINMENU:
                renderMenu(gc);
                break;
            case PLAYGROUND:
                renderPlaying(gc);
                break;
            case GAMEOVER:
                renderGameOver(gc);
                break;
        }
    }

    private void renderMenu(GraphicsContext gc) {
        // Render background asteroids
        for (Asteroid asteroid : asteroids) {
            asteroid.render(gc);
        }

        // Draw title
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 40));
        double titleX = gameStage.getStageWidth() / 2;
        double titleY = gameStage.getStageHeight() / 3;
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("Asteroids Project", titleX, titleY);

        // Draw blinking "Press ENTER to Start" text
        gc.setGlobalAlpha(textAlpha);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        gc.fillText("Press ENTER to Start", titleX, gameStage.getStageHeight() / 2);
        gc.setGlobalAlpha(1.0);

        // Draw controls info
        gc.setFont(Font.font("Arial", 16));
        double infoY = gameStage.getStageHeight() * 0.65;
        gc.fillText("Controls:", titleX, infoY);
        gc.fillText("W - Move", titleX, infoY + 25);
        gc.fillText("A - Rotate left", titleX, infoY + 50);
        gc.fillText("D - Rotate right", titleX, infoY + 75);
        gc.fillText("SPACE - Attack", titleX, infoY + 100);
        gc.fillText("E - Special Attack", titleX, infoY + 125);
        gc.fillText("ESC - Quit", titleX, infoY + 150);
    }

    private void renderPlaying(GraphicsContext gc) {
        // Render game objects
        for (Asteroid asteroid : asteroids) {
            asteroid.render(gc);
        }

        for (Projectile projectile : projectiles) {
            projectile.render(gc);
        }

        for (ExplosionEffect explosionEffect : explosionEffects) {
            explosionEffect.render(gc);
        }

        if (player.isAlive()) {
            player.render(gc);
        }

        // Draw HUD
        renderHUD(gc);
    }

    private void renderHUD(GraphicsContext gc) {
        // Draw score and lives
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 20));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText("Score: " + score, 10, 30);
        gc.fillText("Lives: ", 10, 60);

        // Draw life icons
        double iconSize = 20;
        double baseX = 70;
        double baseY = 45;
        double spacing = 25;
        for (int i = 0; i < player.getLives(); i++) {
            gc.drawImage(lifeIcon,
                    baseX + (i * spacing),
                    baseY,
                    iconSize,
                    iconSize);
        }

//        // Draw bomb status
//        double cooldown = player.getBombCooldown();
//        String bombText = cooldown > 0
//                ? String.format("Bomb: %.1fs", cooldown)
//                : "Bomb: READY";
//
//        gc.setFill(cooldown > 0 ? BOMB_COOLDOWN_COLOR : BOMB_READY_COLOR);
//        gc.setFont(Font.font("Arial", 16));
//        gc.fillText(bombText, 10, gameStage.getStageHeight() - 10);
    }

    private void renderGameOver(GraphicsContext gc) {
        // Render the final game state in background
        renderPlaying(gc);

        // Draw semi-transparent overlay
        gc.setFill(new Color(0, 0, 0, 0.7));
        gc.fillRect(0, 0, gameStage.getStageWidth(), gameStage.getStageHeight());

        // Draw game over text
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 40));
        gc.setTextAlign(TextAlignment.CENTER);
        double centerX = gameStage.getStageWidth() / 2;
        double centerY = gameStage.getStageHeight() / 2;

        gc.fillText("GAME OVER", centerX, centerY - 40);
        gc.setFont(Font.font("Arial", 20));
        gc.fillText("Final Score: " + score, centerX, centerY + 10);

        gc.setGlobalAlpha(textAlpha);
        gc.fillText("Press ENTER to Play Again", centerX, centerY + 50);
        gc.fillText("Press ESC to Quit", centerX, centerY + 75);
        gc.setGlobalAlpha(1.0);
    }

    private void handleAsteroidDestruction(Asteroid asteroid) {
        asteroid.markForDestruction();
        score += asteroid.getPoints();
        explosionEffects.add(new ExplosionEffect(
                asteroid.getX() + asteroid.getWidth()/2,
                asteroid.getY() + asteroid.getHeight()/2
        ));
        logger.info("Asteroids destroyed! Score: {}", score);
    }

    private Optional<Asteroid> findNearestAsteroid() {
        double shortestDistance = Double.MAX_VALUE;
        Asteroid nearest = null;

        for (Asteroid asteroid : asteroids) {
            double dx = asteroid.getX() - player.getX();
            double dy = asteroid.getY() - player.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance < shortestDistance) {
                shortestDistance = distance;
                nearest = asteroid;
            }
        }

        return Optional.ofNullable(nearest);
    }

    public void handleKeyPress(KeyCode code) {
        switch (gameState) {
            case MAINMENU:
                if (code == KeyCode.ENTER) {
                    startNewGame();
                    gameState = GameState.PLAYGROUND;
                }
                if (code == KeyCode.ESCAPE) {
                    Platform.exit();
                }
                break;
            case PLAYGROUND:
                handlePlayingKeyPress(code);
                if (code == KeyCode.ESCAPE) {
                    Platform.exit();
                }
                break;
            case GAMEOVER:
                if (code == KeyCode.ENTER) {
                    startNewGame();
                    gameState = GameState.PLAYGROUND;
                }
                if (code == KeyCode.ESCAPE) {
                    Platform.exit();
                }
                break;
        }
    }

    private void handlePlayingKeyPress(KeyCode code) {
        if (!player.isAlive()) return;

        switch (code) {
            case W:
                player.setMovingForward(true);
                break;
            case S:
                player.setMovingBackward(true);
                break;
            case A:
                player.setMovingLeft(true);
                break;
            case D:
                player.setMovingRight(true);
                break;
//            case A:
//                player.setRotatingLeft(true);
//                break;
//            case D:
//                player.setRotatingRight(true);
//                break;
            case SPACE:
                fireProjectile();
                break;
            case E:
//                activateBomb();
                break;
        }
    }

    public void handleKeyRelease(KeyCode code) {
        if (gameState != GameState.PLAYGROUND || !player.isAlive()) return;

        switch (code) {
            case W:
                player.setMovingForward(false);
                break;
            case S:
                player.setMovingBackward(false);
                break;
            case A:
                player.setMovingLeft(false);
                break;
            case D:
                player.setMovingRight(false);
                break;
//            case A:
//                player.setRotatingLeft(false);
//                break;
//            case D:
//                player.setRotatingRight(false);
//                break;
        }
    }


    private void fireProjectile() {
        if (player.canShoot()) {
            // Calculate projectile spawn position (slightly in front of the ship)
            double angleRad = Math.toRadians(player.getRotation());
            double spawnDistance = 20;
            double projectileX = player.getX() + Math.cos(angleRad) * spawnDistance;
            double projectileY = player.getY() + Math.sin(angleRad) * spawnDistance;

            Projectile projectile = new Projectile(
                    projectileX, projectileY,
                    player.getRotation(),
                    gameStage.getStageWidth(),
                    gameStage.getStageHeight()
            );
            projectiles.add(projectile);
            player.resetShootCooldown();
            logger.debug("Projectile fired from ({}, {})", projectileX, projectileY);
        }
    }

    private void startNewGame() {
        // Reset game state
        score = 0;
        level = 1;
        spawnTimer = SPAWN_INTERVAL;

        // Clear existing objects
        asteroids.clear();
        projectiles.clear();
        explosionEffects.clear();

        // Reset player
        double centerX = gameStage.getStageWidth() / 2;
        double centerY = gameStage.getStageHeight() / 2;
        player = new Player(centerX, centerY, gameStage.getStageWidth(), gameStage.getStageHeight());

        // Spawn initial asteroids
        spawnAsteroids(3);

        logger.info("New game started");
    }

    private void spawnAsteroids(int count) {
        for (int i = 0; i < count; i++) {
            double x, y;
            if (Math.random() < 0.5) {
                x = Math.random() < 0.5 ? -30 : gameStage.getStageWidth() + 30;
                y = Math.random() * gameStage.getStageHeight();
            } else {
                x = Math.random() * gameStage.getStageWidth();
                y = Math.random() < 0.5 ? -30 : gameStage.getStageHeight() + 30;
            }

            int asteroidSize = generateRandomAsteroidSize();
            asteroids.add(new Asteroid(x, y, asteroidSize));
        }
    }

    private int generateRandomAsteroidSize() {
        double random = Math.random();

        if (random < 0.35) {
            return 1; // 35% chance for size 1
        } else if (random < 0.7) {
            return 2; // 35% chance for size 2
        } else {
            return 3; // 30% chance for size 3
        }
    }

    public void startGameLoop() {
        if (!isRunning) {
            gameLoop.start();
            isRunning = true;
            logger.info("Game loop started");
        }
    }
}