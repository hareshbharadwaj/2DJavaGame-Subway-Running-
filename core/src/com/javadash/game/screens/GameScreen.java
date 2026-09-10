package com.javadash.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.javadash.game.Assets;
import com.javadash.game.GameConstants;
import com.javadash.game.JavaDashGame;
import com.javadash.game.audio.AudioManager;
import com.javadash.game.entities.*;
import com.javadash.game.input.GameInputHandler;
import com.javadash.game.storage.SaveManager;
import com.javadash.game.systems.CollisionManager;
import com.javadash.game.systems.SpawnManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static com.javadash.game.GameConstants.*;

public class GameScreen implements Screen, GameInputHandler.GameActionListener {

    public enum GameState {
        PLAYING, PAUSED, GAME_OVER
    }

    private final JavaDashGame game;
    private final GlyphLayout layout = new GlyphLayout();
    private final Random random = new Random();

    private final Player player = new Player();
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<Coin> coins = new ArrayList<>();
    private final List<LifePowerUp> lifePowerUps = new ArrayList<>();
    private final List<PowerUp> powerUps = new ArrayList<>();
    private final List<FloatingText> floatingTexts = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();

    private final SpawnManager spawnManager = new SpawnManager();
    private final CollisionManager collisionManager = new CollisionManager();
    private GameInputHandler inputHandler;

    private GameState state = GameState.PLAYING;

    private double obstacleSpeed = INITIAL_SPEED;
    private double spawnTimer = 0.0;
    private double spawnInterval = 0.85;
    private double invincibilityTimer = 0.0;
    private double screenShakeTimer = 0.0;
    private double shakeIntensity = 0.0;
    private double roadScroll = 0.0;
    private double runCycleTime = 0.0;
    private double scoreAccumulator = 0.0;
    private double distanceAccumulator = 0.0;

    // Power-up timers (all 10 seconds)
    private double shieldTimer = 0.0;
    private double magnetTimer = 0.0;
    private double boostTimer = 0.0;
    private double doubleScoreTimer = 0.0;
    private double jetpackTimer = 0.0;
    private double slowMoTimer = 0.0;

    private boolean newHighScore = false;
    private long celebrationStart = 0L;

    private long score = 0;
    private int coinsCollected = 0;
    private int distance = 0;
    private int lives = STARTING_LIVES;
    private long runStartTime = 0L;
    private String gameOverMessage = "";

    // Environment theme
    private String currentEnv = "city";

    public GameScreen(JavaDashGame game) {
        this.game = game;
    }

    public void resetGame() {
        state = GameState.PLAYING;
        player.reset();
        obstacles.clear();
        coins.clear();
        lifePowerUps.clear();
        powerUps.clear();
        floatingTexts.clear();
        particles.clear();
        spawnManager.reset();

        obstacleSpeed = INITIAL_SPEED;
        spawnTimer = 0.0;
        spawnInterval = 0.85;
        invincibilityTimer = 0.0;
        screenShakeTimer = 0.0;
        shakeIntensity = 0.0;
        roadScroll = 0.0;
        runCycleTime = 0.0;
        scoreAccumulator = 0.0;
        distanceAccumulator = 0.0;

        shieldTimer = 0.0;
        magnetTimer = 0.0;
        boostTimer = 0.0;
        doubleScoreTimer = 0.0;
        jetpackTimer = 0.0;
        slowMoTimer = 0.0;

        newHighScore = false;
        score = 0;
        coinsCollected = 0;
        distance = 0;
        lives = STARTING_LIVES;
        runStartTime = System.currentTimeMillis();
        gameOverMessage = "";

        AudioManager.startMusic();
    }

    @Override
    public void show() {
        if (inputHandler == null) {
            inputHandler = new GameInputHandler(game.viewport, this);
        }
        Gdx.input.setInputProcessor(inputHandler);
        AudioManager.startMusic();
    }

