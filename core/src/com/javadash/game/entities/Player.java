package com.javadash.game.entities;

import com.badlogic.gdx.math.Rectangle;
import com.javadash.game.GameConstants;

import static com.javadash.game.GameConstants.*;

public class Player {

    private int lane = 1;
    private double x = 0;
    private double y = GROUND_Y;
    private double velocityY = 0.0;
    private boolean onGround = true;
    private double slideTimer = 0.0;
    private boolean flying = false;

    private final Rectangle hitbox = new Rectangle();

    public Player() {
        reset();
    }

    public void slide() {
        if (onGround && !flying) {
            slideTimer = SLIDE_DURATION;
        }
    }

    public void setFlying(boolean flying) {
        this.flying = flying;
        if (flying) {
            slideTimer = 0.0;
        }
    }

    public boolean isFlying() {
        return flying;
    }

    public boolean isSliding() {
        return slideTimer > 0.0;
    }

    public void moveLeft() {
        lane = Math.max(0, lane - 1);
    }

    public void moveRight() {
        lane = Math.min(2, lane + 1);
    }

    public void jump() {
        if (onGround) {
            onGround = false;
            slideTimer = 0.0; // jumping cancels a slide
            velocityY = -JUMP_STRENGTH;
        }
    }

    public void update(double deltaTime) {
        if (slideTimer > 0.0) {
            slideTimer = Math.max(0.0, slideTimer - deltaTime);
        }

        // Initialize if x is 0
        if (x == 0 && LANE_CENTER_X[lane] != 0) {
            x = LANE_CENTER_X[lane] - (PLAYER_WIDTH / 2.0);
        }

        // Smooth lane movement interpolation (frame-rate independent)
        double lerpSpeed = 16.0;
        double targetXPos = LANE_CENTER_X[lane] - (PLAYER_WIDTH / 2.0);
        x += (targetXPos - x) * Math.min(1.0, lerpSpeed * deltaTime);
        if (Math.abs(targetXPos - x) < 0.2) {
            x = targetXPos;
        }

        if (flying) {
            // Jetpack: ease up to a fixed hover height and stay there
            double hoverY = GROUND_Y - JETPACK_HOVER_HEIGHT;
            y += (hoverY - y) * Math.min(1.0, 6.0 * deltaTime);
            velocityY = 0.0;
            onGround = false;
            return;
        }

        // Vertical jump physics and gravity
        if (!onGround) {
            velocityY += GRAVITY * deltaTime;
            y += velocityY * deltaTime;

            if (y >= GROUND_Y) {
                y = GROUND_Y;
                velocityY = 0.0;
                onGround = true;
            }
        }
    }

    public void reset() {
        lane = 1;
        x = LANE_CENTER_X[lane] - (PLAYER_WIDTH / 2.0);
        y = GROUND_Y;
        velocityY = 0.0;
        onGround = true;
        slideTimer = 0.0;
        flying = false;
    }

    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getVelocityY() { return velocityY; }
    public int getLane() { return lane; }
    public boolean isJumping() { return !onGround; }
    public boolean isOnGround() { return onGround; }

    public Rectangle getHitbox() {
        // Inset body hitbox to avoid unfair pixel collisions
        double padX = 8.0;
        double padY = 6.0;
        double jumpLeniency = onGround ? 0.0 : 24.0;
        double height = PLAYER_HEIGHT - padY * 2.0 - jumpLeniency;

        if (isSliding()) {
            // Crouched: shorter box anchored to the feet so tall hazards pass overhead
            double slideHeight = height * SLIDE_HEIGHT_RATIO;
            hitbox.set((float)(x + padX), (float)(y + padY + (height - slideHeight)),
                       (float)(PLAYER_WIDTH - padX * 2.0), (float)slideHeight);
            return hitbox;
        }

        hitbox.set((float)(x + padX), (float)(y + padY),
                   (float)(PLAYER_WIDTH - padX * 2.0), (float)height);
        return hitbox;
    }
}
