package com.javadash.game.entities;

import com.badlogic.gdx.graphics.Color;

public class FloatingText {

    public double x;
    public double y;
    public String text;
    public Color color;
    public double life;
    public double maxLife;

    public FloatingText(double x, double y, String text, Color color, double duration) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.color = new Color(color);
        this.life = duration;
        this.maxLife = duration;
    }

    public void update(double deltaTime) {
        y -= 45.0 * deltaTime; // Drifts upward
        life -= deltaTime;
    }

    public boolean isDead() {
        return life <= 0;
    }

    public float getAlpha() {
        return (float) Math.max(0.0, Math.min(1.0, life / maxLife));
    }
}
