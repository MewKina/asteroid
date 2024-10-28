package se233.Asteroids_Project.model.PlayerAsset;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se233.Asteroids_Project.model.GameObject;

public class Player extends GameObject {
    private static final Logger logger = LogManager.getLogger(Player.class);

    // Sprite paths
    private static final String IDLE = "/se233/Asteroids_Project/asset/player_ship.png";
    private static final String Move = "/se233/Asteroids_Project/asset/player_ani.png";
    private static final String Hit = "/se233/Asteroids_Project/asset/explosion.png";

    // Animation-related fields
    private Image HitImage;
    private int HitFrame = 0;
    private double HitAnimationTimer = 0.5;
    private static final double Hit_FRAME_DURATION = 0.1;
    private static final int Hit_FRAME_COUNT = 4;
    private Image idleImage;
    private PlayerState currentState = PlayerState.IDLE;

    // Movement properties
    private double maxSpeed = 5.0;
    private double acceleration = 0.2;
    private double deceleration = 0.98;
    private double rotationSpeed = 5.0;

    // Velocity components
    private double velocityX = 0;
    private double velocityY = 0;

    // Screen boundaries
    private final double screenWidth;
    private final double screenHeight;

    // Game state
    private int lives;
    private boolean isInvulnerable;
    private double invulnerabilityTimer;

    // Movement flags
    private boolean isMovingForward;
    private boolean isMovingBackward;
    private boolean isMovingLeft;
    private boolean isMovingRight;
//    private boolean isRotatingLeft;
//    private boolean isRotatingRight;

    // Shooting properties
    private double shootCooldown = 0.25; // 250ms between shots
    private double timeSinceLastShot = 0.25;

    private enum PlayerState {
        IDLE,
        MOVING
    }

    public Player(double x, double y, double screenWidth, double screenHeight) {
        super(Move, x, y, 30, 30);
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.lives = 3;
        this.isInvulnerable = false;
        this.rotation = -90; // Start facing upward

        // Load images
        try {
            this.idleImage = new Image(getClass().getResourceAsStream(IDLE));
            this.HitImage = new Image(getClass().getResourceAsStream(Hit));
        } catch (Exception e) {
            logger.error("Failed to load player images: " + e.getMessage());
        }

        initializeAnimation(48, 48, 4, 0.1);
        logger.info("Player created at position ({}, {})", x, y);
    }

    public void rotateToCursor(double mouseX, double mouseY){
        double deltaX = mouseX - this.getX();
        double deltaY = mouseY - this.getY();
        this.rotation = Math.toDegrees(Math.atan2(deltaY, deltaX));
    }

