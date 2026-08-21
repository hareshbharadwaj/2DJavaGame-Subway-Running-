package javadash;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.*;

public class Main extends JFrame {
    public Main() {
        super("JavaDash");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setContentPane(new GamePanel());
        pack();
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }

    private static class GamePanel extends JPanel implements ActionListener {
        private static final int WIDTH = 420;
        private static final int HEIGHT = 760;
        private static final int ROAD_X = 48;
        private static final int ROAD_Y = 144;
        private static final int ROAD_W = 324;
        private static final int ROAD_H = 460;
        private static final int[] LANE_Y = {192, 288, 384};
        private static final int PLAYER_WIDTH = 34;
        private static final int PLAYER_HEIGHT = 44;
        private static final double GRAVITY = 0.72;
        private static final int TARGET_FPS = 60;
        private static final int SPAWN_INTERVAL = 32;
        private static final int ROAD_SPEED = 6;
        private static final int[] LANE_X = {102, 210, 318};

        private final Player player = new Player();
        private final List<Obstacle> obstacles = new ArrayList<>();
        private final List<Coin> coins = new ArrayList<>();
        private final Random random = new Random();
        private final Timer timer = new Timer(1000 / TARGET_FPS, this);

        private BufferedImage backgroundImage;
        private BufferedImage roadImage;
        private BufferedImage playerRun1;
        private BufferedImage playerRun2;
        private BufferedImage playerRun3;
        private BufferedImage playerJump;
        private BufferedImage obstacleCar;
        private BufferedImage obstacleBarrier;
        private BufferedImage coinImage;
        private BufferedImage heartImage;
        private BufferedImage shieldImage;
        private BufferedImage magnetImage;

        private boolean gameOver = false;
        private boolean paused = false;
        private long score = 0;
        private int coinsCollected = 0;
        private int distance = 0;
        private int lives = 3;
        private int spawnTimer = 0;
        private int roadOffset = 0;
        private int animationFrame = 0;

        public GamePanel() {
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setFocusable(true);
            setBackground(new Color(8, 12, 24));
            installKeyBindings();
            loadOrCreateAssets();
            timer.start();
        }

