package com.javadash.game.systems;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.javadash.game.audio.AudioManager;
import com.javadash.game.entities.*;
import com.javadash.game.storage.SaveManager;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static com.javadash.game.GameConstants.*;

public class CollisionManager {

    public interface CollisionCallback {
        void onShieldBlock();
        void onLoseLife();
        void onGameOver(boolean isNewBest);
        void onCoinCollected(int value);
        void onPowerUpCollected(PowerUp.Type type);
        void onLifeCollected();
    }

    private final Random random = new Random();

    public void checkCollisions(Player player,
                                List<Obstacle> obstacles,
                                List<Coin> coins,
                                List<PowerUp> powerUps,
                                List<LifePowerUp> lifePowerUps,
                                List<FloatingText> floatingTexts,
                                List<Particle> particles,
                                double invincibilityTimer,
                                double shieldTimer,
                                double jetpackTimer,
                                int doubleScoreMultiplier,
                                int lives,
                                long score,
                                int coinsCollected,
                                int distance,
                                long runStartTime,
                                CollisionCallback callback) {

        Rectangle playerHitbox = player.getHitbox();

        // 1. Obstacle collision
        if (invincibilityTimer <= 0.0) {
            Iterator<Obstacle> iterator = obstacles.iterator();
            while (iterator.hasNext()) {
                Obstacle obstacle = iterator.next();
                if (!obstacle.isHit() && playerHitbox.overlaps(obstacle.getHitbox())) {
                    int type = obstacle.getType();
                    boolean isJumpable = (type == OBS_BARREL || type == OBS_BARRIER || type == OBS_CONE || type == OBS_POTHOLE)
                            || isOncoming(type);

                    // Jetpack flies clean over all hazards
                    if (jetpackTimer > 0.0) {
                        continue;
                    }

                    // Hurdle over low hazards and oncoming traffic
                    if (isJumpable) {
                        double heightAboveGround = GROUND_Y - player.getY();
                        if (heightAboveGround > 30.0) {
                            continue; // successfully cleared
                        }
                    }

                    // Slide under barriers (type 1)
                    if (type == OBS_BARRIER && player.isSliding()) {
                        continue; // successfully ducked
                    }

                    obstacle.setHit(true);
                    iterator.remove();

                    AudioManager.play("crash");

                    if (shieldTimer > 0.0) {
                        floatingTexts.add(new FloatingText(player.getX(), player.getY() - 12, "SHIELD BLOCK!", new Color(0.44f, 0.77f, 1.0f, 1f), 1.0));
                        for (int i = 0; i < 15; i++) {
                            particles.add(new Particle(player.getX() + 20, player.getY() + 20, (random.nextDouble() - 0.5) * 150, (random.nextDouble() - 0.5) * 150, new Color(0.44f, 0.77f, 1.0f, 1f), 8, 0.6));
                        }
                        callback.onShieldBlock();
                    } else if (lives > 0) {
                        floatingTexts.add(new FloatingText(player.getX() + 4, player.getY() - 12, "-1 LIFE", new Color(1f, 0.35f, 0.35f, 1f), 0.9));
                        callback.onLoseLife();
                    } else {
                        AudioManager.stopMusic();
                        AudioManager.stopJetpackLoop();
                        AudioManager.play("gameover");

                        floatingTexts.add(new FloatingText(player.getX() - 4, player.getY() - 12, "CRASH!", new Color(1f, 0.24f, 0.24f, 1f), 1.2));
                        for (int i = 0; i < 15; i++) {
                            particles.add(new Particle(player.getX() + 20, player.getY() + 20, (random.nextDouble() - 0.5) * 200, (random.nextDouble() - 0.5) * 200, new Color(1f, 0.31f, 0.31f, 1f), 8, 0.6));
                        }

                        int finalScore = (int) score;
                        int duration = (int) (System.currentTimeMillis() - runStartTime);
                        boolean isBest = SaveManager.recordRun(finalScore, coinsCollected, distance, duration);
                        callback.onGameOver(isBest);
                    }
                    break;
                }
            }
        }

        // 2. Coin collection
        Iterator<Coin> coinIterator = coins.iterator();
        while (coinIterator.hasNext()) {
            Coin coin = coinIterator.next();
            if (playerHitbox.overlaps(coin.getHitbox())) {
                AudioManager.play("coin");
                int coinValue = 120 * doubleScoreMultiplier;
                floatingTexts.add(new FloatingText(coin.getX() - 2, coin.getY() - 8, "+" + coinValue, new Color(1f, 0.92f, 0.35f, 1f), 0.7));
                for (int i = 0; i < 6; i++) {
                    particles.add(new Particle(coin.getX() + 8, coin.getY() + 8, (random.nextDouble() - 0.5) * 100, (random.nextDouble() - 0.5) * 100, new Color(1f, 0.92f, 0.35f, 1f), 6, 0.5));
                }
                coinIterator.remove();
                callback.onCoinCollected(coinValue);
            }
        }

        // 3. Power-Up collection
        Iterator<PowerUp> puIterator = powerUps.iterator();
        while (puIterator.hasNext()) {
            PowerUp pu = puIterator.next();
            if (playerHitbox.overlaps(pu.getHitbox())) {
                AudioManager.play("powerup");
                PowerUp.Type type = pu.getType();

                String banner = "";
                Color color = Color.WHITE;
                switch (type) {
                    case SHIELD:
                        banner = "SHIELD!";
                        color = new Color(0.44f, 0.77f, 1.0f, 1f);
                        break;
                    case MAGNET:
                        banner = "MAGNET!";
                        color = new Color(1.0f, 0.59f, 0.20f, 1f);
                        break;
                    case BOOST:
                        banner = "BOOST!";
                        color = new Color(1.0f, 0.75f, 0.24f, 1f);
                        break;
                    case DOUBLE_SCORE:
                        banner = "x2 SCORE!";
                        color = new Color(0.75f, 0.55f, 1.0f, 1f);
                        break;
                    case JETPACK:
                        banner = "JETPACK!";
                        color = new Color(0.47f, 0.78f, 1.0f, 1f);
                        AudioManager.startJetpackLoop();
                        break;
                    case SLOW_MO:
                        banner = "SLOW-MO!";
                        color = new Color(0.35f, 0.90f, 0.86f, 1f);
                        break;
                }

                floatingTexts.add(new FloatingText(pu.getX() - 10, pu.getY() - 8, banner, color, 0.8));
                for (int i = 0; i < 8; i++) {
                    particles.add(new Particle(pu.getX() + 10, pu.getY() + 10, (random.nextDouble() - 0.5) * 120, (random.nextDouble() - 0.5) * 120, Color.WHITE, 6, 0.5));
                }
                puIterator.remove();
                callback.onPowerUpCollected(type);
            }
        }

        // 4. Life power-up collection
        Iterator<LifePowerUp> lifeIterator = lifePowerUps.iterator();
        while (lifeIterator.hasNext()) {
            LifePowerUp lpu = lifeIterator.next();
            if (playerHitbox.overlaps(lpu.getHitbox())) {
                if (lives < MAX_LIVES) {
                    AudioManager.play("powerup");
                    floatingTexts.add(new FloatingText(lpu.getX() - 10, lpu.getY() - 8, "+1 LIFE", new Color(1.0f, 0.40f, 0.55f, 1f), 0.8));
                    for (int i = 0; i < 8; i++) {
                        particles.add(new Particle(lpu.getX() + 10, lpu.getY() + 10, (random.nextDouble() - 0.5) * 120, (random.nextDouble() - 0.5) * 120, new Color(1.0f, 0.40f, 0.55f, 1f), 6, 0.5));
                    }
                    lifeIterator.remove();
                    callback.onLifeCollected();
                }
            }
        }
    }
}