    @Override
    public void update() {
        // Update movement state
        if (isMovingForward) {
            currentState = PlayerState.MOVING;
            updateAnimation(0.016);
        } else {
            currentState = PlayerState.IDLE;
        }

        // Wrap around screen
        if (x < 0) x = screenWidth;
        if (x > screenWidth) x = 0;
        if (y < 0) y = screenHeight;
        if (y > screenHeight) y = 0;

        // Update movement
        double angleRad = Math.toRadians(rotation);
        if (isMovingForward) {
            velocityX += Math.cos(angleRad) * acceleration;
            velocityY += Math.sin(angleRad) * acceleration;
        }
        if (isMovingBackward) {
            velocityX -= Math.cos(angleRad) * acceleration;
            velocityY -= Math.sin(angleRad) * acceleration;
        }
        if (isMovingLeft) {
            velocityX += Math.cos(angleRad - Math.PI / 2) * acceleration;
            velocityY += Math.sin(angleRad - Math.PI / 2) * acceleration;
        }
        if (isMovingRight) {
            velocityX += Math.cos(angleRad + Math.PI / 2) * acceleration;
            velocityY += Math.sin(angleRad + Math.PI / 2) * acceleration;
        }

        // Limit speed
        double currentSpeed = Math.sqrt(velocityX * velocityX + velocityY * velocityY);
        if (currentSpeed > maxSpeed) {
            velocityX = (velocityX / currentSpeed) * maxSpeed;
            velocityY = (velocityY / currentSpeed) * maxSpeed;
        }

        // Apply velocity
        x += velocityX;
        y += velocityY;

        // Apply drag
        velocityX *= deceleration;
        velocityY *= deceleration;

        // Wrap around screen
        if (x < 0) x = screenWidth;
        if (x > screenWidth) x = 0;
        if (y < 0) y = screenHeight;
        if (y > screenHeight) y = 0;

        // Update invulnerability
        if (isInvulnerable) {
            HitAnimationTimer += 0.016;
            if (HitAnimationTimer >= Hit_FRAME_DURATION) {
                HitFrame = (HitFrame + 1) % Hit_FRAME_COUNT;
                HitAnimationTimer = 0;
            }
            invulnerabilityTimer -= 0.016;
            if (invulnerabilityTimer <= 0) {
                isInvulnerable = false;
                HitFrame = 0;
            }
        }

        // Update shooting cooldown
        if (timeSinceLastShot < shootCooldown) {
            timeSinceLastShot += 0.016;
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        gc.save();

        // Calculate drawing position
        double drawX = x - frameWidth / 2;
        double drawY = y - frameHeight / 2;

        // Apply rotation
        gc.translate(x, y);
        gc.rotate(rotation + 90);
        gc.translate(-x, -y);

        // Apply invulnerability effect
        if (isInvulnerable && Math.floor(invulnerabilityTimer * 10) % 2 == 0) {
            gc.setGlobalAlpha(0.5);
        }

        // Draw based on current state
        if (currentState == PlayerState.IDLE && idleImage != null) {
            gc.drawImage(idleImage, drawX, drawY, frameWidth, frameHeight);
        } else if (spriteSheet != null) {
            // Draw animation frame from sprite sheet
            double sourceX = currentFrame * frameWidth;
            gc.drawImage(
                    spriteSheet,
                    sourceX, 0, frameWidth, frameHeight,
                    drawX, drawY, frameWidth, frameHeight
            );
        }

        // Draw hit effect if invulnerable
        if (isInvulnerable && HitImage != null) {
            double effectSourceX = HitFrame * frameWidth;
            gc.setGlobalAlpha(0.7);
            gc.drawImage(
                    HitImage,
                    effectSourceX, 0, frameWidth, frameHeight,
                    drawX, drawY, frameWidth, frameHeight
            );
            gc.setGlobalAlpha(1.0);
        }

        gc.restore();
    }

    // Existing movement setters...
    public void setMovingForward(boolean moving) {
        this.isMovingForward = moving;
    }

    public void setMovingBackward(boolean moving) {
        this.isMovingBackward = moving;
    }

    public void setMovingLeft(boolean moving) {
        this.isMovingLeft = moving;
    }

    public void setMovingRight(boolean moving) {
        this.isMovingRight = moving;
    }

//    public void setRotatingLeft(boolean rotating) {
//        this.isRotatingLeft = rotating;
//    }
//
//    public void setRotatingRight(boolean rotating) {
//        this.isRotatingRight = rotating;
//    }

    // Shooting methods
    public boolean canShoot() {
        return timeSinceLastShot >= shootCooldown;
    }

    public void resetShootCooldown() {
        timeSinceLastShot = 0;
    }

    // Game state methods
    public void hit() {
        if (!isInvulnerable) {
            lives--;
            isInvulnerable = true;
            invulnerabilityTimer = 2.0;
            logger.info("Player hit! Lives remaining: {}", lives);
        }
    }

    public boolean isInvulnerable() {
        return isInvulnerable;
    }

    public int getLives() {
        return lives;
    }

    public boolean isAlive() {
        return lives > 0;
    }

    public double getShipAngle() {
        return rotation;
    }
}