        private void installKeyBindings() {
            InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
            ActionMap actionMap = getActionMap();
            inputMap.put(KeyStroke.getKeyStroke("A"), "moveLeft");
            inputMap.put(KeyStroke.getKeyStroke("LEFT"), "moveLeft");
            inputMap.put(KeyStroke.getKeyStroke("D"), "moveRight");
            inputMap.put(KeyStroke.getKeyStroke("RIGHT"), "moveRight");
            inputMap.put(KeyStroke.getKeyStroke("W"), "jump");
            inputMap.put(KeyStroke.getKeyStroke("UP"), "jump");
            inputMap.put(KeyStroke.getKeyStroke("SPACE"), "jump");
            inputMap.put(KeyStroke.getKeyStroke("P"), "pause");
            inputMap.put(KeyStroke.getKeyStroke("R"), "restart");

            actionMap.put("moveLeft", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!gameOver && !paused) {
                        player.moveLeft();
                    }
                }
            });
            actionMap.put("moveRight", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!gameOver && !paused) {
                        player.moveRight();
                    }
                }
            });
            actionMap.put("jump", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!gameOver && !paused) {
                        player.jump();
                    }
                }
            });
            actionMap.put("pause", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!gameOver) {
                        paused = !paused;
                    }
                }
            });
            actionMap.put("restart", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (gameOver) {
                        resetGame();
                    }
                }
            });
        }

        private void resetGame() {
            obstacles.clear();
            coins.clear();
            player.reset();
            gameOver = false;
            paused = false;
            score = 0;
            coinsCollected = 0;
            distance = 0;
            lives = 3;
            spawnTimer = 0;
            roadOffset = 0;
            animationFrame = 0;
        }

        private void loadOrCreateAssets() {
            File baseDir = new File("assets");
            if (!baseDir.exists()) {
                baseDir.mkdirs();
            }
            createAssetDir("player");
            createAssetDir("obstacles");
            createAssetDir("collectibles");
            createAssetDir("environment");
            createAssetDir("ui");

            try {
                backgroundImage = loadOrCreateImage(new File("assets/environment/background.png"), WIDTH, HEIGHT, new Color(14, 20, 34), new Color(34, 60, 110));
                roadImage = loadOrCreateImage(new File("assets/environment/road.png"), ROAD_W, ROAD_H, new Color(47, 67, 118), new Color(20, 28, 48));
                playerRun1 = loadOrCreateImage(new File("assets/player/run1.png"), 48, 48, new Color(29, 208, 255), new Color(255, 255, 255));
                playerRun2 = loadOrCreateImage(new File("assets/player/run2.png"), 48, 48, new Color(29, 208, 255), new Color(255, 255, 255));
                playerRun3 = loadOrCreateImage(new File("assets/player/run3.png"), 48, 48, new Color(29, 208, 255), new Color(255, 255, 255));
                playerJump = loadOrCreateImage(new File("assets/player/jump.png"), 48, 48, new Color(255, 180, 60), new Color(255, 255, 255));
                obstacleCar = loadOrCreateImage(new File("assets/obstacles/car.png"), 38, 38, new Color(255, 90, 90), new Color(255, 255, 255));
                obstacleBarrier = loadOrCreateImage(new File("assets/obstacles/barrier.png"), 38, 38, new Color(142, 75, 255), new Color(240, 240, 240));
                coinImage = loadOrCreateImage(new File("assets/collectibles/coin.png"), 24, 24, new Color(255, 220, 90), new Color(255, 255, 255));
                heartImage = loadOrCreateImage(new File("assets/ui/heart.png"), 24, 24, new Color(255, 92, 92), new Color(255, 255, 255));
                shieldImage = loadOrCreateImage(new File("assets/ui/shield.png"), 24, 24, new Color(90, 140, 255), new Color(255, 255, 255));
                magnetImage = loadOrCreateImage(new File("assets/ui/magnet.png"), 24, 24, new Color(80, 220, 160), new Color(255, 255, 255));
            } catch (IOException e) {
                throw new RuntimeException("Unable to create assets", e);
            }
        }

        private void createAssetDir(String folder) {
            File dir = new File("assets/" + folder);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }

        private BufferedImage loadOrCreateImage(File file, int width, int height, Color primary, Color accent) throws IOException {
            if (file.exists()) {
                return ImageIO.read(file);
            }
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = image.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(primary);
            g2.fillRoundRect(2, 2, width - 4, height - 4, 10, 10);
            g2.setColor(accent);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(2, 2, width - 4, height - 4, 10, 10);
            g2.dispose();
            ImageIO.write(image, "png", file);
            return image;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!gameOver && !paused) {
                updateGame();
            }
            repaint();
        }

        private void updateGame() {
            roadOffset = (roadOffset + ROAD_SPEED) % 36;
            spawnTimer++;
            if (spawnTimer >= SPAWN_INTERVAL) {
                spawnObstacle();
                if (random.nextInt(100) < 55) {
                    spawnCoin();
                }
                spawnTimer = 0;
            }
            animationFrame = (animationFrame + 1) % 4;
            player.update();
            updateObstacles();
            updateCoins();
            checkCollisions();
            distance += 1;
            score += 1;
        }

        private void updateObstacles() {
            Iterator<Obstacle> iterator = obstacles.iterator();
            while (iterator.hasNext()) {
                Obstacle obstacle = iterator.next();
                obstacle.update();
                if (obstacle.isOffScreen()) {
                    iterator.remove();
                    score += 35;
                }
            }
        }

        private void updateCoins() {
            Iterator<Coin> iterator = coins.iterator();
            while (iterator.hasNext()) {
                Coin coin = iterator.next();
                coin.update();
                if (coin.isOffScreen()) {
                    iterator.remove();
                }
            }
        }

        private void checkCollisions() {
            Rectangle playerBounds = player.getBounds();
            for (Obstacle obstacle : obstacles) {
                if (playerBounds.intersects(obstacle.getBounds())) {
                    gameOver = true;
                    return;
                }
            }
            Iterator<Coin> iterator = coins.iterator();
            while (iterator.hasNext()) {
                Coin coin = iterator.next();
                if (playerBounds.intersects(coin.getBounds())) {
                    coinsCollected++;
                    score += 120;
                    iterator.remove();
                }
            }
        }

        private void spawnObstacle() {
            int lane = random.nextInt(3);
            obstacles.add(new Obstacle(ROAD_Y - 48, lane));
        }

        private void spawnCoin() {
            int lane = random.nextInt(3);
            coins.add(new Coin(ROAD_Y - 42, lane));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawBackground(g2);
            drawPhoneFrame(g2);
            drawRoad(g2);
            drawSidePanels(g2);
            drawHeader(g2);
            drawPlayer(g2);
            drawObstacles(g2);
            drawCoins(g2);
            drawFooter(g2);
            drawMobileControls(g2);
            if (gameOver) {
                drawGameOverOverlay(g2);
            } else if (paused) {
                drawPausedOverlay(g2);
            }
            g2.dispose();
        }

        private void drawBackground(Graphics2D g2) {
            g2.setColor(new Color(12, 16, 30));
            g2.fillRect(0, 0, WIDTH, HEIGHT);
            if (backgroundImage != null) {
                g2.drawImage(backgroundImage, 0, 0, WIDTH, HEIGHT, null);
            }
        }

        private void drawPhoneFrame(Graphics2D g2) {
            g2.setColor(new Color(22, 28, 48));
            g2.fillRoundRect(18, 18, WIDTH - 36, HEIGHT - 36, 44, 44);
            g2.setColor(new Color(90, 102, 154));
            g2.setStroke(new BasicStroke(4f));
            g2.drawRoundRect(18, 18, WIDTH - 36, HEIGHT - 36, 44, 44);
            g2.setColor(new Color(255, 255, 255, 120));
            g2.fillOval(WIDTH / 2 - 28, 30, 56, 8);
            g2.fillOval(WIDTH / 2 - 10, HEIGHT - 30, 20, 8);
        }

        private void drawRoad(Graphics2D g2) {
            RoundRectangle2D road = new RoundRectangle2D.Double(ROAD_X, ROAD_Y, ROAD_W, ROAD_H, 24, 24);
            if (roadImage != null) {
                g2.drawImage(roadImage, ROAD_X, ROAD_Y, ROAD_W, ROAD_H, null);
            } else {
                g2.setColor(new Color(40, 60, 108));
                g2.fill(road);
            }
            g2.setColor(new Color(255, 255, 255, 48));
            g2.setStroke(new BasicStroke(2f));
            g2.draw(road);
            int laneWidth = ROAD_W / 3;
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{12, 12}, roadOffset));
            // draw vertical lane separators using lane X positions
            for (int i = 1; i <= 2; i++) {
                int lx = ROAD_X + i * laneWidth;
                g2.drawLine(lx, ROAD_Y + 18, lx, ROAD_Y + ROAD_H - 18);
            }
            g2.setColor(new Color(255, 255, 255, 20));
            for (int lane = 0; lane < 3; lane++) {
                int x = LANE_X[lane] - 36;
                g2.fillRect(x, ROAD_Y + 12, 72, ROAD_H - 24);
            }
        }

        private void drawSidePanels(Graphics2D g2) {
            g2.setColor(new Color(30, 40, 70, 220));
            g2.fillRoundRect(24, 80, 120, 258, 18, 18);
            g2.fillRoundRect(WIDTH - 144, 80, 120, 258, 18, 18);
            g2.setColor(new Color(88, 104, 158));
            g2.drawRoundRect(24, 80, 120, 258, 18, 18);
            g2.drawRoundRect(WIDTH - 144, 80, 120, 258, 18, 18);
            drawStatusPanel(g2);
            drawMissionPanel(g2);
        }

        private void drawStatusPanel(Graphics2D g2) {
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            g2.drawString("STATUS", 40, 106);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.drawString("Lives", 40, 138);
            g2.drawString("Score", 40, 182);
            g2.drawString("Distance", 40, 226);
            g2.drawString("Coins", 40, 270);
            if (heartImage != null) {
                g2.drawImage(heartImage, 36, 282, 16, 16, null);
            }
            g2.setColor(new Color(255, 220, 100));
            g2.drawString(lives + " / 3", 60, 138);
            g2.drawString(String.format("%06d", score), 60, 182);
            g2.drawString(distance + "m", 60, 226);
            g2.drawString(String.valueOf(coinsCollected), 60, 270);
        }

        private void drawMissionPanel(Graphics2D g2) {
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            g2.drawString("MISSION", WIDTH - 132, 106);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.drawString("Collect 12 coins", WIDTH - 132, 132);
            g2.drawString(coinsCollected + " / 12", WIDTH - 132, 156);
            g2.setColor(new Color(78, 218, 255));
            g2.fillRect(WIDTH - 132, 168, Math.min(88, coinsCollected * 7), 8);
            if (shieldImage != null) {
                g2.drawImage(shieldImage, WIDTH - 132, 200, 16, 16, null);
            }
            if (magnetImage != null) {
                g2.drawImage(magnetImage, WIDTH - 132, 232, 16, 16, null);
            }
            g2.setColor(Color.WHITE);
            g2.drawString("Lane", WIDTH - 132, 220);
            g2.drawString("1   2   3", WIDTH - 132, 244);
            g2.setColor(new Color(78, 218, 255));
            g2.fillOval(WIDTH - 130, 248 + player.getLane() * 8, 8, 8);
        }

        private void drawHeader(Graphics2D g2) {
            g2.setColor(new Color(18, 24, 48, 230));
            g2.fillRoundRect(28, 18, WIDTH - 56, 28, 14, 14);
            g2.setColor(new Color(122, 208, 255));
            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2.drawString("JavaDash", 40, 36);
            g2.drawString("Score", 180, 36);
            g2.drawString(String.format("%06d", score), 230, 36);
            g2.drawString("Coins", 360, 36);
            g2.drawString(String.format("%02d", coinsCollected), 420, 36);
            g2.drawString("Lives", 500, 36);
            g2.drawString(lives + " / 3", 548, 36);
        }

        private void drawPlayer(Graphics2D g2) {
            int drawX = player.getX();
            int drawY = player.getY();
            BufferedImage frame = resolvePlayerFrame();
            if (frame != null) {
                g2.drawImage(frame, drawX - (PLAYER_WIDTH/2), drawY - PLAYER_HEIGHT, PLAYER_WIDTH + 14, PLAYER_HEIGHT + 14, null);
            } else {
                g2.setColor(new Color(48, 214, 255));
                g2.fillRoundRect(drawX - PLAYER_WIDTH/2, drawY - PLAYER_HEIGHT, PLAYER_WIDTH, PLAYER_HEIGHT, 12, 12);
            }
        }

        private BufferedImage resolvePlayerFrame() {
            if (player.isJumping()) {
                return playerJump;
            }
            int index = animationFrame % 3;
            return switch (index) {
                case 0 -> playerRun1;
                case 1 -> playerRun2;
                default -> playerRun3;
            };
        }

        private void drawObstacles(Graphics2D g2) {
            for (Obstacle obstacle : obstacles) {
                int ox = obstacle.getX();
                int oy = obstacle.getY();
                if (obstacle.getType() == 0 && obstacleCar != null) {
                    g2.drawImage(obstacleCar, ox, oy, obstacle.getWidth(), obstacle.getHeight(), null);
                } else if (obstacleBarrier != null) {
                    g2.drawImage(obstacleBarrier, ox, oy, obstacle.getWidth(), obstacle.getHeight(), null);
                } else {
                    g2.setColor(new Color(255, 90, 90));
                    g2.fillRoundRect(ox, oy, obstacle.getWidth(), obstacle.getHeight(), 8, 8);
                }
            }
        }

        private void drawCoins(Graphics2D g2) {
            for (Coin coin : coins) {
                int cx = coin.getX();
                int cy = coin.getY();
                if (coinImage != null) {
                    g2.drawImage(coinImage, cx, cy, 24, 24, null);
                } else {
                    g2.setColor(new Color(255, 220, 90));
                    g2.fillOval(cx, cy, 18, 18);
                }
            }
        }

        private void drawFooter(Graphics2D g2) {
            g2.setColor(new Color(255, 255, 255, 70));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2.drawString("WASD / Arrows: move | Space: jump | P: pause | R: restart", 32, HEIGHT - 18);
        }

        private void drawMobileControls(Graphics2D g2) {
            int buttonY = HEIGHT - 90;
            int buttonSize = 54;
            drawTouchButton(g2, 52, buttonY, buttonSize, "<");
            drawTouchButton(g2, 124, buttonY, buttonSize, ">");
            drawTouchButton(g2, 314, buttonY, buttonSize, "^");
        }

        private void drawTouchButton(Graphics2D g2, int x, int y, int size, String label) {
            g2.setColor(new Color(26, 36, 58, 240));
            g2.fillRoundRect(x, y, size, size, 16, 16);
            g2.setColor(new Color(98, 218, 255));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(x, y, size, size, 16, 16);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 22));
            g2.drawString(label, x + 14, y + 31);
        }

        private void drawGameOverOverlay(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(0, 0, WIDTH, HEIGHT);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 30));
            g2.drawString("GAME OVER", 120, 180);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
            g2.drawString("Press R to restart", 128, 212);
        }

        private void drawPausedOverlay(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRect(0, 0, WIDTH, HEIGHT);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 28));
            g2.drawString("PAUSED", 150, 190);
        }

        private class Player {
            private int lane = 1;
            private int x = LANE_X[lane];
            private final int baseY = ROAD_Y + ROAD_H - 20; // feet position
            private int y = baseY;
            private double velocityY = 0;
            private boolean jumping = false;

            void moveLeft() {
                if (!jumping) {
                    lane = Math.max(0, lane - 1);
                    x = LANE_X[lane];
                }
            }

            void moveRight() {
                if (!jumping) {
                    lane = Math.min(2, lane + 1);
                    x = LANE_X[lane];
                }
            }

            void jump() {
                if (!jumping) {
                    jumping = true;
                    velocityY = -12.0;
                }
            }

            void update() {
                if (jumping) {
                    y += velocityY;
                    velocityY += GRAVITY;
                    if (y >= baseY) {
                        y = baseY;
                        velocityY = 0;
                        jumping = false;
                    }
                }
            }

            void reset() {
                lane = 1;
                x = LANE_X[lane];
                y = baseY;
                velocityY = 0;
                jumping = false;
            }

            int getY() {
                return y;
            }

            int getX() {
                return x;
            }

            int getLane() {
                return lane;
            }

            boolean isJumping() {
                return jumping;
            }

            Rectangle getBounds() {
                return new Rectangle(x - PLAYER_WIDTH/2, y - PLAYER_HEIGHT, PLAYER_WIDTH, PLAYER_HEIGHT);
            }
        }

        private class Obstacle {
            private int x;
            private int y;
            private final int lane;
            private final int width = 36;
            private final int height = 36;
            private final int type = random.nextInt(2);

            Obstacle(int startY, int lane) {
                this.y = startY;
                this.lane = lane;
                this.x = LANE_X[lane] - width/2;
            }

            void update() {
                y += ROAD_SPEED;
            }

            boolean isOffScreen() {
                return y > ROAD_Y + ROAD_H;
            }

            int getX() {
                return x;
            }

            int getY() {
                return y;
            }

            int getLane() {
                return lane;
            }

            int getWidth() {
                return width;
            }

            int getHeight() {
                return height;
            }

            int getType() {
                return type;
            }

            Rectangle getBounds() {
                return new Rectangle(x, y - height, width, height);
            }
        }

        private class Coin {
            private int x;
            private int y;
            private final int lane;

            Coin(int startY, int lane) {
                this.y = startY;
                this.lane = lane;
                this.x = LANE_X[lane] - 12;
            }

            void update() {
                y += ROAD_SPEED;
            }

            boolean isOffScreen() {
                return y > ROAD_Y + ROAD_H;
            }

            int getX() {
                return x;
            }

            int getY() {
                return y;
            }

            int getLane() {
                return lane;
            }

            Rectangle getBounds() {
                return new Rectangle(x, y - 12, 24, 24);
            }
        }
    }
}