    @Override
    public void render(float delta) {
        // Delta time clamp to prevent jumps
        delta = Math.min(delta, 0.05f);

        if (state == GameState.PLAYING) {
            updateGame(delta);
        } else if (state == GameState.GAME_OVER) {
            updateParticlesAndTexts(delta);
        }

        // Screen Shake calculation
        float shakeOffsetX = 0f;
        float shakeOffsetY = 0f;
        if (screenShakeTimer > 0) {
            screenShakeTimer -= delta;
            shakeOffsetX = (random.nextFloat() - 0.5f) * 2f * (float) shakeIntensity;
            shakeOffsetY = (random.nextFloat() - 0.5f) * 2f * (float) shakeIntensity;
        }

        Gdx.gl.glClearColor(0.05f, 0.07f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.viewport.apply();
        game.camera.position.set(VIRTUAL_WIDTH / 2f + shakeOffsetX, VIRTUAL_HEIGHT / 2f + shakeOffsetY, 0);
        game.camera.update();

        game.batch.setProjectionMatrix(game.camera.combined);
        game.batch.begin();

        drawWorld();
        drawEntities();
        drawHUD();

        if (state == GameState.PAUSED) {
            drawPauseOverlay();
        } else if (state == GameState.GAME_OVER) {
            drawGameOverOverlay();
        }

        game.batch.end();

        // Reset camera position after shake
        if (shakeOffsetX != 0 || shakeOffsetY != 0) {
            game.camera.position.set(VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT / 2f, 0);
            game.camera.update();
        }
    }

    private void updateGame(float delta) {
        // Power-up count downs
        if (shieldTimer > 0) shieldTimer = Math.max(0, shieldTimer - delta);
        if (magnetTimer > 0) magnetTimer = Math.max(0, magnetTimer - delta);
        if (boostTimer > 0) boostTimer = Math.max(0, boostTimer - delta);
        if (doubleScoreTimer > 0) doubleScoreTimer = Math.max(0, doubleScoreTimer - delta);
        if (slowMoTimer > 0) slowMoTimer = Math.max(0, slowMoTimer - delta);

        if (jetpackTimer > 0) {
            jetpackTimer = Math.max(0, jetpackTimer - delta);
            player.setFlying(true);
            if (jetpackTimer <= 0) {
                player.setFlying(false);
                AudioManager.stopJetpackLoop();
            }
        } else {
            player.setFlying(false);
        }

        if (invincibilityTimer > 0) {
            invincibilityTimer = Math.max(0, invincibilityTimer - delta);
        }

        // Effective world scroll speed
        // Actual cap is 650.0 (per mentor verification)
        double baseSpeed = Math.min(SPEED_CAP, INITIAL_SPEED + distance * 0.4);
        if (boostTimer > 0) baseSpeed *= 1.5;
        if (jetpackTimer > 0) baseSpeed *= 1.8;
        if (slowMoTimer > 0) baseSpeed *= 0.5;
        obstacleSpeed = baseSpeed;

        // Road scroll & player run animation
        roadScroll += obstacleSpeed * delta;
        runCycleTime += delta * (obstacleSpeed / 60.0);

        // Distance & score accumulators
        distanceAccumulator += obstacleSpeed * delta * 0.05;
        if (distanceAccumulator >= 1.0) {
            int distStep = (int) distanceAccumulator;
            distance += distStep;
            distanceAccumulator -= distStep;

            // Environment transition every 350m
            int themeIdx = (distance / 350) % 4;
            String[] envs = {"city", "forest", "desert", "night"};
            currentEnv = envs[themeIdx];
        }

        int scoreMult = doubleScoreTimer > 0 ? 2 : 1;
        scoreAccumulator += obstacleSpeed * delta * 0.15 * scoreMult;
        if (scoreAccumulator >= 1.0) {
            int scoreStep = (int) scoreAccumulator;
            score += scoreStep;
            scoreAccumulator -= scoreStep;
        }

        // Player physics
        player.update(delta);

        // Spawning logic
        spawnInterval = Math.max(0.48, 0.85 - (distance * 0.00035));
        spawnTimer += delta;
        if (spawnTimer >= spawnInterval) {
            spawnTimer = 0.0;
            spawnManager.maybeRotateSafeLane(obstacles);
            spawnManager.spawnWorldObjects(
                obstacles, coins, lifePowerUps, powerUps,
                distance, lives, Assets.obstacleRegions
            );
        }

        // Update obstacles
        Iterator<Obstacle> obsIt = obstacles.iterator();
        while (obsIt.hasNext()) {
            Obstacle o = obsIt.next();
            o.update(delta, obstacleSpeed);

            if (!o.isPassed() && o.getY() > player.getY() + PLAYER_HEIGHT) {
                o.setPassed(true);
                score += 40 * scoreMult;
                floatingTexts.add(new FloatingText(o.getX() + 8, player.getY() - 14, "+40", new Color(0.44f, 0.77f, 1.0f, 1f), 0.6));
            }

            if (o.isOffScreen()) {
                obsIt.remove();
            }
        }

        // Update coins (with magnet attraction)
        Iterator<Coin> coinIt = coins.iterator();
        while (coinIt.hasNext()) {
            Coin c = coinIt.next();
            if (magnetTimer > 0 && c.getY() > 0 && c.getY() < VIRTUAL_HEIGHT) {
                double pCenterX = player.getX() + PLAYER_WIDTH / 2.0;
                double cCenterX = c.getX() + c.getSize() / 2.0;
                double dirX = pCenterX - cCenterX;
                double dirY = (player.getY() + PLAYER_HEIGHT / 2.0) - (c.getY() + c.getSize() / 2.0);
                double dist = Math.sqrt(dirX * dirX + dirY * dirY);
                if (dist > 5.0 && dist < 300.0) {
                    c.setX(c.getX() + (dirX / dist) * 400.0 * delta);
                    c.setY(c.getY() + (dirY / dist) * 400.0 * delta);
                }
            }
            c.update(delta, magnetTimer > 0 ? obstacleSpeed * 0.5 : obstacleSpeed);
            if (c.isOffScreen()) {
                coinIt.remove();
            }
        }

        // Update power-ups
        Iterator<PowerUp> puIt = powerUps.iterator();
        while (puIt.hasNext()) {
            PowerUp pu = puIt.next();
            pu.update(delta, obstacleSpeed);
            if (pu.isOffScreen()) puIt.remove();
        }

        // Update life power-ups
        Iterator<LifePowerUp> lifeIt = lifePowerUps.iterator();
        while (lifeIt.hasNext()) {
            LifePowerUp lpu = lifeIt.next();
            lpu.update(delta, obstacleSpeed);
            if (lpu.isOffScreen()) lifeIt.remove();
        }

        // Update particles and floating texts
        updateParticlesAndTexts(delta);

        // Check collisions
        collisionManager.checkCollisions(
            player, obstacles, coins, powerUps, lifePowerUps, floatingTexts, particles,
            invincibilityTimer, shieldTimer, jetpackTimer, scoreMult,
            lives, score, coinsCollected, distance, runStartTime,
            new CollisionManager.CollisionCallback() {
                @Override
                public void onShieldBlock() {
                    shieldTimer = 0.0;
                }

                @Override
                public void onLoseLife() {
                    lives--;
                    invincibilityTimer = 1.2;
                    screenShakeTimer = 0.28;
                    shakeIntensity = 5.0;
                }

                @Override
                public void onGameOver(boolean isNewBest) {
                    state = GameState.GAME_OVER;
                    screenShakeTimer = 0.4;
                    shakeIntensity = 8.0;
                    if (isNewBest && score > 0) {
                        gameOverMessage = "New personal best!";
                        triggerCelebration();
                    } else {
                        gameOverMessage = "Best: " + SaveManager.getHighScore();
                    }
                }

                @Override
                public void onCoinCollected(int value) {
                    coinsCollected++;
                    score += value;
                }

                @Override
                public void onPowerUpCollected(PowerUp.Type type) {
                    switch (type) {
                        case SHIELD: shieldTimer = ABILITY_DURATION; break;
                        case MAGNET: magnetTimer = ABILITY_DURATION; break;
                        case BOOST: boostTimer = ABILITY_DURATION; break;
                        case DOUBLE_SCORE: doubleScoreTimer = ABILITY_DURATION; break;
                        case JETPACK: jetpackTimer = ABILITY_DURATION; break;
                        case SLOW_MO: slowMoTimer = ABILITY_DURATION; break;
                    }
                    score += 50;
                }

                @Override
                public void onLifeCollected() {
                    if (lives < MAX_LIVES) lives++;
                }
            }
        );
    }

    private void updateParticlesAndTexts(float delta) {
        Iterator<Particle> pIt = particles.iterator();
        while (pIt.hasNext()) {
            Particle p = pIt.next();
            p.update(delta);
            if (p.isDead()) pIt.remove();
        }

        Iterator<FloatingText> tIt = floatingTexts.iterator();
        while (tIt.hasNext()) {
            FloatingText t = tIt.next();
            t.update(delta);
            if (t.isDead()) tIt.remove();
        }
    }

    private void triggerCelebration() {
        newHighScore = true;
        celebrationStart = System.currentTimeMillis();
        AudioManager.play("powerup");
        for (int i = 0; i < 150; i++) {
            float px = 20 + random.nextFloat() * (VIRTUAL_WIDTH - 40);
            float py = VIRTUAL_HEIGHT * (0.5f + random.nextFloat() * 0.5f);
            particles.add(new Particle(
                px, py,
                (random.nextDouble() - 0.5) * 300.0,
                -250.0 - random.nextDouble() * 200.0,
                new Color(random.nextFloat(), random.nextFloat(), random.nextFloat(), 1f),
                7, 1.8
            ));
        }
    }

    private void drawWorld() {
        // Draw Left & Right Side Backgrounds
        Texture sideL = Assets.sideLeft.get(currentEnv);
        Texture sideR = Assets.sideRight.get(currentEnv);
        if (sideL != null) game.batch.draw(sideL, 0, 0, SIDE_W, VIRTUAL_HEIGHT);
        if (sideR != null) game.batch.draw(sideR, ROAD_RIGHT, 0, SIDE_W, VIRTUAL_HEIGHT);

        // Draw Scrolling Road
        if (Assets.roadImage != null) {
            float roadOffset = (float) (roadScroll % VIRTUAL_HEIGHT);
            game.batch.draw(Assets.roadImage, ROAD_LEFT, roadOffset - VIRTUAL_HEIGHT, ROAD_RIGHT - ROAD_LEFT, VIRTUAL_HEIGHT);
            game.batch.draw(Assets.roadImage, ROAD_LEFT, roadOffset, ROAD_RIGHT - ROAD_LEFT, VIRTUAL_HEIGHT);
        }

        // Draw Barricades
        if (Assets.barricadeLeft != null) {
            game.batch.draw(Assets.barricadeLeft, ROAD_LEFT - 16, 0, 16, VIRTUAL_HEIGHT);
        }
        if (Assets.barricadeRight != null) {
            game.batch.draw(Assets.barricadeRight, ROAD_RIGHT, 0, 16, VIRTUAL_HEIGHT);
        }
    }

    private void drawEntities() {
        // 1. Obstacles
        for (Obstacle o : obstacles) {
            int type = o.getType();
            TextureRegion reg = (type >= 0 && type < Assets.obstacleRegions.length) ? Assets.obstacleRegions[type] : null;
            if (reg != null) {
                game.batch.draw(reg, (float) o.getX(), (float) o.getY(), (float) o.getWidth(), (float) o.getHeight());
            } else {
                drawRect((float) o.getX(), (float) o.getY(), (float) o.getWidth(), (float) o.getHeight(), Color.RED);
            }
        }

        // 2. Coins (8-frame spin)
        int coinFrame = (int) ((runCycleTime * 10) % 8);
        TextureRegion coinReg = Assets.coinRegions[coinFrame];
        for (Coin c : coins) {
            if (coinReg != null) {
                game.batch.draw(coinReg, (float) c.getX(), (float) c.getY(), (float) c.getSize(), (float) c.getSize());
            } else {
                drawRect((float) c.getX(), (float) c.getY(), (float) c.getSize(), (float) c.getSize(), Color.YELLOW);
            }
        }

        // 3. Life Power-ups
        for (LifePowerUp lpu : lifePowerUps) {
            if (Assets.heartImage != null) {
                game.batch.draw(Assets.heartImage, (float) lpu.getX(), (float) lpu.getY(), (float) lpu.getSize(), (float) lpu.getSize());
            } else {
                drawRect((float) lpu.getX(), (float) lpu.getY(), (float) lpu.getSize(), (float) lpu.getSize(), Color.MAGENTA);
            }
        }

        // 4. Power-ups
        for (PowerUp pu : powerUps) {
            Texture icon = null;
            switch (pu.getType()) {
                case SHIELD: icon = Assets.shieldImage; break;
                case MAGNET: icon = Assets.magnetImage; break;
                case BOOST: icon = Assets.boostImage; break;
                case DOUBLE_SCORE: icon = Assets.doubleScoreImage; break;
                case JETPACK: icon = Assets.jetpackImage; break;
                case SLOW_MO: icon = Assets.slowMoImage; break;
            }
            if (icon != null) {
                game.batch.draw(icon, (float) pu.getX(), (float) pu.getY(), (float) pu.getSize(), (float) pu.getSize());
            } else {
                drawRect((float) pu.getX(), (float) pu.getY(), (float) pu.getSize(), (float) pu.getSize(), Color.CYAN);
            }
        }

        // 5. Player
        // Invulnerability blinking
        boolean blinkVisible = (invincibilityTimer <= 0) || (((int) (invincibilityTimer * 12)) % 2 == 0);
        if (blinkVisible) {
            Texture playerTex = Assets.playerRun1;
            if (player.isFlying()) {
                playerTex = (Assets.jetpackImage != null) ? Assets.playerJump : Assets.playerRun1;
            } else if (player.isJumping()) {
                playerTex = (Assets.playerJump != null) ? Assets.playerJump : Assets.playerRun1;
            } else if (player.isSliding()) {
                playerTex = (Assets.playerSlide != null) ? Assets.playerSlide : Assets.playerRun1;
            } else {
                // Alternating run frames
                boolean frame2 = ((int) (runCycleTime * 6) % 2 == 1);
                playerTex = (frame2 && Assets.playerRun2 != null) ? Assets.playerRun2 : Assets.playerRun1;
            }

            if (playerTex != null) {
                float pw = PLAYER_WIDTH;
                float ph = PLAYER_HEIGHT;
                if (player.isSliding()) ph *= SLIDE_HEIGHT_RATIO;
                game.batch.draw(playerTex, (float) player.getX(), (float) player.getY(), pw, ph);
            }

            // Shield bubble aura
            if (shieldTimer > 0) {
                game.batch.setColor(0.44f, 0.77f, 1.0f, 0.45f);
                if (Assets.shieldImage != null) {
                    game.batch.draw(Assets.shieldImage, (float) player.getX() - 8, (float) player.getY() - 8, PLAYER_WIDTH + 16, PLAYER_HEIGHT + 16);
                }
                game.batch.setColor(Color.WHITE);
            }
        }

        // 6. Particles
        for (Particle p : particles) {
            game.batch.setColor(p.color.r, p.color.g, p.color.b, p.getAlpha());
            game.batch.draw(Assets.pixel, (float) p.x, (float) p.y, p.size, p.size);
            game.batch.setColor(Color.WHITE);
        }

        // 7. Floating Texts
        for (FloatingText ft : floatingTexts) {
            Assets.fontSmall.setColor(ft.color.r, ft.color.g, ft.color.b, ft.getAlpha());
            Assets.fontSmall.draw(game.batch, ft.text, (float) ft.x, (float) ft.y);
            Assets.fontSmall.setColor(Color.WHITE);
        }
    }

    private void drawHUD() {
        // Top HUD Header Bar
        drawRect(0, 0, VIRTUAL_WIDTH, 48, new Color(0.06f, 0.09f, 0.15f, 0.85f));

        // Score
        Assets.fontMedium.setColor(1f, 0.85f, 0.2f, 1f);
        Assets.fontMedium.draw(game.batch, String.format("%,d", score), 16, 32);

        // Distance & Coins
        Assets.fontSmall.setColor(0.5f, 0.8f, 1f, 1f);
        Assets.fontSmall.draw(game.batch, distance + "m", 160, 31);

        if (Assets.coinRegions[0] != null) {
            game.batch.draw(Assets.coinRegions[0], 235, 14, 18, 18);
        }
        Assets.fontSmall.setColor(1f, 0.9f, 0.3f, 1f);
        Assets.fontSmall.draw(game.batch, String.valueOf(coinsCollected), 260, 31);

        // Hearts / Lives
        float heartX = 330;
        for (int i = 0; i < lives; i++) {
            if (Assets.heartImage != null) {
                game.batch.draw(Assets.heartImage, heartX + i * 22, 14, 18, 18);
            } else {
                drawRect(heartX + i * 22, 14, 18, 18, Color.RED);
            }
        }

        // Active Ability Countdown Bars (under header)
        float barY = 52;
        drawAbilityBar("SHIELD", shieldTimer, barY, new Color(0.44f, 0.77f, 1.0f, 1f));
        if (shieldTimer > 0) barY += 16;
        drawAbilityBar("MAGNET", magnetTimer, barY, new Color(1.0f, 0.59f, 0.20f, 1f));
        if (magnetTimer > 0) barY += 16;
        drawAbilityBar("BOOST", boostTimer, barY, new Color(1.0f, 0.75f, 0.24f, 1f));
        if (boostTimer > 0) barY += 16;
        drawAbilityBar("x2 SCORE", doubleScoreTimer, barY, new Color(0.75f, 0.55f, 1.0f, 1f));
        if (doubleScoreTimer > 0) barY += 16;
        drawAbilityBar("JETPACK", jetpackTimer, barY, new Color(0.47f, 0.78f, 1.0f, 1f));
        if (jetpackTimer > 0) barY += 16;
        drawAbilityBar("SLOW-MO", slowMoTimer, barY, new Color(0.35f, 0.90f, 0.86f, 1f));
    }

    private void drawAbilityBar(String name, double timer, float y, Color col) {
        if (timer <= 0) return;
        float progress = (float) (timer / ABILITY_DURATION);
        drawRect(16, y, 130, 10, new Color(0.1f, 0.1f, 0.1f, 0.7f));
        drawRect(16, y, 130 * progress, 10, col);
        Assets.fontSmall.setColor(col);
        Assets.fontSmall.draw(game.batch, name, 152, y + 10);
    }

    private void drawPauseOverlay() {
        drawRect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT, new Color(0f, 0f, 0f, 0.65f));

        layout.setText(Assets.fontLarge, "PAUSED");
        Assets.fontLarge.setColor(Color.WHITE);
        Assets.fontLarge.draw(game.batch, "PAUSED", (VIRTUAL_WIDTH - layout.width) / 2f, 320);

        layout.setText(Assets.fontSmall, "Tap or press P to resume");
        Assets.fontSmall.setColor(Color.LIGHT_GRAY);
        Assets.fontSmall.draw(game.batch, layout, (VIRTUAL_WIDTH - layout.width) / 2f, 370);
    }

