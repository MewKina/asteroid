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
import se233.Asteroids_Project.model.Asset.BossProjectile;
import se233.Asteroids_Project.model.Asset.MinionProjectile;
import se233.Asteroids_Project.model.Asset.PlayerProjectile;
import se233.Asteroids_Project.model.Effect.NukeExplosion;
import se233.Asteroids_Project.model.Effect.ExplosionEffect;
import se233.Asteroids_Project.model.Entities.Asteroids;
import se233.Asteroids_Project.model.Entities.Boss;
import se233.Asteroids_Project.model.Entities.Minion;
import se233.Asteroids_Project.model.Entities.Player;
import se233.Asteroids_Project.view.GameStage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class GameController {
    private static final Logger logger = LogManager.getLogger(GameController.class);

    private GameStage gameStage;
    private AnimationTimer gameLoop;
    private boolean isRunning;
    private Image lifeIcon;

    // Game objects
    private Player player;
    private List<Asteroids> asteroids;
    private List<Minion> enemies;
    private List<Boss> boss;
    private List<PlayerProjectile> playerProjectiles;
    private List<MinionProjectile> minionProjectiles;
    private List<BossProjectile> bossProjectiles;
    private List<ExplosionEffect> explosionEffects;
    private List<NukeExplosion> nukeExplosions;

    // Game state
    private GameState gameState = GameState.MAIN_MENU;
    private int level;
    // private int score;
    private double spawnTimer;
    private static final double SPAWN_INTERVAL = 3.0;
    private boolean bossSpawned = false;
    private boolean enemySpawned = false;
    private boolean scoreThresholdReached = false;
    private boolean canRotate = false;

    private static final int SPREAD_SHOT_COUNT = 5;
    private static final double SPREAD_ANGLE = 60.0;
    private int bossAttackPattern = 0;
    private static final int PATTERN_SWITCH_INTERVAL = 300; // frames (about 5 seconds at 60 FPS)
    private int patternTimer = 0;

    // Menu animation
    private double textAlpha = 1.0;
    private double textAlphaChange = -0.02;

    // Bomb ability display
    private static final Color NUKE_READY_COLOR = Color.LIGHTGREEN;
    private static final Color NUKE_COOLDOWN_COLOR = Color.RED;

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
        enemies = new ArrayList<>();
        boss = new ArrayList<>();
        explosionEffects = new ArrayList<>();
        nukeExplosions = new ArrayList<>();
        playerProjectiles = new ArrayList<>();
        minionProjectiles = new ArrayList<>();
        bossProjectiles = new ArrayList<>();

        //     score = 0;
        Scoring.resetScore(); // Reset the score at game start
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

        // Spawn initial objects for menu background
        spawnAsteroids(3);
        spawnEnemies(1);
        spawnBoss();
    }

    private void updateGame() {
        switch (gameState) {
            case MAIN_MENU:
                updateMenu();
                break;
            case PLAYGROUND:
                updatePlaying();
                break;
            case GAME_OVER:
                updateGameOver();
                break;
        }
    }
    public void rotatePlayerToCursor(double mouseX, double mouseY) {
        if (player != null && player.isAlive()) { // Ensure player is initialized and alive
            player.rotateToCursor(mouseX, mouseY); // Update player rotation
        }
    }
    public void handleMouseMoved(MouseEvent event) {
        if(canRotate){
            rotatePlayerToCursor(event.getX(), event.getY());
        }
    }

    private void updateMenu() {
        // Update background objects
        for (Asteroids asteroids : this.asteroids) {
            asteroids.update();
        }
        for (Minion minion : enemies) {
            minion.update();
        }

        // Update text fade effect
        textAlpha += textAlphaChange;
        if (textAlpha <= 0 || textAlpha >= 1) {
            textAlphaChange *= -1;
        }
    }

    private void updatePlaying() {
        if (!player.isAlive()) {
            gameState = GameState.GAME_OVER;
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

        // Update bomb explosions
        Iterator<NukeExplosion> bombexplosionIterator = nukeExplosions.iterator();
        while (bombexplosionIterator.hasNext()) {
            NukeExplosion nukeExplosion = bombexplosionIterator.next();
            nukeExplosion.update();
            if (nukeExplosion.isFinished()) {
                bombexplosionIterator.remove();
            }
        }

        // Update projectiles and check collisions
        updateProjectiles();

        // Update asteroids
        Iterator<Asteroids> asteroidIterator = asteroids.iterator();
        while (asteroidIterator.hasNext()) {
            Asteroids asteroids = asteroidIterator.next();
            asteroids.update();
            if (asteroids.isMarkedForDestruction()) {
                asteroidIterator.remove();
            }
        }

        // Update enemies
        Iterator<Minion> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Minion minion = enemyIterator.next();
            minion.update();

            // Handle enemy shooting
            if (minion.canShoot() && player.isAlive()) {
                double angleToPlayer = minion.getAngleToPlayer();

                // Calculate projectile spawn position
                double spawnDistance = 20;
                double angleRad = Math.toRadians(angleToPlayer);
                double projectileX = minion.getX() + Math.cos(angleRad) * spawnDistance;
                double projectileY = minion.getY() + Math.sin(angleRad) * spawnDistance;

                MinionProjectile enemyprojectile = new MinionProjectile(
                        projectileX, projectileY,
                        angleToPlayer,
                        gameStage.getStageWidth(),
                        gameStage.getStageHeight()
                );
                minionProjectiles.add(enemyprojectile);
                minion.resetShootCooldown();
            }

            if (minion.isMarkedForDestructionMinion()) {
                enemyIterator.remove();
            }
        }

        // Update Boss
        Iterator<Boss> bossIterator = boss.iterator();
        while (bossIterator.hasNext()) {
            Boss boss = bossIterator.next();
            boss.update();

            // Handle Boss shooting
            if (boss.canShoot() && player.isAlive()) {
                double angleToPlayer = boss.getAngleToPlayer();

                // Calculate projectile spawn position
                double spawnDistance = 200;
                double angleRad = Math.toRadians(angleToPlayer);
                double projectileX = boss.getX() + (boss.getWidth() / 2) + Math.cos(angleRad) * spawnDistance;
                double projectileY = boss.getY() + (boss.getHeight() / 2) + Math.sin(angleRad) * spawnDistance;

                // Update pattern timer and switch patterns
                patternTimer++;
                if (patternTimer >= PATTERN_SWITCH_INTERVAL) {
                    bossAttackPattern = (bossAttackPattern + 1) % 3; // Cycle through 3 patterns
                    patternTimer = 0;
                }

                // Different attack patterns
                switch (bossAttackPattern) {
                    case 0: // Multi-shot pattern
                        double centerX = boss.getX() + (boss.getWidth() / 2);
                        double centerY = boss.getY() + (boss.getHeight() / 2);
                        BossProjectile[] multiShots = BossProjectile.createMultiShotPattern(
                                centerX, centerY,
                                angleToPlayer,

                                gameStage.getStageWidth(),
                                gameStage.getStageHeight(),
                                5,  // Number of bullets
                                10.0 // Spacing between bullets
                        );
                        for (BossProjectile shot : multiShots) {
                            bossProjectiles.add(shot);
                        }
                        break;

//                    case 1: // Spiral pattern
//                        BossProjectile spiralShot = new BossProjectile(
//                                projectileX, projectileY,
//                                angleToPlayer,
//                                gameStage.getStageWidth(),
//                                gameStage.getStageHeight(),
//                                BossProjectile.ProjectilePattern.SPIRAL
//                        );
//                        bossProjectiles.add(spiralShot);
//                        break;
//
//                    case 2: // Spread pattern
//
//
//                        double center1X = boss.getX() + (boss.getWidth() / 2);
//                        double center1Y = boss.getY() + (boss.getHeight() / 2);
//                        BossProjectile[] spreadShots = BossProjectile.createSpreadPattern(
//                                center1X, center1Y,
//                                angleToPlayer,
//                                gameStage.getStageWidth(),
//                                gameStage.getStageHeight(),
//                                SPREAD_SHOT_COUNT,
//                                SPREAD_ANGLE
//                        );
//                        for (BossProjectile shot : spreadShots) {
//                            bossProjectiles.add(shot);
//                        }
//                        break;
                }
                boss.resetShootCooldown();
            }

            if (boss.isMarkedForDestructionBoss()) {
                bossIterator.remove();
            }
        }

        // Update enemy projectiles
        updateMinionProjectiles();
        updateBossProjectiles();

        // Handle all collisions
        Collisions.handleCollisions(player, asteroids, enemies, boss, playerProjectiles);

        if (Scoring.getCurrentScore() >= 10 && !scoreThresholdReached) {
            scoreThresholdReached = true;
            logger.info("Score threshold reached! Boss can now spawn");
        }

        // Update spawn timer
        spawnTimer -= 0.016;
        if (spawnTimer <= 0) {
            spawnAsteroids(1);
            if (!bossSpawned) {
                spawnEnemies(1);
            }

            if (scoreThresholdReached) {
                enemySpawned = true;
                // Only spawn boss if none exists
                spawnBoss();
            }

            if(Scoring.getCurrentScore() >= 20 ) {
                enemySpawned = false;
                spawnEnemies(1);
            }
            spawnTimer = SPAWN_INTERVAL;
        }

    }

    private void updateProjectiles() {
        Iterator<PlayerProjectile> projectileIterator = playerProjectiles.iterator();
        while (projectileIterator.hasNext()) {
            PlayerProjectile playerProjectile = projectileIterator.next();
            playerProjectile.update();

            if (playerProjectile.isExpired()) {
                projectileIterator.remove();
                continue;
            }

            // Check collisions with asteroids
            for (Asteroids asteroids : this.asteroids) {
                if (Collisions.checkCollision(playerProjectile, asteroids)) {
                    projectileIterator.remove();
                    asteroids.takeDamage(1);
                    explosionEffects.add(new ExplosionEffect(
                            playerProjectile.getX(),
                            playerProjectile.getY()
                    ));
                    if (asteroids.isMarkedForDestruction()) {
                        handleAsteroidDestruction(asteroids);
                    }

                    break;
                }
            }

            // Check collisions with enemies
            for (Minion minion : enemies) {
                if (Collisions.checkCollision(playerProjectile, minion)) {
                    projectileIterator.remove();
                    // Instead of immediately destroying the enemy, damage it
                    minion.takeDamage(1);
                    // Create small explosion effect for hit feedback
                    explosionEffects.add(new ExplosionEffect(
                            playerProjectile.getX(),
                            playerProjectile.getY()
                    ));
                    // Only award points and create big explosion if enemy is destroyed
                    if (minion.isMarkedForDestructionMinion()) {
                        handleMinionDestruction(minion);
                    }
                    break;
                }
            }

            // Check collisions with boss
            for (Boss boss1 : boss) {
                if (Collisions.checkCollision(playerProjectile, boss1) ) {
                    projectileIterator.remove();
                    boss1.takeDamage(1);
                    explosionEffects.add(new ExplosionEffect(
                            playerProjectile.getX(),
                            playerProjectile.getY()
                    ));
                    if (boss1.isMarkedForDestructionBoss()) {
                        handleBossDestruction(boss1);
                    }
                    break;
                }


            }
        }
    }

    private void updateMinionProjectiles() {
        Iterator<MinionProjectile> projectileIterator = minionProjectiles.iterator();
        while (projectileIterator.hasNext()) {
            MinionProjectile minionProjectile = projectileIterator.next();
            minionProjectile.update();

            if (minionProjectile.isExpired()) {
                projectileIterator.remove();
                continue;
            }

            // Check collision with player
            if (player.isAlive() && Collisions.checkCollision(minionProjectile, player)) {
                projectileIterator.remove();
                player.hit(); // Assuming Player class has a hit() method
                continue;
            }
        }
    }

    private void updateBossProjectiles() {
        Iterator<BossProjectile> projectileIterator = bossProjectiles.iterator();
        while (projectileIterator.hasNext()) {
            BossProjectile bossProjectile = projectileIterator.next();
            bossProjectile.update();

            if (bossProjectile.isExpired()) {
                projectileIterator.remove();
                continue;
            }

            // Check collision with player
            if (player.isAlive() && Collisions.checkCollision(bossProjectile, player)) {
                projectileIterator.remove();
                player.hit(); // Assuming Player class has a hit() method
                continue;
            }
        }
    }

    private void updateGameOver() {
        textAlpha += textAlphaChange;
        if (textAlpha <= 0 || textAlpha >= 1) {
            textAlphaChange *= -1;
        }
    }

    private void renderGame() {
        var gc = gameStage.getGraphicsContext();
        gc.clearRect(0, 0, gameStage.getStageWidth(), gameStage.getStageHeight());

        switch (gameState) {
            case MAIN_MENU:
                renderMenu(gc);
                break;
            case PLAYGROUND:
                renderPlaying(gc);
                break;
            case GAME_OVER:
                renderGameOver(gc);
                break;
        }
    }

    private void renderMenu(GraphicsContext gc) {
        // Render background objects
        // Draw title
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 40));
        double titleX = gameStage.getStageWidth() / 2;
        double titleY = gameStage.getStageHeight() / 3;
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("Asteroids Project", titleX, titleY);

        // Draw blinking "PUSH SPACE TO START" text
        gc.setGlobalAlpha(textAlpha);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        gc.fillText("Press ENTER To Start", titleX, gameStage.getStageHeight() / 2);
        gc.setGlobalAlpha(1.0);

        // Draw controls info
        gc.setFont(Font.font("Arial", 16));
        double infoY = gameStage.getStageHeight() * 0.7;
        gc.fillText("Controls:", titleX, infoY);
        gc.fillText("WASD - Move", titleX, infoY + 25);
        gc.fillText("LEFT/RIGHT - Rotate", titleX, infoY + 50);
        gc.fillText("SPACE - Shoot", titleX, infoY + 75);
        gc.fillText("E - Activate Nuke", titleX, infoY + 100);
        gc.fillText("ESC - Quit", titleX, infoY + 125);
    }

    private void renderPlaying(GraphicsContext gc) {
        // Render game objects
        for (Asteroids asteroids : this.asteroids) {
            asteroids.render(gc);
        }
        for (Minion minion : enemies) {
            minion.render(gc);
        }

        for (Boss boss1 : boss) {
            boss1.render(gc);
        }

        for (PlayerProjectile playerProjectile : playerProjectiles) {
            playerProjectile.render(gc);
        }

        for (MinionProjectile minionProjectile : minionProjectiles) {
            minionProjectile.render(gc);
        }

        for (BossProjectile bossProjectile : bossProjectiles) {
            bossProjectile.render(gc);
        }

        for (ExplosionEffect explosionEffect : explosionEffects) {
            explosionEffect.render(gc);
        }

        for (NukeExplosion nukeExplosion : nukeExplosions) {
            nukeExplosion.render(gc);
        }

        if (player.isAlive()) {
            player.render(gc);
        }

        // Draw HUD
        renderHUD(gc);
    }

    private void renderHUD(GraphicsContext gc) {
        // Draw score and lives
        // Draw scores
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 20));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText("Score: " + Scoring.getCurrentScore(), 10, 30);

        // Draw high score
        if (Scoring.checkHighestScore()) {
            gc.setFill(Color.GOLD); // Gold color for new high score
        }
        gc.fillText("Highest Score: " + Scoring.getHighestScore(), 10, 60);
        // Draw combo if active
        if (Scoring.getCombo() > 1) {
            gc.setFill(Color.YELLOW);
            gc.fillText("Combo x" + Scoring.getCombo(), 10, 90);
        }
        // Reset color for lives display
        gc.setFill(Color.WHITE);
        gc.fillText("Lives: ", 10, 120);

        // Draw life icons
        double iconSize = 30;
        double baseX = 70;
        double baseY = 105;
        double spacing = 25;
        for (int i = 0; i < player.getLives(); i++) {
            gc.drawImage(lifeIcon, baseX + (i * spacing), baseY, iconSize, iconSize);
        }

        // Draw bomb status
        double cooldown = player.getBombCooldown();
        String nukeText = cooldown > 0
                ? String.format("Nuke: %.1fs", cooldown)
                : "Nuke: READY";

        gc.setFill(cooldown > 0 ? NUKE_COOLDOWN_COLOR : NUKE_READY_COLOR);
        gc.setFont(Font.font("Arial", 16));
        gc.fillText(nukeText, 10, gameStage.getStageHeight() - 10);
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
        gc.fillText("Final Score: " + Scoring.getCurrentScore(), centerX, centerY + 10);
        gc.setGlobalAlpha(textAlpha);
        gc.fillText("Press ENTER to Play Again!", centerX, centerY + 50);
        gc.fillText("Press ESC toQuit", centerX, centerY + 75);
        gc.setGlobalAlpha(1.0);
    }

    private void handleAsteroidDestruction(Asteroids asteroids) {
        asteroids.markForDestruction();
        Scoring.addPoints(asteroids.getPoints());
        explosionEffects.add(new ExplosionEffect(
                asteroids.getX() + asteroids.getWidth()/2,
                asteroids.getY() + asteroids.getHeight()/2
        ));
        logger.info("Asteroid destroyed! Score: {}", Scoring.getCurrentScore());
    }

    private void handleMinionDestruction(Minion minion) {
        minion.markForDestructionMinion();
        Scoring.addPoints(minion.getPointsMinion());
        explosionEffects.add(new ExplosionEffect(
                minion.getX() + minion.getWidth()/2,
                minion.getY() + minion.getHeight()/2
        ));

        logger.info("Enemy destroyed! Score: {}", Scoring.getCurrentScore());
    }

    private void handleNukeAsteroidDestruction(Asteroids asteroids) {
        asteroids.markForDestruction();
        Scoring.addPoints(asteroids.getPoints());
        nukeExplosions.add(new NukeExplosion(
                asteroids.getX() + asteroids.getWidth()/2,
                asteroids.getY() + asteroids.getHeight()/2
        ));
        logger.info("Asteroid destroyed! Score: {}", Scoring.getCurrentScore());
    }

    private void handleNukeMinionDestruction(Minion minion) {
        minion.markForDestructionMinion();
        Scoring.addPoints(minion.getPointsMinion());
        nukeExplosions.add(new NukeExplosion(
                minion.getX() + minion.getWidth()/2,
                minion.getY() + minion.getHeight()/2
        ));

        logger.info("Minion destroyed! Score: {}", Scoring.getCurrentScore());
    }

    private void handleBossDestruction(Boss boss) {

        Scoring.addPoints(boss.getPointsBoss());
        explosionEffects.add(new ExplosionEffect(
                boss.getX() + boss.getWidth()/2,
                boss.getY() + boss.getHeight()/2
        ));
        boss.markForDestructionBoss();
        bossSpawned = true;
        enemySpawned = false;
        logger.info("Boss destroyed! Score: {}", Scoring.getCurrentScore());
    }

    private Optional<Asteroids> findNearestAsteroid() {
        double shortestDistance = Double.MAX_VALUE;
        Asteroids nearest = null;

        for (Asteroids asteroids : this.asteroids) {
            double dx = asteroids.getX() - player.getX();
            double dy = asteroids.getY() - player.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance < shortestDistance) {
                shortestDistance = distance;
                nearest = asteroids;
            }
        }

        return Optional.ofNullable(nearest);
    }

    private Optional<Minion> findNearestMinion() {
        double shortestDistance = Double.MAX_VALUE;
        Minion nearest = null;

        for (Minion minion : enemies) {
            double dx = minion.getX() - player.getX();
            double dy = minion.getY() - player.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance < shortestDistance) {
                shortestDistance = distance;
                nearest = minion;
            }
        }

        return Optional.ofNullable(nearest);
    }

    private void activateNuke() {
        if (player.canUseNuke()) {
            findNearestAsteroid().ifPresent(asteroids -> {
                handleNukeAsteroidDestruction(asteroids);
                player.useNuke();
                logger.info("Nuke used on nearest asteroid");
            });

            findNearestMinion().ifPresent(minion -> {

                handleNukeMinionDestruction(minion);
                player.useNuke();
                logger.info("Nuke used on nearest enemy");
            });
        }
    }

    public void handleKeyPress(KeyCode code) {
        switch (gameState) {
            case MAIN_MENU:
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
            case GAME_OVER:
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

    public void handleMouseClick(MouseEvent event) {
        fireProjectile();
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
            case LEFT:
                player.setRotatingLeft(true);
                break;
            case RIGHT:
                player.setRotatingRight(true);
                break;
//            case SPACE:
//                fireProjectile();
//                break;
            case E:
                activateNuke();
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
            case LEFT:
                player.setRotatingLeft(false);
                break;
            case RIGHT:
                player.setRotatingRight(false);
                break;
        }
    }
    private void fireProjectile() {
        if (player.canShoot()) {
            // Calculate projectile spawn position (slightly in front of the ship)
            double angleRad = Math.toRadians(player.getRotation());
            double spawnDistance = 20;
            double projectileX = player.getX() + Math.cos(angleRad) * spawnDistance;
            double projectileY = player.getY() + Math.sin(angleRad) * spawnDistance;

            PlayerProjectile playerProjectile = new PlayerProjectile(
                    projectileX, projectileY,
                    player.getRotation(),
                    gameStage.getStageWidth(),
                    gameStage.getStageHeight()
            );
            playerProjectiles.add(playerProjectile);
            player.resetShootCooldown();
            logger.debug("Projectile fired from ({}, {})", projectileX, projectileY);
        }
    }

    private void startNewGame() {
        // Reset game state
        Scoring.resetScore();
        level = 1;
        spawnTimer = SPAWN_INTERVAL;
        bossSpawned = false;
        scoreThresholdReached = false;
        enemySpawned = false;
        canRotate = true;


        // Clear existing objects
        asteroids.clear();
        playerProjectiles.clear();
        minionProjectiles.clear();
        bossProjectiles.clear();
        explosionEffects.clear();
        enemies.clear();
        boss.clear();

        // Reset player
        double centerX = gameStage.getStageWidth() / 2;
        double centerY = gameStage.getStageHeight() / 2;
        player = new Player(centerX, centerY, gameStage.getStageWidth(), gameStage.getStageHeight());

        // Spawn initial asteroids
        spawnAsteroids(2);
        spawnEnemies(1);

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
            asteroids.add(new Asteroids(x, y, asteroidSize));
        }
    }
    private void spawnEnemies(int count) {
        enemySpawned = false;
        for (int i = 0; i < count; i++) {
            double x, y;
            if (Math.random() < 0.5) {
                x = Math.random() < 0.5 ? -30 : gameStage.getStageWidth() + 30;
                y = Math.random() * gameStage.getStageHeight();
            } else {
                x = Math.random() * gameStage.getStageWidth();
                y = Math.random() < 0.5 ? -30 : gameStage.getStageHeight() + 30;
            }

            int EnemyType = generateRandomEnemy();
            enemies.add(new Minion(x, y, EnemyType ,player));
        }
    }

    private void spawnBoss() {

        if (scoreThresholdReached && !bossSpawned && boss.isEmpty()) { // Double check both flags
            //enemies.clear();
            enemySpawned = true;


            double x, y;
            if (Math.random() < 0.5) {
                x = Math.random() < 0.5 ? -30 : gameStage.getStageWidth() + 30;
                y = Math.random() * gameStage.getStageHeight();
            } else {
                x = Math.random() * gameStage.getStageWidth();
                y = Math.random() < 0.5 ? -30 : gameStage.getStageHeight() + 30;
            }

            int Boss = generateRandomBoss();
            boss.add(new Boss(x, y, Boss, player));
            bossSpawned = true;
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

    private int generateRandomEnemy() {
        return Math.random() < 0.6 ? 1 : 2;
    }

    private int generateRandomBoss() {
        return 1;
    }

    public void startGameLoop() {
        if (!isRunning) {
            gameLoop.start();
            isRunning = true;
            logger.info("Game loop started");
        }
    }
}