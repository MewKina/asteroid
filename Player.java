package se233.Asteroids_Project.model.Entities;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se233.Asteroids_Project.model.AllObject;

public class Player extends AllObject {
    private static final Logger logger = LogManager.getLogger(Player.class);

    private static final String Idle = "/se233/Asteroids_Project/asset/player_ship.png";
    private static final String Move = "/se233/Asteroids_Project/asset/player_ani.png";
    private static final String Hit = "/se233/Asteroids_Project/asset/BlueExplosion.png";
    private static final String Shoot = "/se233/Asteroids_Project/asset/GreenE2.png";

    private Image hitImage;
    private Image shootEffectImage; // New image for shoot effect
    private int HitFrame = 0;
    private int shootEffectFrame = 0; // Track current frame of shoot effect
    private double HitAnimationTimer = 0;
    private double shootEffectTimer = 0; // Timer for shoot effect animation
    private static final double Hit_FRAME_DURATION = 0.1;
    private static final double SHOOT_EFFECT_FRAME_DURATION = 0.02; // 50ms per frame for faster animation
    private static final int Hit_FRAME_COUNT = 4;
    private static final int SHOOT_EFFECT_FRAME_COUNT = 6; // Number of frames in shoot effect animation
    private boolean isShowingShootEffect = false; // Flag to control shoot effect visibility
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
    private boolean isRotatingLeft;
    private boolean isRotatingRight;

    // Shooting properties
    private double shootCooldown = 0.25; // 250ms between shots
    private double timeSinceLastShot = 0.25;

    // Bomb ability properties
    private static final double BOMB_COOLDOWN = 15.0; // 15 seconds cooldown
    private double bombCooldownTimer = 0;
    private boolean canUseBomb = true;

    private enum PlayerState {
        IDLE,
        MOVING
    }


    public Player(double x, double y, double screenWidth, double screenHeight) {
        super(Move,x, y, 48, 48);
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.lives = 3;
        this.isInvulnerable = false;
        this.rotation = -90;  // Start facing upward

        try {
            this.idleImage = new Image(getClass().getResourceAsStream(Idle));
            this.hitImage = new Image(getClass().getResourceAsStream(Hit));
            this.shootEffectImage = new Image(getClass().getResourceAsStream(Shoot));
        } catch (Exception e) {
            logger.error("Failed to load idle image: " + e.getMessage());
        }

        initializeAnimation(48, 48, 4, 0.1);
        logger.info("Player created at position ({}, {})", x, y);
    }

    @Override
    public void update() {

        // Update shoot effect animation
        if (isShowingShootEffect) {
            shootEffectTimer += 0.016;
            if (shootEffectTimer >= SHOOT_EFFECT_FRAME_DURATION) {
                shootEffectFrame = (shootEffectFrame + 1) % SHOOT_EFFECT_FRAME_COUNT;
                shootEffectTimer = 0;

                // End animation after one complete cycle
                if (shootEffectFrame == 0) {
                    isShowingShootEffect = false;
                }
            }
        }

        // Update bomb cooldown
        if (!canUseBomb) {
            bombCooldownTimer -= 0.016; // Assuming 60 FPS
            if (bombCooldownTimer <= 0) {
                canUseBomb = true;
                logger.debug("Bomb ability ready");
            }
        }

        if (isMovingForward ) {
            currentState = PlayerState.MOVING;
            updateAnimation(0.016);
        } else {
            currentState = PlayerState.IDLE;
        }

        // Update rotation
        if (isRotatingLeft) {
            rotation -= rotationSpeed;
        }
        if (isRotatingRight) {
            rotation += rotationSpeed;
        }

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
        // Left movement (perpendicular to forward, 90 degrees counterclockwise)
        if (isMovingLeft) {
            velocityX += Math.cos(angleRad - Math.PI / 2) * acceleration;
            velocityY += Math.sin(angleRad - Math.PI / 2) * acceleration;
        }

        // Right movement (perpendicular to forward, 90 degrees clockwise)
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
        if (isInvulnerable && hitImage != null) {
            double effectSourceX = HitFrame * frameWidth;
            gc.setGlobalAlpha(0.7); // Make the effect slightly transparent
//            gc.drawImage(
//                    hitImage,
//                    effectSourceX, 0,
//                    frameWidth, frameHeight,
//                    drawX, drawY,
//                    frameWidth, frameHeight
//            );
        }

        // Draw shoot effect
        if (isShowingShootEffect && shootEffectImage != null) {
            double effectSourceX = shootEffectFrame * frameWidth;
            gc.setGlobalAlpha(0.8);


            gc.drawImage(
                    shootEffectImage,
                    effectSourceX, 0,
                    frameWidth, frameHeight,
                    drawX + 7.5, drawY - frameHeight * 0.3, // Offset upward in the rotated space
                    frameWidth * 0.5, frameHeight * 0.5 // Maintain the smaller size
            );
        }

        gc.setGlobalAlpha(1.0);
        gc.restore();
    }

    // Bomb ability methods
    public boolean canUseNuke() {
        return canUseBomb;
    }

    public void useNuke() {
        if (canUseBomb) {
            canUseBomb = false;
            bombCooldownTimer = BOMB_COOLDOWN;
            logger.info("Bomb ability used");
        }
    }

    public double getBombCooldown() {
        return Math.max(0, bombCooldownTimer);
    }

    // Existing movement setters...
    public void setMovingForward(boolean moving) {
        this.isMovingForward = moving;
        logger.debug("Moving forward");
    }

    public void setMovingBackward(boolean moving) {
        this.isMovingBackward = moving;
        logger.debug("Moving backward");
    }

    public void setMovingLeft(boolean moving) {
        this.isMovingLeft = moving;
        logger.debug("Moving left");
    }

    public void setMovingRight(boolean moving) {
        this.isMovingRight = moving;
        logger.debug("Moving right");
    }

    public void setRotatingLeft(boolean rotating) {
        this.isRotatingLeft = rotating;
        logger.debug("Rotating left");
    }

    public void setRotatingRight(boolean rotating) {
        this.isRotatingRight = rotating;
        logger.debug("Rotating right");
    }

    // Shooting methods
    public boolean canShoot() {
        return timeSinceLastShot >= shootCooldown;
    }

    public void resetShootCooldown() {
        timeSinceLastShot = 0;
        // Trigger shoot effect
        isShowingShootEffect = true;
        shootEffectFrame = 0;
        shootEffectTimer = 0;
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

    public void rotateToCursor(double mouseX, double mouseY){
        double deltaX = mouseX - this.getX();
        double deltaY = mouseY - this.getY();
        this.rotation = Math.toDegrees(Math.atan2(deltaY, deltaX));
        logger.info("Rotating to cursor - x: {}, y: {}, rotation: {}", mouseX, mouseY, rotation);
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