    private void drawGameOverOverlay() {
        drawRect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT, new Color(0.04f, 0.05f, 0.08f, 0.85f));

        layout.setText(Assets.fontLarge, "GAME OVER");
        Assets.fontLarge.setColor(1f, 0.25f, 0.25f, 1f);
        Assets.fontLarge.draw(game.batch, "GAME OVER", (VIRTUAL_WIDTH - layout.width) / 2f, 180);

        // Score Card
        drawRect(50, 210, 320, 140, new Color(0.11f, 0.15f, 0.24f, 0.95f));

        Assets.fontSmall.setColor(Color.LIGHT_GRAY);
        Assets.fontSmall.draw(game.batch, "FINAL SCORE:", 70, 245);
        Assets.fontSmall.draw(game.batch, "COINS COLLECTED:", 70, 275);
        Assets.fontSmall.draw(game.batch, "DISTANCE RUN:", 70, 305);

        Assets.fontMedium.setColor(1f, 0.85f, 0.2f, 1f);
        Assets.fontMedium.draw(game.batch, String.format("%,d", score), 220, 247);
        Assets.fontSmall.setColor(1f, 0.9f, 0.3f, 1f);
        Assets.fontSmall.draw(game.batch, String.valueOf(coinsCollected), 220, 275);
        Assets.fontSmall.setColor(0.5f, 0.8f, 1f, 1f);
        Assets.fontSmall.draw(game.batch, distance + " m", 220, 305);

