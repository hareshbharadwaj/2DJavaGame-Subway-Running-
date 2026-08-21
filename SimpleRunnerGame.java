import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.swing.*;

public class SimpleRunnerGame extends JFrame {
    public SimpleRunnerGame() {
        super("Simple Runner Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setContentPane(new GamePanel());
        pack();
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SimpleRunnerGame game = new SimpleRunnerGame();
            game.setVisible(true);
        });
    }

    private static class GamePanel extends JPanel implements ActionListener {
        private enum GameState {
            START_MENU,
            PLAYING,
            PAUSED,
            GAME_OVER
        }

        private static final int WIDTH = 420;
        private static final int HEIGHT = 760;
        private static final int ROAD_X = 50;
        private static final int ROAD_Y = 92;
        private static final int ROAD_W = 320;
        private static final int ROAD_H = 560;
        private static final int[] LANE_X = {105, 185, 265};
        private static final int PLAYER_WIDTH = 44;
        private static final int PLAYER_HEIGHT = 56;
        private static final double GROUND_Y = ROAD_Y + ROAD_H - 92.0; // 560.0
        private static final double GRAVITY = 1750.0; // px/s^2
        private static final double JUMP_STRENGTH = 560.0; // px/s, yields ~90px apex & ~0.64s jump
        private static final double MAX_JUMP_APEX = (JUMP_STRENGTH * JUMP_STRENGTH) / (2.0 * GRAVITY);
        private static final double INITIAL_SPEED = 300.0; // px/s
        private static final double MAX_SPEED = 560.0; // px/s
        private static final int TARGET_FPS = 60;
        private static final int MAX_LIVES = 3;

        private GameState gameState = GameState.START_MENU;
        private final Player player = new Player();
        private final List<Obstacle> obstacles = new ArrayList<>();
        private final List<Coin> coins = new ArrayList<>();
        private final List<LifePowerUp> lifePowerUps = new ArrayList<>();
        private final List<FloatingText> floatingTexts = new ArrayList<>();
        private final List<Particle> particles = new ArrayList<>();
        private final Random random = new Random();
        private final Timer timer = new Timer(1000 / TARGET_FPS, this);

        private long lastTime = System.nanoTime();
        private double obstacleSpeed = INITIAL_SPEED;
        private double spawnTimer = 0.0;
        private double spawnInterval = 0.85; // seconds (decreases dynamically with difficulty)
        private double invincibilityTimer = 0.0;
        private double screenShakeTimer = 0.0;
        private double shakeIntensity = 0.0;
        private double roadScroll = 0.0;
        private double runCycleTime = 0.0;
        private double scoreAccumulator = 0.0;
        private double distanceAccumulator = 0.0;

        private long score = 0;
        private int coinsCollected = 0;
        private int distance = 0;
        private int lives = 0;
        private int lastSpawnedLane = 1;
        private int consecutiveLaneCount = 0;
        private boolean debugMode = false;
        private double rollingFps = 60.0;
        private double currentDeltaTime = 0.016;

        private boolean leftPressed = false;
        private boolean rightPressed = false;
        private boolean upPressed = false;

        public GamePanel() {
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setFocusable(true);
            setBackground(new Color(12, 18, 30));
            installKeyBindings();
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    handleTouch(e.getX(), e.getY());
                }
            });
            timer.start();
        }

        // Unified input handling for touch / mouse
        private void handleTouch(int x, int y) {
            int buttonY = HEIGHT - 86;
            int buttonSize = 46;
            if (gameState == GameState.START_MENU) {
                resetGame();
                return;
            }
            if (y < buttonY || y > buttonY + buttonSize || gameState != GameState.PLAYING) {
                return;
            }
            if (x >= 95 && x <= 141) {
                moveLeft();
            } else if (x >= 155 && x <= 201) {
                moveRight();
            } else if (x >= 265 && x <= 311) {
                jump();
            }
        }

        public void moveLeft() {
            if (gameState == GameState.PLAYING) {
                player.moveLeft();
            }
        }

        public void moveRight() {
            if (gameState == GameState.PLAYING) {
                player.moveRight();
            }
        }

        public void jump() {
            if (gameState == GameState.START_MENU) {
                resetGame();
                return;
            }
            if (gameState == GameState.PLAYING) {
                player.jump();
            }
        }

        public void togglePause() {
            if (gameState == GameState.PLAYING) {
                gameState = GameState.PAUSED;
            } else if (gameState == GameState.PAUSED) {
                gameState = GameState.PLAYING;
                lastTime = System.nanoTime(); // Reset delta-time tracker on unpause to prevent time jumps
            }
        }

        public void restartGame() {
            if (gameState == GameState.GAME_OVER) {
                resetGame();
            }
        }

        private void installKeyBindings() {
            InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
            ActionMap actionMap = getActionMap();

            inputMap.put(KeyStroke.getKeyStroke("pressed A"), "laneLeft");
            inputMap.put(KeyStroke.getKeyStroke("pressed LEFT"), "laneLeft");
            inputMap.put(KeyStroke.getKeyStroke("pressed D"), "laneRight");
            inputMap.put(KeyStroke.getKeyStroke("pressed RIGHT"), "laneRight");
            inputMap.put(KeyStroke.getKeyStroke("pressed W"), "jump");
            inputMap.put(KeyStroke.getKeyStroke("pressed UP"), "jump");
            inputMap.put(KeyStroke.getKeyStroke("pressed SPACE"), "jump");
            inputMap.put(KeyStroke.getKeyStroke("pressed P"), "pause");
            inputMap.put(KeyStroke.getKeyStroke("pressed R"), "restart");
            inputMap.put(KeyStroke.getKeyStroke("pressed F3"), "toggleDebug");

            inputMap.put(KeyStroke.getKeyStroke("released A"), "laneLeftRelease");
            inputMap.put(KeyStroke.getKeyStroke("released LEFT"), "laneLeftRelease");
            inputMap.put(KeyStroke.getKeyStroke("released D"), "laneRightRelease");
            inputMap.put(KeyStroke.getKeyStroke("released RIGHT"), "laneRightRelease");
            inputMap.put(KeyStroke.getKeyStroke("released W"), "jumpRelease");
            inputMap.put(KeyStroke.getKeyStroke("released UP"), "jumpRelease");
            inputMap.put(KeyStroke.getKeyStroke("released SPACE"), "jumpRelease");

            actionMap.put("laneLeft", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!leftPressed) {
                        moveLeft();
                        leftPressed = true;
                    }
                }
            });
            actionMap.put("laneLeftRelease", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    leftPressed = false;
                }
            });

            actionMap.put("laneRight", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!rightPressed) {
                        moveRight();
                        rightPressed = true;
                    }
                }
            });
            actionMap.put("laneRightRelease", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    rightPressed = false;
                }
            });

            actionMap.put("jump", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!upPressed) {
                        jump();
                        upPressed = true;
                    }
                }
            });
            actionMap.put("jumpRelease", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    upPressed = false;
                }
            });

            actionMap.put("pause", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    togglePause();
                }
            });

            actionMap.put("restart", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    restartGame();
                }
            });

            actionMap.put("toggleDebug", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    debugMode = !debugMode;
                }
            });
        }

        private void resetGame() {
            obstacles.clear();
            coins.clear();
            lifePowerUps.clear();
            floatingTexts.clear();
            particles.clear();
            player.reset();

            gameState = GameState.PLAYING;
            score = 0;
            coinsCollected = 0;
            distance = 0;
            lives = 0;
            obstacleSpeed = INITIAL_SPEED;
            spawnTimer = 0.0;
            spawnInterval = 0.85;
            invincibilityTimer = 0.0;
            screenShakeTimer = 0.0;
            shakeIntensity = 0.0;
            roadScroll = 0.0;
            scoreAccumulator = 0.0;
            distanceAccumulator = 0.0;
            lastSpawnedLane = 1;
            consecutiveLaneCount = 0;
            lastTime = System.nanoTime();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            long now = System.nanoTime();
            double deltaTime = (now - lastTime) / 1_000_000_000.0;
            lastTime = now;

            // Reject invalid/negative delta time and clamp max to 0.033s to prevent tunneling
            if (deltaTime <= 0.0) {
                deltaTime = 0.001;
            }
            deltaTime = Math.min(deltaTime, 0.033);
            currentDeltaTime = deltaTime;

            // Rolling average FPS measurement
            rollingFps = rollingFps * 0.9 + (1.0 / deltaTime) * 0.1;

            if (gameState == GameState.PLAYING) {
                updateGame(deltaTime);
            }
            repaint();
        }

        private void updateGame(double deltaTime) {
            // Difficulty scaling: speed increases from 300 to 560 px/s based on distance
            obstacleSpeed = Math.min(MAX_SPEED, INITIAL_SPEED + distance * 0.25);
            // Spawn interval decreases smoothly from 0.85s down to a fair floor of 0.42s
            spawnInterval = Math.max(0.42, 0.85 - (distance * 0.00035));

            // Road scroll (delta-time based)
            roadScroll += obstacleSpeed * deltaTime;

            // Invincibility and screen shake timers (delta-time based)
            if (invincibilityTimer > 0.0) {
                invincibilityTimer = Math.max(0.0, invincibilityTimer - deltaTime);
            }
            if (screenShakeTimer > 0.0) {
                screenShakeTimer = Math.max(0.0, screenShakeTimer - deltaTime);
            }

            // Real distance accumulation (time & speed based, not frame-rate dependent)
            distanceAccumulator += (obstacleSpeed * 0.02) * deltaTime;
            distance = (int) distanceAccumulator;

            // Real score accumulation (time based, not frame-rate dependent)
            scoreAccumulator += 15.0 * deltaTime;
            if (scoreAccumulator >= 1.0) {
                int add = (int) scoreAccumulator;
                score += add;
                scoreAccumulator -= add;
            }

            // Spawning
            spawnTimer += deltaTime;
            if (spawnTimer >= spawnInterval) {
                spawnWorldObjects();
                spawnTimer = 0.0;
            }

            // Entity Updates
            player.update(deltaTime);
            // Dust particles when grounded
            if (player.isOnGround() && random.nextInt(100) < 30) {
                particles.add(new Particle(player.getX() + 10 + random.nextInt(24), player.getY() + PLAYER_HEIGHT - 2, 
                    (random.nextDouble() - 0.5) * 40.0, (random.nextDouble() - 0.5) * 20.0 - 20.0, 
                    new Color(200, 200, 210, 150), 6.0 + random.nextDouble()*4.0, 0.4 + random.nextDouble()*0.3));
            }
            
            runCycleTime += obstacleSpeed * deltaTime;
            updateParticles(deltaTime);

            updateObstacles(deltaTime);
            updateCoins(deltaTime);
            updateLifePowerUps(deltaTime);
            updateFloatingTexts(deltaTime);
            checkCollisions();
        }

        private void spawnWorldObjects() {
            // Guaranteed escape route and reaction window fairness:
            // 1. Never spawn 3 consecutive obstacles on the same lane
            // 2. Guarantee at least one open lane at all times
            int lane = random.nextInt(3);
            if (lane == lastSpawnedLane) {
                consecutiveLaneCount++;
                if (consecutiveLaneCount >= 2) {
                    lane = (lane + 1 + random.nextInt(2)) % 3;
                    consecutiveLaneCount = 0;
                }
            } else {
                consecutiveLaneCount = 0;
            }
            lastSpawnedLane = lane;

            // Obstacle height between 34px and 44px (generously cleared by ~90px jump apex)
            int obsHeight = 34 + random.nextInt(11);
            double spawnY = ROAD_Y - obsHeight - 12.0;

            // Ensure obstacle stays strictly inside road boundaries
            double obsX = LANE_X[lane];
            obsX = Math.max(ROAD_X + 10, Math.min(ROAD_X + ROAD_W - 54, obsX));
            obstacles.add(new Obstacle(obsX, spawnY, lane, obsHeight));

            // Coin Generation (55% chance)
            if (random.nextInt(100) < 55) {
                // 30% chance: spawn elevated coin directly above obstacle to reward jumping
                if (random.nextInt(100) < 30) {
                    coins.add(new Coin(obsX + 13.0, spawnY - 52.0, lane));
                } else {
                    // Spawn ground coin in a separate free lane without obstacle overlap
                    int coinLane = (lane + 1 + random.nextInt(2)) % 3;
                    double coinX = LANE_X[coinLane] + 13.0;
                    coins.add(new Coin(coinX, spawnY - 10.0, coinLane));
                    // 25% chance of a 2-coin sequence
                    if (random.nextInt(100) < 25) {
                        coins.add(new Coin(coinX, spawnY - 38.0, coinLane));
                    }
                }
            }

            // Life Power-Up Generation (15% chance, only spawned when lives < MAX_LIVES)
            if (lives < MAX_LIVES && random.nextInt(100) < 15) {
                // Pick a lane different from obstacle
                int lifeLane = (lane + 1 + random.nextInt(2)) % 3;
                double lifeX = LANE_X[lifeLane] + 11.0;
                // Prevent exact overlap with ground coins
                double lifeY = spawnY - 26.0;
                lifePowerUps.add(new LifePowerUp(lifeX, lifeY, lifeLane));
            }
        }

        private void updateObstacles(double deltaTime) {
            Iterator<Obstacle> iterator = obstacles.iterator();
            while (iterator.hasNext()) {
                Obstacle obstacle = iterator.next();
                obstacle.update(deltaTime, obstacleSpeed);

                // Passed obstacle reward (+40 awarded exactly once when obstacle passes player)
                if (!obstacle.isPassed() && obstacle.getY() > player.getY() + PLAYER_HEIGHT) {
                    obstacle.setPassed(true);
                    score += 40;
                    floatingTexts.add(new FloatingText(obstacle.getX() + 8, player.getY() - 14, "+40", new Color(112, 196, 255), 0.6));
                }

                if (obstacle.isOffScreen()) {
                    iterator.remove();
                }
            }
        }

        private void updateCoins(double deltaTime) {
            Iterator<Coin> iterator = coins.iterator();
            while (iterator.hasNext()) {
                Coin coin = iterator.next();
                coin.update(deltaTime, obstacleSpeed);
                if (coin.isOffScreen()) {
                    iterator.remove();
                }
            }
        }

        private void updateLifePowerUps(double deltaTime) {
            Iterator<LifePowerUp> iterator = lifePowerUps.iterator();
            while (iterator.hasNext()) {
                LifePowerUp lifePowerUp = iterator.next();
                lifePowerUp.update(deltaTime, obstacleSpeed);
                if (lifePowerUp.isOffScreen()) {
                    iterator.remove();
                }
            }
        }

        private void updateParticles(double deltaTime) {
            Iterator<Particle> iterator = particles.iterator();
            while (iterator.hasNext()) {
                Particle p = iterator.next();
                p.update(deltaTime, obstacleSpeed);
                if (p.isDead()) iterator.remove();
            }
        }

        private void updateFloatingTexts(double deltaTime) {
            Iterator<FloatingText> iterator = floatingTexts.iterator();
            while (iterator.hasNext()) {
                FloatingText ft = iterator.next();
                ft.update(deltaTime);
                if (ft.isDead()) {
                    iterator.remove();
                }
            }
        }

        private void checkCollisions() {
            Rectangle2D.Double playerHitbox = player.getHitbox();

            // Obstacle collision (evaluated geometrically via Rectangle2D.intersects)
            if (invincibilityTimer <= 0.0) {
                Iterator<Obstacle> iterator = obstacles.iterator();
                while (iterator.hasNext()) {
                    Obstacle obstacle = iterator.next();
                    if (!obstacle.isHit() && playerHitbox.intersects(obstacle.getHitbox())) {
                        obstacle.setHit(true);
                        iterator.remove(); // Remove immediately to prevent duplicate collision

                        if (lives > 0) {
                            lives--;
                            invincibilityTimer = 1.2; // 1.2s invulnerability frames
                            screenShakeTimer = 0.28;
                            shakeIntensity = 5.0;
                            floatingTexts.add(new FloatingText(player.getX() + 4, player.getY() - 12, "-1 LIFE", new Color(255, 90, 90), 0.9));
                        } else {
                            gameState = GameState.GAME_OVER;
                            screenShakeTimer = 0.4;
                            shakeIntensity = 8.0;
                            floatingTexts.add(new FloatingText(player.getX() - 4, player.getY() - 12, "CRASH!", new Color(255, 60, 60), 1.2));
                            for (int i=0; i<15; i++) particles.add(new Particle(player.getX()+20, player.getY()+20, (random.nextDouble()-0.5)*200, (random.nextDouble()-0.5)*200, new Color(255,80,80), 8, 0.6));
                        }
                        break;
                    }
                }
            }

            // Coin collection
            Iterator<Coin> coinIterator = coins.iterator();
            while (coinIterator.hasNext()) {
                Coin coin = coinIterator.next();
                if (playerHitbox.intersects(coin.getHitbox())) {
                    coinsCollected++;
                    score += 120;
                    floatingTexts.add(new FloatingText(coin.getX() - 2, coin.getY() - 8, "+120", new Color(255, 234, 90), 0.7));
                    for (int i=0; i<6; i++) particles.add(new Particle(coin.getX()+8, coin.getY()+8, (random.nextDouble()-0.5)*100, (random.nextDouble()-0.5)*100, new Color(255,234,90), 6, 0.5));
                    coinIterator.remove();
                }
            }

            // Life power-up collection
            Iterator<LifePowerUp> lifeIterator = lifePowerUps.iterator();
            while (lifeIterator.hasNext()) {
                LifePowerUp lifePowerUp = lifeIterator.next();
                if (playerHitbox.intersects(lifePowerUp.getHitbox())) {
                    if (lives < MAX_LIVES) {
                        lives++;
                        floatingTexts.add(new FloatingText(lifePowerUp.getX() - 10, lifePowerUp.getY() - 8, "+1 LIFE", new Color(255, 110, 160), 0.8));
                    } else {
                        floatingTexts.add(new FloatingText(lifePowerUp.getX() - 6, lifePowerUp.getY() - 8, "+250", new Color(255, 110, 160), 0.8));
                    }
                    score += 250;
                    for (int i=0; i<10; i++) particles.add(new Particle(lifePowerUp.getX()+10, lifePowerUp.getY()+10, (random.nextDouble()-0.5)*120, (random.nextDouble()-0.5)*120, new Color(255,92,132), 7, 0.6));
                    lifeIterator.remove();
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Screen shake transform
            if (screenShakeTimer > 0.0) {
                double shakeX = (random.nextDouble() * 2.0 - 1.0) * shakeIntensity;
                double shakeY = (random.nextDouble() * 2.0 - 1.0) * shakeIntensity;
                g2.translate(shakeX, shakeY);
            }

            drawBackground(g2);
            drawPhoneFrame(g2);
            drawRoad(g2);
            drawHeader(g2);
            drawObstacles(g2);
            drawCoins(g2);
            drawLifePowerUps(g2);
            drawPlayer(g2);
            drawFloatingTexts(g2);
            drawParticles(g2);
            drawFooter(g2);
            drawMobileControls(g2);

            if (debugMode) {
                drawDebugOverlay(g2);
            }

            if (gameState == GameState.START_MENU) {
                drawStartMenuOverlay(g2);
            } else if (gameState == GameState.GAME_OVER) {
                drawGameOverOverlay(g2);
            } else if (gameState == GameState.PAUSED) {
                drawPausedOverlay(g2);
            }

            g2.dispose();
        }

        
        private void drawParticles(Graphics2D g2) {
            for (Particle p : particles) {
                p.draw(g2);
            }
        }

        private void drawStartMenuOverlay(Graphics2D g2) {
            g2.setColor(new Color(12, 18, 30, 200));
            g2.fillRect(0, 0, WIDTH, HEIGHT);
            
            g2.setColor(new Color(52, 208, 255));
            g2.setFont(new Font("SansSerif", Font.BOLD, 36));
            drawCenteredText(g2, "NEON RUNNER", 240);
            
            double pulse = Math.abs(Math.sin(System.nanoTime() / 3e8));
            g2.setColor(new Color(255, 255, 255, (int)(100 + 155 * pulse)));
            g2.setFont(new Font("SansSerif", Font.BOLD, 18));
            drawCenteredText(g2, "Tap or Press Space to Start", 320);
        }

        
        private void drawBackground(Graphics2D g2) {
            // Sky gradient
            GradientPaint sky = new GradientPaint(0, 0, new Color(10, 14, 26), 0, HEIGHT, new Color(20, 26, 46));
            g2.setPaint(sky);
            g2.fillRect(0, 0, WIDTH, HEIGHT);

            // Parallax Cityscape
            g2.setColor(new Color(16, 20, 34));
            int layer1Scroll = (int)(distanceAccumulator * 1.5) % 800;
            for (int i = -1; i < 3; i++) {
                int baseX = i * 200 - (layer1Scroll % 200);
                g2.fillRect(baseX + 20, 80, 40, HEIGHT);
                g2.fillRect(baseX + 70, 140, 50, HEIGHT);
                g2.fillRect(baseX + 130, 50, 30, HEIGHT);
            }
            
            g2.setColor(new Color(24, 30, 48));
            int layer2Scroll = (int)(distanceAccumulator * 3.0) % 800;
            for (int i = -1; i < 3; i++) {
                int baseX = i * 200 - (layer2Scroll % 200);
                g2.fillRect(baseX + 10, 160, 60, HEIGHT);
                g2.fillRect(baseX + 90, 110, 40, HEIGHT);
                g2.fillRect(baseX + 150, 200, 35, HEIGHT);
            }
        }

        private void drawPhoneFrame(Graphics2D g2) {
            g2.setColor(new Color(20, 26, 46));
            g2.fillRoundRect(24, 24, WIDTH - 48, HEIGHT - 48, 50, 50);

            g2.setColor(new Color(72, 82, 128));
            g2.setStroke(new BasicStroke(4f));
            g2.drawRoundRect(24, 24, WIDTH - 48, HEIGHT - 48, 50, 50);

            g2.setColor(new Color(255, 255, 255, 120));
            g2.fillOval(WIDTH / 2 - 28, 34, 56, 8);
            g2.fillOval(WIDTH / 2 - 10, HEIGHT - 36, 20, 8);
        }

        private void drawRoad(Graphics2D g2) {
            RoundRectangle2D road = new RoundRectangle2D.Double(ROAD_X, ROAD_Y, ROAD_W, ROAD_H, 28, 28);
            g2.setColor(new Color(30, 42, 76));
            g2.fill(road);
            g2.setColor(new Color(255, 255, 255, 40));
            g2.setStroke(new BasicStroke(3f));
            g2.draw(road);

            int roadDashOffset = (int) (roadScroll % 28);
            g2.setColor(new Color(255, 255, 255, 80));
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{14, 14}, roadDashOffset));
            int laneWidth = ROAD_W / 3;
            g2.drawLine(ROAD_X + laneWidth, ROAD_Y + 18, ROAD_X + laneWidth, ROAD_Y + ROAD_H - 18);
            g2.drawLine(ROAD_X + laneWidth * 2, ROAD_Y + 18, ROAD_X + laneWidth * 2, ROAD_Y + ROAD_H - 18);

            g2.setStroke(new BasicStroke(1f));
            for (int lane = 0; lane < 3; lane++) {
                int x = LANE_X[lane] + PLAYER_WIDTH / 2;
                g2.setColor(new Color(255, 255, 255, 16));
                g2.fillRect(x, ROAD_Y + 12, 4, ROAD_H - 24);
            }

            g2.setColor(new Color(255, 255, 255, 20));
            for (int i = 0; i < 18; i++) {
                int x = ROAD_X + 28 + i * 22 + (roadDashOffset % 22);
                g2.fillRect(x, ROAD_Y + ROAD_H - 30, 14, 4);
            }
        }

        private void drawHeader(Graphics2D g2) {
            g2.setColor(new Color(18, 26, 48, 235));
            g2.fillRoundRect(24, 14, WIDTH - 48, 30, 14, 14);
            g2.setColor(new Color(112, 190, 240));
            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2.drawString("Score " + String.format("%06d", score), 34, 34);
            g2.drawString("Coins " + String.format("%03d", coinsCollected), 140, 34);
            g2.drawString("Dist " + distance + "m", 232, 34);

            // Lives counter with hearts
            g2.setColor(new Color(255, 100, 130));
            g2.drawString("❤️ " + lives, 326, 34);
        }

        private void drawPlayer(Graphics2D g2) {
            // Flash/blink during invincibility frames
            if (invincibilityTimer > 0.0) {
                int flashStep = (int) (invincibilityTimer * 16.0);
                if (flashStep % 2 == 0) {
                    return; // Skip render frame for clean blinking effect
                }
            }

            double pX = player.getX();
            double pY = player.getY();
            double heightAboveGround = Math.max(0.0, GROUND_Y - pY);

            // Dynamic ground shadow based on actual height above ground
            double shadowRatio = Math.max(0.2, 1.0 - (heightAboveGround / MAX_JUMP_APEX));
            int shadowWidth = (int) (PLAYER_WIDTH * 0.8 * shadowRatio);
            int shadowHeight = (int) (8 * shadowRatio);
            int shadowX = (int) (pX + (PLAYER_WIDTH - shadowWidth) / 2.0);
            int shadowY = (int) (GROUND_Y + PLAYER_HEIGHT - 3);

            if (!player.isOnGround() && heightAboveGround > 40.0) {
                g2.setColor(new Color(100, 255, 100, (int) (120 * shadowRatio))); // Green glow for safe clearance
            } else {
                g2.setColor(new Color(0, 0, 0, (int) (90 * shadowRatio)));
            }
            g2.fillOval(shadowX, shadowY, shadowWidth, shadowHeight);

            // Visual scale smoothly increases at jump apex
            double jumpScale = 1.0 + 0.12 * Math.min(1.0, heightAboveGround / MAX_JUMP_APEX);

            Graphics2D playerG2 = (Graphics2D) g2.create();
            playerG2.translate(pX + PLAYER_WIDTH / 2.0, pY + PLAYER_HEIGHT);
            playerG2.scale(jumpScale, jumpScale);

            int drawX = -PLAYER_WIDTH / 2;
            int drawY = -PLAYER_HEIGHT;

            // Player body
            playerG2.setColor(new Color(52, 208, 255));
            playerG2.fillRoundRect(drawX, drawY, PLAYER_WIDTH, PLAYER_HEIGHT, 10, 10);

            // Highlights & Face
            playerG2.setColor(new Color(255, 255, 255));
            playerG2.fillOval(drawX + 8, drawY + 6, 16, 16);
            playerG2.setColor(new Color(32, 40, 62));
            playerG2.fillOval(drawX + 14, drawY + 10, 5, 5);

            // Tie / Accent
            playerG2.setColor(new Color(255, 114, 114));
            playerG2.fillRect(drawX + 7, drawY + 26, 20, 12);

            
            // Run cycle feet
            if (player.isOnGround()) {
                double footCycle = Math.sin(runCycleTime * 0.04);
                int leftFootY = drawY + PLAYER_HEIGHT + (int)(footCycle * 6);
                int rightFootY = drawY + PLAYER_HEIGHT + (int)(-footCycle * 6);
                playerG2.setColor(new Color(30, 150, 200));
                playerG2.fillRoundRect(drawX + 6, leftFootY, 12, 8, 4, 4);
                playerG2.fillRoundRect(drawX + 26, rightFootY, 12, 8, 4, 4);
            }
            
            playerG2.dispose();

        }

        
        private void drawObstacles(Graphics2D g2) {
            for (Obstacle obstacle : obstacles) {
                int x = (int) obstacle.getX();
                int y = (int) obstacle.getY();
                int w = (int) obstacle.getWidth();
                int h = (int) obstacle.getHeight();

                // Base block
                g2.setColor(new Color(240, 60, 60));
                g2.fillRoundRect(x, y, w, h, 6, 6);
                
                // Warning stripes
                g2.setColor(new Color(255, 200, 40));
                Graphics2D obsG2 = (Graphics2D) g2.create();
                obsG2.setClip(new RoundRectangle2D.Double(x, y, w, h, 6, 6));
                for(int i=-w; i<w+h; i+=12) {
                    obsG2.fillPolygon(new int[]{x+i, x+i+6, x+i+6-h, x+i-h}, new int[]{y, y, y+h, y+h}, 4);
                }
                obsG2.dispose();
                
                // Top highlight
                g2.setColor(new Color(255, 255, 255, 120));
                g2.fillRect(x + 2, y + 2, w - 4, 3);
            }
        }

        private void drawCoins(Graphics2D g2) {
            for (Coin coin : coins) {
                int x = (int) coin.getX();
                int y = (int) coin.getY();
                int size = (int) coin.getSize();
                
                double spin = Math.abs(Math.sin((distanceAccumulator + coin.hashCode()) * 0.015));
                int spinWidth = (int)(size * Math.max(0.1, spin));
                int offsetX = (size - spinWidth) / 2;

                g2.setColor(new Color(242, 206, 80));
                g2.fillOval(x + offsetX, y, spinWidth, size);
                g2.setColor(new Color(255, 255, 255, 210));
                if (spinWidth > size * 0.4) {
                    g2.fillOval(x + offsetX + spinWidth/4, y + size/4, spinWidth/2, size/2);
                }
            }
        }

        private void drawLifePowerUps(Graphics2D g2) {
            for (LifePowerUp lifePowerUp : lifePowerUps) {
                int x = (int) lifePowerUp.getX();
                int y = (int) lifePowerUp.getY();
                int size = (int) lifePowerUp.getSize();

                g2.setColor(new Color(255, 92, 132));
                g2.fillOval(x, y, size, size);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 14));
                g2.drawString("+", x + 6, y + 16);
            }
        }

        private void drawFloatingTexts(Graphics2D g2) {
            for (FloatingText ft : floatingTexts) {
                ft.draw(g2);
            }
        }

        private void drawFooter(Graphics2D g2) {
            g2.setColor(new Color(255, 255, 255, 60));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2.drawString("A/D or touch: lanes  |  Space/Up: jump  |  P: pause", 46, HEIGHT - 16);
        }

        private void drawMobileControls(Graphics2D g2) {
            int buttonY = HEIGHT - 86;
            int buttonSize = 46;
            int leftX = 95;
            int rightX = 155;
            int jumpX = 265;

            drawTouchButton(g2, leftX, buttonY, buttonSize, "<");
            drawTouchButton(g2, rightX, buttonY, buttonSize, ">");
            drawTouchButton(g2, jumpX, buttonY, buttonSize, "^");

            g2.setColor(new Color(255, 255, 255, 140));
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.drawString("Touch controls", 155, buttonY + 74);
        }

        private void drawTouchButton(Graphics2D g2, int x, int y, int size, String label) {
            g2.setColor(new Color(24, 30, 50, 230));
            g2.fillRoundRect(x, y, size, size, 16, 16);
            g2.setColor(new Color(112, 196, 255));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(x, y, size, size, 16, 16);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 24));
            int labelWidth = g2.getFontMetrics().stringWidth(label);
            g2.drawString(label, x + (size - labelWidth) / 2, y + size - 14);
        }

        private void drawGameOverOverlay(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 185));
            g2.fillRect(0, 0, WIDTH, HEIGHT);
            g2.setColor(new Color(255, 90, 90));
            g2.setFont(new Font("SansSerif", Font.BOLD, 30));
            drawCenteredText(g2, "GAME OVER", 320);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 18));
            drawCenteredText(g2, "Final Score: " + score, 355);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 15));
            drawCenteredText(g2, "Coins: " + coinsCollected + "  |  Distance: " + distance + "m", 380);

            g2.setColor(new Color(112, 196, 255));
            g2.setFont(new Font("SansSerif", Font.BOLD, 16));
            drawCenteredText(g2, "Press 'R' to Restart", 420);
        }

        private void drawPausedOverlay(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, WIDTH, HEIGHT);
            g2.setColor(new Color(255, 255, 255));
            g2.setFont(new Font("SansSerif", Font.BOLD, 28));
            drawCenteredText(g2, "PAUSED", 340);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
            drawCenteredText(g2, "Press 'P' to Resume", 375);
        }

        private void drawDebugOverlay(Graphics2D g2) {
            g2.setStroke(new BasicStroke(1.5f));

            // Player hitbox (green)
            Rectangle2D.Double pb = player.getHitbox();
            g2.setColor(Color.GREEN);
            g2.draw(pb);

            // Obstacles hitbox (red)
            g2.setColor(Color.RED);
            for (Obstacle obstacle : obstacles) {
                g2.draw(obstacle.getHitbox());
            }

            // Coins hitbox (yellow)
            g2.setColor(Color.YELLOW);
            for (Coin coin : coins) {
                g2.draw(coin.getHitbox());
            }

            // Life powerups hitbox (magenta)
            g2.setColor(Color.MAGENTA);
            for (LifePowerUp lpu : lifePowerUps) {
                g2.draw(lpu.getHitbox());
            }

            // Comprehensive Debug telemetry metrics panel
            g2.setColor(new Color(10, 16, 32, 230));
            g2.fillRoundRect(24, 48, WIDTH - 48, 140, 12, 12);
            g2.setColor(new Color(72, 180, 255));
            g2.drawRoundRect(24, 48, WIDTH - 48, 140, 12, 12);

            g2.setColor(Color.CYAN);
            g2.setFont(new Font("Monospaced", Font.BOLD, 10));
            g2.drawString(String.format("FPS: %4.1f | dt: %5.2fms | State: %s", rollingFps, currentDeltaTime * 1000.0, gameState), 32, 64);
            g2.drawString(String.format("Player: X=%.1f Y=%.1f (Gnd=%.1f)", player.getX(), player.getY(), GROUND_Y), 32, 78);
            g2.drawString(String.format("VelY: %5.1f | onGnd: %-5b | Lane: %d->%d", player.getVelocityY(), player.isOnGround(), player.getLane(), player.getTargetLane()), 32, 92);
            g2.drawString(String.format("Speed: %4.1f px/s | SpwInt: %4.2fs | SpwTmr: %4.2fs", obstacleSpeed, spawnInterval, spawnTimer), 32, 106);
            g2.drawString(String.format("Score: %d | Dist: %dm | Coins: %d | Lives: %d", score, distance, coinsCollected, lives), 32, 120);
            g2.drawString(String.format("Invinc: %4.2fs | Shake: %4.2fs | FText: %d", invincibilityTimer, screenShakeTimer, floatingTexts.size()), 32, 134);
            g2.drawString(String.format("Active: Obs=%d Coins=%d Lives=%d", obstacles.size(), coins.size(), lifePowerUps.size()), 32, 148);
        }

        private void drawCenteredText(Graphics2D g2, String text, int baseline) {
            int textWidth = g2.getFontMetrics().stringWidth(text);
            g2.drawString(text, (WIDTH - textWidth) / 2, baseline);
        }

        
        private static class Particle {
            double x, y, vx, vy;
            Color color;
            double life, maxLife, size;

            Particle(double x, double y, double vx, double vy, Color color, double size, double life) {
                this.x = x; this.y = y; this.vx = vx; this.vy = vy;
                this.color = color; this.size = size; this.life = life; this.maxLife = life;
            }

            void update(double dt, double scrollSpeed) {
                x += vx * dt;
                y += (vy + scrollSpeed) * dt;
                life -= dt;
            }

            boolean isDead() { return life <= 0; }

            void draw(Graphics2D g2) {
                float alpha = (float) Math.max(0, life / maxLife);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha * 255)));
                int s = (int) (size * alpha);
                g2.fillOval((int) x - s/2, (int) y - s/2, s, s);
            }
        }

        private static class FloatingText {
            private final double x;
            private double y;
            private final String text;
            private final Color color;
            private double lifeTimer;
            private final double maxLife;

            FloatingText(double x, double y, String text, Color color, double duration) {
                this.x = x;
                this.y = y;
                this.text = text;
                this.color = color;
                this.lifeTimer = duration;
                this.maxLife = duration;
            }

            void update(double deltaTime) {
                y -= 38.0 * deltaTime;
                lifeTimer -= deltaTime;
            }

            boolean isDead() {
                return lifeTimer <= 0.0;
            }

            void draw(Graphics2D g2) {
                float alpha = (float) Math.max(0.0, Math.min(1.0, lifeTimer / maxLife));
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha * 255)));
                g2.setFont(new Font("SansSerif", Font.BOLD, 13));
                g2.drawString(text, (int) x, (int) y);
            }
        }

        private static class Player {
            private int lane = 1;
            private double x = LANE_X[lane];
            private double targetX = LANE_X[lane];
            private double y = GROUND_Y;
            private double velocityY = 0.0;
            private boolean onGround = true;

            void moveLeft() {
                lane = Math.max(0, lane - 1);
                targetX = LANE_X[lane];
            }

            void moveRight() {
                lane = Math.min(2, lane + 1);
                targetX = LANE_X[lane];
            }

            void jump() {
                if (onGround) {
                    onGround = false;
                    velocityY = -JUMP_STRENGTH;
                }
            }

            void update(double deltaTime) {
                // Smooth lane movement interpolation (frame-rate independent)
                double lerpSpeed = 16.0;
                x += (targetX - x) * Math.min(1.0, lerpSpeed * deltaTime);
                if (Math.abs(targetX - x) < 0.2) {
                    x = targetX;
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

            void reset() {
                lane = 1;
                x = LANE_X[lane];
                targetX = LANE_X[lane];
                y = GROUND_Y;
                velocityY = 0.0;
                onGround = true;
            }

            double getX() {
                return x;
            }

            double getY() {
                return y;
            }

            double getVelocityY() {
                return velocityY;
            }

            int getLane() {
                return lane;
            }

            int getTargetLane() {
                for (int i = 0; i < 3; i++) {
                    if (Math.abs(targetX - LANE_X[i]) < 1.0) return i;
                }
                return lane;
            }

            boolean isJumping() {
                return !onGround;
            }

            boolean isOnGround() {
                return onGround;
            }

            Rectangle2D.Double getHitbox() {
                // Inset body hitbox to avoid unfair pixel collisions
                double padX = 5.0;
                double padY = 4.0;
                double jumpLeniency = onGround ? 0.0 : 12.0;
                return new Rectangle2D.Double(x + padX, y + padY, PLAYER_WIDTH - padX * 2.0, PLAYER_HEIGHT - padY * 2.0 - jumpLeniency);
            }
        }

        private static class Obstacle {
            private final double x;
            private double y;
            private final int lane;
            private final double width = 44.0;
            private final double height;
            private boolean hit = false;
            private boolean passed = false;

            Obstacle(double startX, double startY, int lane, double obstacleHeight) {
                this.x = startX;
                this.y = startY;
                this.lane = lane;
                this.height = obstacleHeight;
            }

            void update(double deltaTime, double speed) {
                y += speed * deltaTime;
            }

            boolean isOffScreen() {
                return y > ROAD_Y + ROAD_H + 10;
            }

            boolean isHit() {
                return hit;
            }

            void setHit(boolean hit) {
                this.hit = hit;
            }

            boolean isPassed() {
                return passed;
            }

            void setPassed(boolean passed) {
                this.passed = passed;
            }

            double getX() {
                return x;
            }

            int getLane() {
                return lane;
            }

            double getY() {
                return y;
            }

            double getWidth() {
                return width;
            }

            double getHeight() {
                return height;
            }

            Rectangle2D.Double getHitbox() {
                double padX = 3.0;
                double padY = 2.0;
                return new Rectangle2D.Double(x + padX, y + padY, width - padX * 2.0, height - padY * 2.0);
            }
        }

        private static class LifePowerUp {
            private final double x;
            private double y;
            private final int lane;
            private final double size = 22.0;

            LifePowerUp(double startX, double startY, int lane) {
                this.x = startX;
                this.y = startY;
                this.lane = lane;
            }

            void update(double deltaTime, double speed) {
                y += speed * deltaTime;
            }

            boolean isOffScreen() {
                return y > ROAD_Y + ROAD_H + 10;
            }

            double getX() {
                return x;
            }

            int getLane() {
                return lane;
            }

            double getY() {
                return y;
            }

            double getSize() {
                return size;
            }

            Rectangle2D.Double getHitbox() {
                return new Rectangle2D.Double(x + 1.0, y + 1.0, size - 2.0, size - 2.0);
            }
        }

        private static class Coin {
            private final double x;
            private double y;
            private final int lane;
            private final double size = 18.0;

            Coin(double startX, double startY, int lane) {
                this.x = startX;
                this.y = startY;
                this.lane = lane;
            }

            void update(double deltaTime, double speed) {
                y += speed * deltaTime;
            }

            boolean isOffScreen() {
                return y > ROAD_Y + ROAD_H + 10;
            }

            double getX() {
                return x;
            }

            int getLane() {
                return lane;
            }

            double getY() {
                return y;
            }

            double getSize() {
                return size;
            }

            Rectangle2D.Double getHitbox() {
                return new Rectangle2D.Double(x + 1.0, y + 1.0, size - 2.0, size - 2.0);
            }
        }
    }
}
