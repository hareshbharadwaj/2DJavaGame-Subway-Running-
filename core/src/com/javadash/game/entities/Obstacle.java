package com.javadash.game.entities;

import com.badlogic.gdx.math.Rectangle;
import static com.javadash.game.GameConstants.*;

public class Obstacle {

    private final double x;
    private double y;
    private final int lane;
    private final double width;
    private final double height;
    private final int type;

    private boolean hit = false;
    private boolean passed = false;
    private double wheelSpin = 0.0;

    private final Rectangle hitbox = new Rectangle();

    public Obstacle(double startX, double startY, int lane, double w, double h, int type) {
        this.x = startX;
        this.y = startY;
        this.lane = lane;
        this.width = w;
        this.height = h;
        this.type = type;
    }

    public int getType() { return type; }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getLane() { return lane; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public double getWheelSpin() { return wheelSpin; }

    public boolean isHit() { return hit; }
    public void setHit(boolean hit) { this.hit = hit; }

    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }

    public void update(double deltaTime, double speed) {
        // Oncoming vehicles add their own closing speed on top of world scroll
        y += (speed + oncomingClosingSpeed(type)) * deltaTime;
        if (isOncoming(type)) {
            wheelSpin += deltaTime;
        }
    }

    public boolean isOffScreen() {
        return y > ROAD_Y + ROAD_H + 10;
    }

    public Rectangle getHitbox() {
        double padX = 3.0;
        double padY = 2.0;
        hitbox.set((float)(x + padX), (float)(y + padY),
                   (float)(width - padX * 2.0), (float)(height - padY * 2.0));
        return hitbox;
    }
}