        if (!gameOverMessage.isEmpty()) {
            layout.setText(Assets.fontSmall, gameOverMessage);
            Assets.fontSmall.setColor(newHighScore ? new Color(1f, 0.85f, 0.2f, 1f) : Color.WHITE);
            Assets.fontSmall.draw(game.batch, layout, (VIRTUAL_WIDTH - layout.width) / 2f, 335);
        }

        // PLAY AGAIN Button (x: 80 to 340, y: 440 to 490)
        drawRect(80, 440, 260, 50, new Color(0.18f, 0.65f, 0.35f, 1f));
        layout.setText(Assets.fontMedium, "PLAY AGAIN");
        Assets.fontMedium.setColor(Color.WHITE);
        Assets.fontMedium.draw(game.batch, "PLAY AGAIN", (VIRTUAL_WIDTH - layout.width) / 2f, 472);

        // MAIN MENU Button (x: 80 to 340, y: 510 to 560)
        drawRect(80, 510, 260, 50, new Color(0.25f, 0.35f, 0.5f, 1f));
        layout.setText(Assets.fontMedium, "MAIN MENU");
        Assets.fontMedium.setColor(Color.WHITE);
        Assets.fontMedium.draw(game.batch, "MAIN MENU", (VIRTUAL_WIDTH - layout.width) / 2f, 542);
    }

    private void drawRect(float x, float y, float w, float h, Color c) {
        game.batch.setColor(c);
        game.batch.draw(Assets.pixel, x, y, w, h);
        game.batch.setColor(Color.WHITE);
    }

    // Input actions implementation
    @Override
    public void onMoveLeft() {
        if (state == GameState.PLAYING) player.moveLeft();
    }

    @Override
    public void onMoveRight() {
        if (state == GameState.PLAYING) player.moveRight();
    }

    @Override
    public void onJump() {
        if (state == GameState.PLAYING) {
            player.jump();
            AudioManager.play("jump");
        }
    }

    @Override
    public void onSlide() {
        if (state == GameState.PLAYING && player.isOnGround() && !player.isSliding()) {
            player.slide();
            AudioManager.play("jump");
        }
    }

    @Override
    public void onTogglePause() {
        if (state == GameState.PLAYING) state = GameState.PAUSED;
        else if (state == GameState.PAUSED) state = GameState.PLAYING;
    }

    @Override
    public void onRestart() {
        if (state == GameState.GAME_OVER) {
            AudioManager.play("click");
            resetGame();
        }
    }

    @Override
    public void onToggleMute() {
        AudioManager.toggleMute();
    }

    @Override
    public void onTouchTap(float worldX, float worldY) {
        if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
            return;
        }

        if (state == GameState.GAME_OVER) {
            // PLAY AGAIN (x: 80 to 340, y: 440 to 490)
            if (worldX >= 80 && worldX <= 340 && worldY >= 440 && worldY <= 490) {
                AudioManager.play("click");
                resetGame();
                return;
            }
            // MAIN MENU (x: 80 to 340, y: 510 to 560)
            if (worldX >= 80 && worldX <= 340 && worldY >= 510 && worldY <= 560) {
                AudioManager.play("click");
                game.showMenu();
                return;
            }
        }
    }

    @Override public void resize(int width, int height) { game.viewport.update(width, height); }
    @Override public void pause() { if (state == GameState.PLAYING) state = GameState.PAUSED; }
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {}
}
