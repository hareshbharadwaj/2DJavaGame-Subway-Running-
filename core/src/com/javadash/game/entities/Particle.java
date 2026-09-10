package com.javadash.game.entities;

import com.badlogic.gdx.graphics.Color;

public class Particle {

    public double x;
    public double y;
    public double vx;
    public double vy;
    public Color color;
    public float size;
    public double life;
    public double maxLife;

    public Particle(double x, double y, double vx, double vy, Color color, float size, double duration) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.color = new Color(color);
        this.size = size;
        this.life = duration;
        this.maxLife = duration;
    }

    public void update(double deltaTime) {
        x += vx * deltaTime;
        y += vy * deltaTime;
        vy += 120.0 * deltaTime; // gravity
        life -= deltaTime;
    }

    public boolean isDead() {
        return life <= 0;
    }

    public float getAlpha() {
        return (float) Math.max(0.0, Math.min(1.0, life / maxLife));
    }
}
