import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.io.File;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.EnumMap;

public class SimpleRunnerGame extends JFrame {
    public static CardLayout cardLayout = new CardLayout();
    public static JPanel mainPanel = new JPanel(cardLayout);
    public static GamePanel gamePanel;
    public static HomePanel homePanel;
    public static LeaderboardPanel leaderboardPanel;
    public static String currentUsername = SessionManager.loadSession();

    public static void saveUsername(String name) {
        currentUsername = name;
        SessionManager.saveSession(name);
    }

    public static LoginPanel loginPanel;

    public SimpleRunnerGame() {
        super("JavaDash");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        
        DbManager.initializeTables();
        
        gamePanel = new GamePanel();
        homePanel = new HomePanel();
        leaderboardPanel = new LeaderboardPanel();
        loginPanel = new LoginPanel();
        
        mainPanel.add(loginPanel, "LOGIN");
        mainPanel.add(homePanel, "HOME");
        mainPanel.add(gamePanel, "GAME");
        mainPanel.add(leaderboardPanel, "LEADERBOARD");
        
        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(null);
    }

    public static void showScreen(String name) {
        if (name.equals("HOME")) homePanel.refresh();
        if (name.equals("LEADERBOARD")) leaderboardPanel.refresh();
        if (name.equals("GAME")) {
            gamePanel.resetGame();
            gamePanel.requestFocusInWindow();
        }
        cardLayout.show(mainPanel, name);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SimpleRunnerGame game = new SimpleRunnerGame();
            game.setVisible(true);
            if (currentUsername == null || currentUsername.isEmpty()) {
                showScreen("LOGIN");
            } else {
                showScreen("HOME");
            }
        });
    }

    private static class LoginPanel extends JPanel {
        private JTextField nameField;
        private JPasswordField passField;
        private JLabel msgLabel;

        public LoginPanel() {
            setPreferredSize(new Dimension(420, 760));
            setBackground(new Color(12, 18, 30));
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            add(Box.createVerticalStrut(150));

            JLabel title = new JLabel("JavaDash Login");
            title.setFont(new Font("SansSerif", Font.BOLD, 40));
            title.setForeground(new Color(52, 208, 255));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(title);

            add(Box.createVerticalStrut(50));

            JLabel userLbl = new JLabel("Username:");
            userLbl.setForeground(Color.WHITE);
            userLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(userLbl);

            nameField = new JTextField(15);
            nameField.setMaximumSize(new Dimension(200, 30));
            nameField.setHorizontalAlignment(JTextField.CENTER);
            add(nameField);

            add(Box.createVerticalStrut(20));

            JLabel passLbl = new JLabel("Password:");
            passLbl.setForeground(Color.WHITE);
            passLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(passLbl);

            passField = new JPasswordField(15);
            passField.setMaximumSize(new Dimension(200, 30));
            passField.setHorizontalAlignment(JTextField.CENTER);
            add(passField);

            add(Box.createVerticalStrut(10));

            msgLabel = new JLabel(" ");
            msgLabel.setForeground(new Color(255, 100, 100));
            msgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(msgLabel);

            add(Box.createVerticalStrut(20));

            JPanel btnPanel = new JPanel();
            btnPanel.setOpaque(false);
            btnPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 0));

            JButton loginBtn = new JButton("LOGIN");
            loginBtn.addActionListener(e -> attemptLogin());

            JButton createBtn = new JButton("CREATE");
            createBtn.addActionListener(e -> attemptCreate());

            btnPanel.add(loginBtn);
            btnPanel.add(createBtn);
            btnPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(btnPanel);
        }

        private void attemptLogin() {
            String u = nameField.getText().trim();
            String p = new String(passField.getPassword()).trim();
            if (u.isEmpty() || p.isEmpty()) {
                msgLabel.setText("Please enter username and password.");
                return;
            }
            if (DbManager.authenticatePlayer(u, p)) {
                saveUsername(u);
                showScreen("HOME");
            } else {
                msgLabel.setText("Invalid username or password.");
            }
        }

        private void attemptCreate() {
            String u = nameField.getText().trim();
            String p = new String(passField.getPassword()).trim();
            if (u.isEmpty() || p.isEmpty()) {
                msgLabel.setText("Please enter username and password.");
                return;
            }
            if (DbManager.registerPlayer(u, p)) {
                saveUsername(u);
                showScreen("HOME");
            } else {
                msgLabel.setText("Username already exists or DB offline.");
            }
        }
    }

    private static class HomePanel extends JPanel {
        private JLabel scoreLabel;
        private JLabel coinsLabel;
        private JLabel gamesLabel;
        private JLabel titleLabel;
        private Image bgImage;
        private Image playerImage;
        private Image logoImage;

        public HomePanel() {
            setPreferredSize(new Dimension(420, 760));
            setLayout(null);
            
            try {
                File menuBg = new File("assets/ui/home_bg.png");
                bgImage = ImageIO.read(menuBg.exists() ? menuBg : new File("assets/environment/background.png"));
                playerImage = ImageIO.read(new File("assets/player/player_run (2).png"));
                File logo = new File("assets/ui/logo.png");
                if (logo.exists()) logoImage = ImageIO.read(logo);
            } catch (Exception e) {}
            
            titleLabel = new JLabel(logoImage != null ? "" : "JavaDash");
            if (logoImage != null) {
                // Logo art carries the branding; the label becomes the player greeting.
                titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
                titleLabel.setForeground(new Color(150, 225, 255));
                titleLabel.setBounds(30, 130, 360, 34);
            } else {
                titleLabel.setFont(new Font("SansSerif", Font.BOLD, 48));
                titleLabel.setForeground(Color.WHITE);
                titleLabel.setBounds(100, 50, 250, 60);
            }
            add(titleLabel);

            JPanel statsPanel = new JPanel();
            statsPanel.setLayout(new GridLayout(3, 1, 0, 10));
            statsPanel.setOpaque(false);
            statsPanel.setBounds(200, 150, 200, 150);

            scoreLabel = new JLabel("Score: 0");
            scoreLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
            scoreLabel.setForeground(Color.WHITE);
            scoreLabel.setOpaque(true);
            scoreLabel.setBackground(new Color(60, 120, 216, 200));
            scoreLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

            coinsLabel = new JLabel("Coins: 0");
            coinsLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
            coinsLabel.setForeground(Color.WHITE);
            coinsLabel.setOpaque(true);
            coinsLabel.setBackground(new Color(60, 120, 216, 200));
            coinsLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

            gamesLabel = new JLabel("Games: 0");
            gamesLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
            gamesLabel.setForeground(Color.WHITE);
            gamesLabel.setOpaque(true);
            gamesLabel.setBackground(new Color(60, 120, 216, 200));
            gamesLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

            statsPanel.add(scoreLabel);
            statsPanel.add(coinsLabel);
            statsPanel.add(gamesLabel);
            add(statsPanel);

            JButton startBtn = new JButton("PLAY");
            startBtn.setFont(new Font("SansSerif", Font.BOLD, 36));
            startBtn.setBackground(new Color(112, 196, 255));
            startBtn.setForeground(Color.WHITE);
            startBtn.setBounds(90, 600, 240, 70);
            startBtn.setFocusPainted(false);
            startBtn.addActionListener(e -> showScreen("GAME"));
            add(startBtn);

            JButton lbBtn = new JButton("Rankings");
            lbBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
            lbBtn.setBounds(310, 700, 100, 40);
            lbBtn.addActionListener(e -> showScreen("LEADERBOARD"));
            add(lbBtn);

            JButton switchBtn = new JButton("Logout");
            switchBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
            switchBtn.setBounds(10, 700, 100, 40);
            switchBtn.addActionListener(e -> {
                SessionManager.clearSession();
                currentUsername = null;
                showScreen("LOGIN");
            });
            add(switchBtn);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            if (bgImage != null) {
                g2.drawImage(bgImage, 0, 0, 420, 760, null);
            } else {
                g2.setColor(new Color(40, 160, 220));
                g2.fillRect(0, 0, 420, 760);
            }
            
            if (playerImage != null) {
                g2.drawImage(playerImage, 40, 250, 140, 180, null);
            }

            if (logoImage != null) {
                int logoW = 300;
                int logoH = logoImage.getHeight(null) * logoW / Math.max(1, logoImage.getWidth(null));
                g2.drawImage(logoImage, (420 - logoW) / 2, 40, logoW, logoH, null);
            }
        }

        public void refresh() {
            boolean dbUp = DbManager.test();
            if (dbUp && currentUsername != null) {
                PlayerInfo p = DbManager.getPlayer(currentUsername);
                if (p != null) {
                    scoreLabel.setText("Best Score: " + p.getBestScore());
                    coinsLabel.setText("Coins: " + p.getBestCoins());
                    gamesLabel.setText("Games: " + p.getGamesPlayed());
                    titleLabel.setText("Hi, " + currentUsername + "!");
                }
            } else {
                scoreLabel.setText("Best Score: N/A");
                coinsLabel.setText("Coins: N/A");
                gamesLabel.setText("Offline");
                titleLabel.setText("Hi, " + currentUsername + "!");
            }
        }
    }

    private static class LeaderboardPanel extends JPanel {
        private JTable table;
        private String[] cols = {"Rank", "Player", "Best Score", "Coins", "Games"};
        private String[][] data = new String[0][5];
        
        public LeaderboardPanel() {
            setPreferredSize(new Dimension(420, 760));
            setBackground(new Color(12, 18, 30));
            setLayout(new BorderLayout());
            
            JLabel title = new JLabel("LEADERBOARD", SwingConstants.CENTER);
            title.setFont(new Font("SansSerif", Font.BOLD, 28));
            title.setForeground(new Color(52, 208, 255));
            title.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
            add(title, BorderLayout.NORTH);
            
            table = new JTable(data, cols);
            table.setFillsViewportHeight(true);
            JScrollPane scroll = new JScrollPane(table);
            add(scroll, BorderLayout.CENTER);
            
            JPanel btnPanel = new JPanel();
            btnPanel.setBackground(new Color(12, 18, 30));
            JButton backBtn = new JButton("BACK");
            backBtn.addActionListener(e -> showScreen("HOME"));
            JButton refreshBtn = new JButton("REFRESH");
            refreshBtn.addActionListener(e -> refresh());
            btnPanel.add(backBtn);
            btnPanel.add(refreshBtn);
            add(btnPanel, BorderLayout.SOUTH);
        }
        
        public void refresh() {
            if (!DbManager.test()) return;
            List<PlayerInfo> top = DbManager.getLeaderboard(20);
            data = new String[top.size()][5];
            int highlightRow = -1;
            for (int i = 0; i < top.size(); i++) {
                PlayerInfo p = top.get(i);
                data[i][0] = String.valueOf(i + 1);
                data[i][1] = p.getUsername();
                data[i][2] = String.valueOf(p.getBestScore());
                data[i][3] = String.valueOf(p.getBestCoins());
                data[i][4] = String.valueOf(p.getGamesPlayed());
                if (p.getUsername().equals(currentUsername)) highlightRow = i;
            }
            table.setModel(new javax.swing.table.DefaultTableModel(data, cols));
            if (highlightRow != -1) {
                table.setRowSelectionInterval(highlightRow, highlightRow);
            }
        }
    }

    private static class GamePanel extends JPanel implements ActionListener {
        private enum GameState {
            START_MENU,
            PLAYING,
            PAUSED,
            GAME_OVER
        }

        public enum Environment { CITY, FOREST, DESERT, NIGHT_HIGHWAY }

        private static final double ENV_1 = 100.0;
        private static final double ENV_2 = 200.0;
        private static final double ENV_3 = 300.0;
        private static final boolean LOOP_ENVIRONMENTS = false;

        private static Environment environmentFor(double distance) {
            int segment = Math.max(0, (int) (distance / 200.0));
            Environment[] envs = Environment.values();
            // A pseudo-random sequence that guarantees the environment changes every 200m without repeating consecutively
            return envs[(segment * 3 + 1) % envs.length];
        }

        private static Image playerRunImage;
        private static Image playerRunImage2;
        private static Image playerLeftImage;
        private static Image playerRightImage;
        private static Image playerJumpImage;
        private static Image playerSlideImage;
        
        private static Map<Environment, BufferedImage> sideLeft = new EnumMap<>(Environment.class);
        private static Map<Environment, BufferedImage> sideRight = new EnumMap<>(Environment.class);
        private static BufferedImage barricadeLeft;
        private static BufferedImage barricadeRight;
        private static BufferedImage backgroundLeftFallback;
        private static BufferedImage backgroundRightFallback;
        private static Image roadImage;
        // Obstacle type ids. 0-8 are static hazards that sit on the road and scroll
        // toward the player; 9-11 are oncoming traffic that DRIVES at the player.
        private static final int OBS_TRUCK_LONG = 8;
        private static final int OBS_BIKE_ONCOMING = 9;
        private static final int OBS_AUTO_ONCOMING = 10;
        private static final int OBS_CAR_ONCOMING = 11;
        private static final int OBSTACLE_TYPE_COUNT = 12;
        private static final int STATIC_OBSTACLE_COUNT = 9; // types 0-8

        /**
         * Extra closing speed (px/s) for vehicles driving toward the player, added on
         * top of the world scroll. Kept modest so the reaction window stays fair:
         * at top speed the quickest bike still gives roughly a second to change lane.
         */
        private static double oncomingClosingSpeed(int type) {
            switch (type) {
                case OBS_BIKE_ONCOMING: return 200.0; // bikes weave in fastest
                case OBS_AUTO_ONCOMING: return 130.0; // autos putter along
                case OBS_CAR_ONCOMING:  return 170.0;
                default: return 0.0;
            }
        }

        private static boolean isOncoming(int type) {
            return type >= OBS_BIKE_ONCOMING && type <= OBS_CAR_ONCOMING;
        }

        private static Image[] obstacleImages = new Image[OBSTACLE_TYPE_COUNT];
        private static Image[] coinImages = new Image[8];
        private static Image heartImage;
        private static Image shieldImage;
        private static Image magnetImage;
        private static Image boostImage;
        private static Image doubleScoreImage;
        private static Image jetpackImage;
        private static Image slowMoImage;
        private static Image logoImage;

        private static Image safeLoadImage(String path) {
            try {
                File f = new File(path);
                if (f.exists()) return ImageIO.read(f);
            } catch (Exception e) {}
            return null;
        }

        private static BufferedImage safeLoadBufferedImage(String path) {
            try {
                File f = new File(path);
                if (f.exists()) return ImageIO.read(f);
                System.out.println("Warning: Missing asset " + path);
            } catch (Exception e) {
                System.out.println("Error loading asset " + path + ": " + e.getMessage());
            }
            return null;
        }

        static {
            playerRunImage = safeLoadImage("assets/player/player_run (2).png");
            playerRunImage2 = safeLoadImage("assets/player/player_run (1)_processed.png");
            playerLeftImage = safeLoadImage("assets/player/player_left (1).png");
            playerRightImage = safeLoadImage("assets/player/player_right (1).png");
            playerJumpImage = safeLoadImage("assets/player/player_jump (1).png");
            playerSlideImage = safeLoadImage("assets/player/player_slide.png");
            
            // Removed backgroundLeft/RightFallback loads to prevent missing asset warnings
            backgroundLeftFallback = null;
            backgroundRightFallback = null;

            String[] envNames = {"city", "forest", "desert", "night"};
            Environment[] envs = {Environment.CITY, Environment.FOREST, Environment.DESERT, Environment.NIGHT_HIGHWAY};
            
            for (int i = 0; i < envNames.length; i++) {
                BufferedImage l = safeLoadBufferedImage("assets/environment/" + envNames[i] + "_side_raw left.png");
                BufferedImage r = safeLoadBufferedImage("assets/environment/" + envNames[i] + "_side_raw right.png");
                if (l != null) sideLeft.put(envs[i], l);
                if (r != null) sideRight.put(envs[i], r);
            }
            
            barricadeLeft = safeLoadBufferedImage("assets/environment/barricade_raw left.png");
            barricadeRight = safeLoadBufferedImage("assets/environment/barricade_raw right.png");
            roadImage = safeLoadImage("assets/environment/road (1).png");
            
            // Slots 3 and 7 use dedicated art instead of duplicating car_blue/barrier.
            // Slot 8 is the long articulated lorry; 9-11 are the oncoming vehicles.
            String[] obsNames = {"barrel (1)", "barrier (1)", "car_blue (1)", "car_green", "car_red (1)",
                                 "car_taxi (1)", "cone (1)", "pothole", "truck_long",
                                 "bike_oncoming", "auto_oncoming", "car_oncoming"};
            for(int i = 0; i < obsNames.length; i++) {
                obstacleImages[i] = safeLoadImage("assets/obstacles/" + obsNames[i] + ".png");
                if (obstacleImages[i] == null) {
                    obstacleImages[i] = safeLoadImage("assets/obstacles/barrier (1).png");
                }
            }
            
            for(int i = 0; i < 8; i++) {
                coinImages[i] = safeLoadImage("assets/collectibles/coin" + (i+1) + ".png");
            }
            heartImage = safeLoadImage("assets/ui/heart (1).png");
            shieldImage = safeLoadImage("assets/ui/shield (1).png");
            magnetImage = safeLoadImage("assets/ui/magnet (1).png");
            boostImage = safeLoadImage("assets/ui/boost.png");
            doubleScoreImage = safeLoadImage("assets/ui/x2.png");
            jetpackImage = safeLoadImage("assets/ui/jetpack.png");
            slowMoImage = safeLoadImage("assets/ui/slowmo.png");
            logoImage = safeLoadImage("assets/ui/logo.png");
        }

        private static final int WIDTH = 420;
        private static final int HEIGHT = 760;
        private static final int ROAD_Y = 24;
        private static final int ROAD_H = 712; // HEIGHT - 48

        // --- layout (all drawing and spawning must use these; delete every other X constant) ---
        private static int panelW, panelH;
        private static int SIDE_W;        // width of each side strip
        private static int ROAD_LEFT;     // road left edge
        private static int ROAD_RIGHT;    // road right edge
        private static int LANE_W;        // width of one lane
        private static int[] laneCenterX = new int[3];

        private void computeLayout() {
            int w = getWidth();
            int h = getHeight();
            if (w == 0 || h == 0) return;
            panelW = w;
            panelH = h;
            
            // Balanced layout for 420px width
            SIDE_W = 90; // Background area + barricade
            ROAD_LEFT = SIDE_W; 
            ROAD_RIGHT = panelW - SIDE_W;         // road is CENTERED
            LANE_W = (ROAD_RIGHT - ROAD_LEFT) / 3;
            
            for (int i = 0; i < 3; i++) {
                laneCenterX[i] = ROAD_LEFT + LANE_W * i + LANE_W / 2;
            }
        }
        private static final int PLAYER_WIDTH = 44;
        private static final int PLAYER_HEIGHT = 56;
        private static final double GROUND_Y = 24 + 712 - 92.0;
        private static final double GRAVITY = 2200.0; // px/s^2
        private static final double JUMP_STRENGTH = 700.0; // px/s, yields ~111px apex & ~0.63s jump
        private static final double MAX_JUMP_APEX = (JUMP_STRENGTH * JUMP_STRENGTH) / (2.0 * GRAVITY);
        private static final double SLIDE_DURATION = 0.6;      // seconds spent crouched
        private static final double SLIDE_HEIGHT_RATIO = 0.5;  // hitbox height while sliding
        private static final double JETPACK_HOVER_HEIGHT = 150.0; // px above the road while flying
        private static final double INITIAL_SPEED = 300.0; // px/s
        private static final double MAX_SPEED = 560.0; // px/s
        private static final int TARGET_FPS = 60;
        private static final int STARTING_LIVES = 1;
        private static final int MAX_LIVES = 3;

        private GameState gameState = GameState.START_MENU;
        private final Player player = new Player();
        private final List<Obstacle> obstacles = new ArrayList<>();
        private final List<Coin> coins = new ArrayList<>();
        private final List<LifePowerUp> lifePowerUps = new ArrayList<>();
        private final List<PowerUp> powerUps = new ArrayList<>();
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
        
        private double shieldTimer = 0.0;
        private double magnetTimer = 0.0;
        private double boostTimer = 0.0;
        private double doubleScoreTimer = 0.0;
        private double jetpackTimer = 0.0;
        private double slowMoTimer = 0.0;

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
        private boolean downPressed = false;
        
        private long runStartTime = 0;
        private String gameOverMessage = "";

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
            SoundManager.loadAll();
            SoundManager.loop("music");
            timer.start();
        }

        // Unified input handling for touch / mouse
        private void handleTouch(int x, int y) {
            if (gameState == GameState.GAME_OVER) {
                // Check PLAY AGAIN button
                if (x >= 110 && x <= 310 && y >= 450 && y <= 490) {
                    SoundManager.play("click");
                    resetGame();
                }
                // Check HOME button
                if (x >= 110 && x <= 310 && y >= 510 && y <= 550) {
                    SoundManager.play("click");
                    showScreen("HOME");
                }
                return;
            }
            int buttonY = HEIGHT - 86;
            int buttonSize = 46;
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
                SoundManager.play("click");
                resetGame();
                return;
            }
            if (gameState == GameState.PLAYING) {
                player.jump();
                SoundManager.play("jump");
            }
        }

        public void slide() {
            if (gameState == GameState.PLAYING && player.isOnGround() && !player.isSliding()) {
                player.slide();
                SoundManager.play("jump");
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
                SoundManager.play("click");
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
            inputMap.put(KeyStroke.getKeyStroke("pressed S"), "slide");
            inputMap.put(KeyStroke.getKeyStroke("pressed DOWN"), "slide");
            inputMap.put(KeyStroke.getKeyStroke("pressed P"), "pause");
            inputMap.put(KeyStroke.getKeyStroke("pressed R"), "restart");
            inputMap.put(KeyStroke.getKeyStroke("pressed F3"), "toggleDebug");
            inputMap.put(KeyStroke.getKeyStroke("pressed M"), "toggleMute");

            inputMap.put(KeyStroke.getKeyStroke("released A"), "laneLeftRelease");
            inputMap.put(KeyStroke.getKeyStroke("released LEFT"), "laneLeftRelease");
            inputMap.put(KeyStroke.getKeyStroke("released D"), "laneRightRelease");
            inputMap.put(KeyStroke.getKeyStroke("released RIGHT"), "laneRightRelease");
            inputMap.put(KeyStroke.getKeyStroke("released W"), "jumpRelease");
            inputMap.put(KeyStroke.getKeyStroke("released UP"), "jumpRelease");
            inputMap.put(KeyStroke.getKeyStroke("released SPACE"), "jumpRelease");
            inputMap.put(KeyStroke.getKeyStroke("released S"), "slideRelease");
            inputMap.put(KeyStroke.getKeyStroke("released DOWN"), "slideRelease");

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

            actionMap.put("slide", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!downPressed) {
                        slide();
                        downPressed = true;
                    }
                }
            });
            actionMap.put("slideRelease", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    downPressed = false;
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
            actionMap.put("toggleMute", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SoundManager.toggleMute();
                }
            });
        }

        public void resetGame() {
            obstacles.clear();
            coins.clear();
            lifePowerUps.clear();
            powerUps.clear();
            floatingTexts.clear();
            particles.clear();
            player.reset();

            gameState = GameState.PLAYING;
            score = 0;
            coinsCollected = 0;
            distance = 0;
            lives = STARTING_LIVES;
            obstacleSpeed = INITIAL_SPEED;
            spawnTimer = 0.0;
            spawnInterval = 0.85;
            invincibilityTimer = 0.0;
            screenShakeTimer = 0.0;
            shakeIntensity = 0.0;
            roadScroll = 0.0;
            scoreAccumulator = 0.0;
            distanceAccumulator = 0.0;
            shieldTimer = 0.0;
            magnetTimer = 0.0;
            boostTimer = 0.0;
            doubleScoreTimer = 0.0;
            jetpackTimer = 0.0;
            slowMoTimer = 0.0;
            lastSpawnedLane = 1;
            consecutiveLaneCount = 0;
            lastTime = System.nanoTime();
            runStartTime = System.currentTimeMillis();
            gameOverMessage = "";
            SoundManager.loop("music");
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

            computeLayout();
            if (LANE_W <= 0) return;

            if (gameState == GameState.PLAYING) {
                updateGame(deltaTime);
            }
            repaint();
        }

        private void updateGame(double deltaTime) {
            // Difficulty scaling: speed increases progressively based on distance
            obstacleSpeed = Math.min(650.0, INITIAL_SPEED + distance * 0.4);
            if (boostTimer > 0.0) obstacleSpeed *= 1.5;  // speed boost power-up
            if (slowMoTimer > 0.0) obstacleSpeed *= 0.5; // slow-motion power-up
            // Spawn interval decreases smoothly down to a fair floor of 0.38s
            spawnInterval = Math.max(0.38, 0.85 - (distance * 0.0005));

            // Road scroll (delta-time based)
            roadScroll += obstacleSpeed * deltaTime;

            // Invincibility and screen shake timers (delta-time based)
            if (invincibilityTimer > 0.0) {
                invincibilityTimer = Math.max(0.0, invincibilityTimer - deltaTime);
            }
            if (screenShakeTimer > 0.0) {
                screenShakeTimer = Math.max(0.0, screenShakeTimer - deltaTime);
            }
            if (shieldTimer > 0.0) shieldTimer = Math.max(0.0, shieldTimer - deltaTime);
            if (magnetTimer > 0.0) magnetTimer = Math.max(0.0, magnetTimer - deltaTime);
            if (boostTimer > 0.0) boostTimer = Math.max(0.0, boostTimer - deltaTime);
            if (doubleScoreTimer > 0.0) doubleScoreTimer = Math.max(0.0, doubleScoreTimer - deltaTime);
            if (jetpackTimer > 0.0) jetpackTimer = Math.max(0.0, jetpackTimer - deltaTime);
            if (slowMoTimer > 0.0) slowMoTimer = Math.max(0.0, slowMoTimer - deltaTime);

            // Real distance accumulation (time & speed based, not frame-rate dependent)
            distanceAccumulator += (obstacleSpeed * 0.02) * deltaTime;
            distance = (int) distanceAccumulator;

            // Real score accumulation (time based, not frame-rate dependent)
            scoreAccumulator += 15.0 * deltaTime * scoreMultiplier();
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
            player.setFlying(jetpackTimer > 0.0);
            player.update(deltaTime);

            // Jetpack exhaust trail
            if (jetpackTimer > 0.0) {
                for (int i = 0; i < 2; i++) {
                    particles.add(new Particle(player.getX() + 14 + random.nextInt(16),
                        player.getY() + PLAYER_HEIGHT - 4,
                        (random.nextDouble() - 0.5) * 40.0, 90.0 + random.nextDouble() * 70.0,
                        i == 0 ? new Color(255, 190, 70, 220) : new Color(255, 110, 40, 200),
                        6.0 + random.nextDouble() * 4.0, 0.35 + random.nextDouble() * 0.25));
                }
            }
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
            updatePowerUps(deltaTime);
            updateFloatingTexts(deltaTime);
            checkCollisions();
        }

        /** Score multiplier granted by the x2 power-up. */
        private int scoreMultiplier() {
            return doubleScoreTimer > 0.0 ? 2 : 1;
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

            // Ensure obstacle stays strictly inside road boundaries (no longer clamped since laneCenterX is strict)
            double obsX = laneCenterX[lane];
            // Oncoming traffic appears once the run is under way and ramps up with distance.
            int oncomingChance = distance < 150 ? 0 : Math.min(30, 8 + (distance - 150) / 60);
            boolean spawnOncoming = random.nextInt(100) < oncomingChance;

            int obsType;
            if (spawnOncoming) {
                int[] pool = {OBS_BIKE_ONCOMING, OBS_AUTO_ONCOMING, OBS_CAR_ONCOMING};
                obsType = pool[random.nextInt(pool.length)];
            } else {
                obsType = random.nextInt(STATIC_OBSTACLE_COUNT); // 0 to 8
            }

            double w = 44.0;
            double h = obsHeight;
            if (obstacleImages[obsType] != null) {
                int imgW = obstacleImages[obsType].getWidth(null);
                int imgH = obstacleImages[obsType].getHeight(null);
                if (imgW > 0 && imgH > 0) {
                    // Apply a constant scale factor to all obstacles to preserve their relative sizes
                    double scale = 0.35; // Adjust this if they are too big/small globally
                    w = imgW * scale;
                    h = imgH * scale;

                    // Cap width just in case it's still too large for the lane.
                    double maxW = (obsType == OBS_BIKE_ONCOMING) ? 40.0 : 70.0;
                    if (w > maxW) {
                        scale = maxW / imgW;
                        w = maxW;
                        h = imgH * scale;
                    }

                    // The articulated lorry is deliberately long: it stretches well down
                    // the lane, so it takes noticeably longer to drive past the player.
                    if (obsType == OBS_TRUCK_LONG) {
                        h = Math.min(h, 300.0);
                    }
                }
            }
            
            // Adjust spawn Y so the obstacle rests on the spawn line correctly
            double adjustedSpawnY = ROAD_Y - h - 12.0;
            
            // Center the obstacle in the lane properly
            double centeredX = laneCenterX[lane] - (w / 2.0);
            
            obstacles.add(new Obstacle(centeredX, adjustedSpawnY, lane, w, h, obsType));

            // Coin Generation (55% chance)
            if (random.nextInt(100) < 55) {
                // 30% chance: spawn elevated coin directly above obstacle to reward jumping
                if (random.nextInt(100) < 30) {
                    coins.add(new Coin(laneCenterX[lane] - 9.0, spawnY - 52.0, lane));
                } else {
                    // Spawn ground coin in a separate free lane without obstacle overlap
                    int coinLane = (lane + 1 + random.nextInt(2)) % 3;
                    double coinX = laneCenterX[coinLane] - 9.0;
                    coins.add(new Coin(coinX, spawnY - 10.0, coinLane));
                    // 25% chance of a 2-coin sequence
                    if (random.nextInt(100) < 25) {
                        coins.add(new Coin(coinX, spawnY - 38.0, coinLane));
                    }
                }
            }

            // Life Power-Up Generation (10% chance)
            if (lives < MAX_LIVES && random.nextInt(100) < 10) {
                int lifeLane = (lane + 1 + random.nextInt(2)) % 3;
                double lifeX = laneCenterX[lifeLane] - 11.0;
                double lifeY = spawnY - 26.0;
                lifePowerUps.add(new LifePowerUp(lifeX, lifeY, lifeLane));
            }
            
            // Shield or Magnet Generation (10% chance total)
            if (random.nextInt(100) < 10) {
                int puLane = (lane + 1 + random.nextInt(2)) % 3;
                double puX = laneCenterX[puLane] - 11.0;
                double puY = spawnY - 26.0;
                PowerUp.Type[] types = PowerUp.Type.values();
                PowerUp.Type type = types[random.nextInt(types.length)];
                powerUps.add(new PowerUp(puX, puY, puLane, type));
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
                if (magnetTimer > 0.0 && coin.getY() > 0 && coin.getY() < HEIGHT) {
                    double pCenterX = player.getX() + PLAYER_WIDTH / 2.0;
                    double cCenterX = coin.getX() + coin.getSize() / 2.0;
                    double dirX = pCenterX - cCenterX;
                    double dirY = (player.getY() + PLAYER_HEIGHT / 2.0) - (coin.getY() + coin.getSize() / 2.0);
                    double dist = Math.sqrt(dirX * dirX + dirY * dirY);
                    if (dist > 5.0 && dist < 300.0) { // Only pull if reasonably close
                        coin.setX(coin.getX() + (dirX / dist) * 400.0 * deltaTime);
                        coin.setY(coin.getY() + (dirY / dist) * 400.0 * deltaTime);
                    }
                }
                coin.update(deltaTime, magnetTimer > 0.0 ? obstacleSpeed * 0.5 : obstacleSpeed);
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
        private void updatePowerUps(double deltaTime) {
            Iterator<PowerUp> iterator = powerUps.iterator();
            while (iterator.hasNext()) {
                PowerUp pu = iterator.next();
                pu.update(deltaTime, obstacleSpeed);
                if (pu.isOffScreen()) {
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
                        // Types: 0: barrel, 1: barrier, 6: cone, 7: pothole are jumpable.
                        // Vehicles (2,3,4,5,8) and all oncoming traffic (9-11) are not.
                        int type = obstacle.getType();
                        boolean isJumpable = (type == 0 || type == 1 || type == 6 || type == 7);

                        // The jetpack flies clean over everything on the road.
                        if (jetpackTimer > 0.0) {
                            continue;
                        }

                        if (isJumpable) {
                            double heightAboveGround = GROUND_Y - player.getY();
                            if (heightAboveGround > 30.0) {
                                continue; // successfully jumped over
                            }
                        }

                        // Barriers (type 1) sit high enough to duck under.
                        if (type == 1 && player.isSliding()) {
                            continue; // successfully slid under
                        }

                        obstacle.setHit(true);
                        iterator.remove(); // Remove immediately to prevent duplicate collision

                        SoundManager.play("crash");
                        if (shieldTimer > 0.0) {
                            shieldTimer = 0.0; // Consume shield
                            floatingTexts.add(new FloatingText(player.getX(), player.getY() - 12, "SHIELD BLOCK!", new Color(112, 196, 255), 1.0));
                            for (int i=0; i<15; i++) particles.add(new Particle(player.getX()+20, player.getY()+20, (random.nextDouble()-0.5)*150, (random.nextDouble()-0.5)*150, new Color(112,196,255), 8, 0.6));
                        } else if (lives > 0) {
                            lives--;
                            invincibilityTimer = 1.2; // 1.2s invulnerability frames
                            screenShakeTimer = 0.28;
                            shakeIntensity = 5.0;
                            floatingTexts.add(new FloatingText(player.getX() + 4, player.getY() - 12, "-1 LIFE", new Color(255, 90, 90), 0.9));
                        } else {
                            gameState = GameState.GAME_OVER;
                            SoundManager.stop("music");
                            SoundManager.play("gameover");
                            screenShakeTimer = 0.4;
                            shakeIntensity = 8.0;
                            floatingTexts.add(new FloatingText(player.getX() - 4, player.getY() - 12, "CRASH!", new Color(255, 60, 60), 1.2));
                            for (int i=0; i<15; i++) particles.add(new Particle(player.getX()+20, player.getY()+20, (random.nextDouble()-0.5)*200, (random.nextDouble()-0.5)*200, new Color(255,80,80), 8, 0.6));
                            
                            gameOverMessage = "Saving score...";
                            final long finalScore = score;
                            final int finalCoins = coinsCollected;
                            final int finalDistance = distance;
                            final int duration = (int)(System.currentTimeMillis() - runStartTime);
                            PlayerInfo prevInfo = DbManager.getPlayer(currentUsername);
                            final int oldBest = (prevInfo != null) ? prevInfo.getBestScore() : 0;
                            
                            new SwingWorker<Boolean, Void>() {
                                @Override
                                protected Boolean doInBackground() {
                                    return DbManager.submitScore(currentUsername, (int)finalScore, finalCoins, finalDistance, duration);
                                }
                                @Override
                                protected void done() {
                                    try {
                                        boolean success = get();
                                        if (success) {
                                            if (finalScore > oldBest) {
                                                gameOverMessage = "New personal best!";
                                            } else {
                                                gameOverMessage = "Score saved!";
                                            }
                                        } else {
                                            gameOverMessage = "Score not saved (DB offline)";
                                        }
                                    } catch (Exception e) {
                                        gameOverMessage = "Score not saved (Error)";
                                    }
                                }
                            }.execute();
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
                    SoundManager.play("coin");
                    int coinValue = 120 * scoreMultiplier();
                    score += coinValue;
                    floatingTexts.add(new FloatingText(coin.getX() - 2, coin.getY() - 8, "+" + coinValue, new Color(255, 234, 90), 0.7));
                    for (int i=0; i<6; i++) particles.add(new Particle(coin.getX()+8, coin.getY()+8, (random.nextDouble()-0.5)*100, (random.nextDouble()-0.5)*100, new Color(255,234,90), 6, 0.5));
                    coinIterator.remove();
                }
            }

            // Shield/Magnet collection
            Iterator<PowerUp> puIterator = powerUps.iterator();
            while (puIterator.hasNext()) {
                PowerUp pu = puIterator.next();
                if (playerHitbox.intersects(pu.getHitbox())) {
                    SoundManager.play("powerup");
                    if (pu.getType() == PowerUp.Type.SHIELD) {
                        shieldTimer = 10.0;
                        floatingTexts.add(new FloatingText(pu.getX() - 10, pu.getY() - 8, "SHIELD!", new Color(112, 196, 255), 0.8));
                    } else if (pu.getType() == PowerUp.Type.MAGNET) {
                        magnetTimer = 10.0;
                        floatingTexts.add(new FloatingText(pu.getX() - 10, pu.getY() - 8, "MAGNET!", new Color(255, 150, 50), 0.8));
                    } else if (pu.getType() == PowerUp.Type.BOOST) {
                        boostTimer = 6.0;
                        floatingTexts.add(new FloatingText(pu.getX() - 10, pu.getY() - 8, "BOOST!", new Color(255, 190, 60), 0.8));
                    } else if (pu.getType() == PowerUp.Type.DOUBLE_SCORE) {
                        doubleScoreTimer = 10.0;
                        floatingTexts.add(new FloatingText(pu.getX() - 10, pu.getY() - 8, "x2 SCORE!", new Color(190, 140, 255), 0.8));
                    } else if (pu.getType() == PowerUp.Type.JETPACK) {
                        jetpackTimer = 7.0;
                        floatingTexts.add(new FloatingText(pu.getX() - 10, pu.getY() - 8, "JETPACK!", new Color(120, 200, 255), 0.9));
                    } else if (pu.getType() == PowerUp.Type.SLOW_MO) {
                        slowMoTimer = 6.0;
                        floatingTexts.add(new FloatingText(pu.getX() - 10, pu.getY() - 8, "SLOW-MO!", new Color(90, 230, 220), 0.9));
                    }
                    score += 50;
                    for (int i=0; i<8; i++) particles.add(new Particle(pu.getX()+10, pu.getY()+10, (random.nextDouble()-0.5)*120, (random.nextDouble()-0.5)*120, Color.WHITE, 6, 0.5));
                    puIterator.remove();
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
            computeLayout();
            if (LANE_W <= 0) return;
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
            double distanceMeters = distanceAccumulator;
            drawEnvironmentSides(g2, distanceMeters, roadScroll);
            drawRoad(g2);
            drawEnvironmentBarricades(g2, roadScroll);
            drawHeader(g2);
            drawObstacles(g2);
            drawCoins(g2);
            drawLifePowerUps(g2);
            drawPowerUps(g2);
            drawPlayer(g2);
            drawFloatingTexts(g2);
            drawParticles(g2);

            // Slow-motion tints the whole scene a cool cyan
            if (slowMoTimer > 0.0) {
                g2.setColor(new Color(90, 230, 220, 42));
                g2.fillRect(0, 0, panelW, panelH);
            }

            drawFooter(g2);
            // drawMobileControls removed

            if (debugMode) {
                drawDebugOverlay(g2);
            }

            if (gameState == GameState.GAME_OVER) {
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
            g2.fillRect(0, 0, panelW, panelH);
            
            if (logoImage != null) {
                int logoW = Math.min(320, panelW - 60);
                int logoH = logoImage.getHeight(null) * logoW / Math.max(1, logoImage.getWidth(null));
                g2.drawImage(logoImage, (panelW - logoW) / 2, 240 - logoH, logoW, logoH, null);
            } else {
                g2.setColor(new Color(52, 208, 255));
                g2.setFont(new Font("SansSerif", Font.BOLD, 36));
                drawCenteredText(g2, "NEON RUNNER", 240);
            }
            
            double pulse = Math.abs(Math.sin(System.nanoTime() / 3e8));
            g2.setColor(new Color(255, 255, 255, (int)(100 + 155 * pulse)));
            g2.setFont(new Font("SansSerif", Font.BOLD, 18));
            drawCenteredText(g2, "Tap or Press Space to Start", 320);
        }

        
        private void drawBackground(Graphics2D g2) {
            // Sky gradient or dark fill
            GradientPaint sky = new GradientPaint(0, 0, new Color(10, 14, 26), 0, panelH, new Color(20, 26, 46));
            g2.setPaint(sky);
            g2.fillRect(0, 0, panelW, panelH);
        }
        
        private void drawSideStrip(Graphics2D g, BufferedImage tex, int x, int w, int panelH, double scrollY) {
            if (tex == null || w <= 0) return;
            java.awt.Shape oldClip = g.getClip();
            g.clipRect(x, 0, w, panelH);
            int th = Math.max(1, (int) (tex.getHeight() * ((double) w / tex.getWidth())));
            int off = (int) (scrollY % th);
            if (off > 0) off -= th;
            for (int y = off; y < panelH; y += th) {
                g.drawImage(tex, x, y, x + w, y + th, 0, 0, tex.getWidth(), tex.getHeight(), null);
            }
            g.setClip(oldClip);
        }

        private void drawBarricade(Graphics2D g, BufferedImage bar, int x, int panelH, double scrollY) {
            if (bar == null) return;
            int bh = bar.getHeight();
            int off = (int) (scrollY % bh);
            if (off > 0) off -= bh;
            for (int y = off; y < panelH; y += bh) {
                g.drawImage(bar, x, y, null);
            }
        }

        private void drawEnvironmentSides(Graphics2D g2, double distanceMeters, double scrollY) {
            Graphics2D g2Clip = (Graphics2D) g2.create();
            g2Clip.setClip(new RoundRectangle2D.Double(24, 24, panelW - 48, panelH - 48, 50, 50));
            
            int barricadeW = 16;
            int leftEnvX = 24;
            int leftEnvW = Math.max(0, (ROAD_LEFT - barricadeW) - leftEnvX);
            int rightEnvX = ROAD_RIGHT + barricadeW;
            int rightEnvW = Math.max(0, (panelW - 24) - rightEnvX);
            
            int currentSegment = (int) (distanceMeters / 200.0);
            double distIntoSegment = distanceMeters - (currentSegment * 200.0);
            // 1 meter = 50 pixels of scrolling (based on obstacleSpeed * 0.02 update rate)
            int boundaryY = (int) (distIntoSegment * 50.0);
            
            Environment envNew = environmentFor(currentSegment * 200.0);
            Environment envOld = environmentFor((currentSegment - 1) * 200.0);
            
            // Draw OLD environment in the lower part (sliding off the bottom)
            if (boundaryY < panelH) {
                Graphics2D gOld = (Graphics2D) g2Clip.create();
                gOld.clipRect(0, boundaryY, panelW, panelH - boundaryY);
                BufferedImage ltOld = sideLeft.getOrDefault(envOld, backgroundLeftFallback);
                BufferedImage rtOld = sideRight.getOrDefault(envOld, backgroundRightFallback);
                drawSideStrip(gOld, ltOld, leftEnvX, leftEnvW, panelH, scrollY);
                drawSideStrip(gOld, rtOld, rightEnvX, rightEnvW, panelH, scrollY);
                gOld.dispose();
            }
            
            // Draw NEW environment in the upper part (rolling down from the top)
            if (boundaryY > 0) {
                Graphics2D gNew = (Graphics2D) g2Clip.create();
                gNew.clipRect(0, 0, panelW, boundaryY);
                BufferedImage ltNew = sideLeft.getOrDefault(envNew, backgroundLeftFallback);
                BufferedImage rtNew = sideRight.getOrDefault(envNew, backgroundRightFallback);
                drawSideStrip(gNew, ltNew, leftEnvX, leftEnvW, panelH, scrollY);
                drawSideStrip(gNew, rtNew, rightEnvX, rightEnvW, panelH, scrollY);
                gNew.dispose();
            }
            
            g2Clip.dispose();
        }

        private void drawEnvironmentBarricades(Graphics2D g2, double scrollY) {
            Graphics2D g2Clip = (Graphics2D) g2.create();
            g2Clip.setClip(new RoundRectangle2D.Double(24, 24, panelW - 48, panelH - 48, 50, 50));
            
            int barricadeW = 16;
            drawSideStrip(g2Clip, barricadeLeft, ROAD_LEFT - barricadeW, barricadeW, panelH, scrollY);
            drawSideStrip(g2Clip, barricadeRight, ROAD_RIGHT, barricadeW, panelH, scrollY);
            
            g2Clip.dispose();
        }

        private void drawPhoneFrame(Graphics2D g2) {
            g2.setColor(new Color(20, 26, 46));
            g2.fillRoundRect(24, 24, panelW - 48, panelH - 48, 50, 50);

            g2.setColor(new Color(72, 82, 128));
            g2.setStroke(new BasicStroke(4f));
            g2.drawRoundRect(24, 24, panelW - 48, panelH - 48, 50, 50);

            g2.setColor(new Color(255, 255, 255, 120));
            g2.fillOval(panelW / 2 - 28, 34, 56, 8);
            g2.fillOval(panelW / 2 - 10, panelH - 36, 20, 8);
        }

        private void drawRoad(Graphics2D g2) {
            Graphics2D g2Clip = (Graphics2D) g2.create();
            g2Clip.setClip(new RoundRectangle2D.Double(24, 24, panelW - 48, panelH - 48, 50, 50));
            
            // road rectangle
            g2Clip.setColor(new Color(100, 100, 100));
            g2Clip.fillRect(ROAD_LEFT, 0, ROAD_RIGHT - ROAD_LEFT, panelH);
            
            // curbs
            g2Clip.setColor(Color.LIGHT_GRAY);
            g2Clip.fillRect(ROAD_LEFT - 4, 0, 4, panelH);
            g2Clip.fillRect(ROAD_RIGHT, 0, 4, panelH);
            g2Clip.setColor(Color.DARK_GRAY);
            g2Clip.fillRect(ROAD_LEFT, 0, 3, panelH);
            g2Clip.fillRect(ROAD_RIGHT - 3, 0, 3, panelH);

            // lane dashes
            g2Clip.setColor(Color.WHITE);
            for (int i = 1; i <= 2; i++) {
                int x = ROAD_LEFT + LANE_W * i;
                int off = (int) (roadScroll % 55);
                if (off > 0) off -= 55;
                for (int y = off; y < panelH; y += 55)
                    g2Clip.fillRect(x - 3, y, 6, 30);
            }
            g2Clip.dispose();
        }

        private void drawHeader(Graphics2D g2) {
            g2.setColor(new Color(18, 26, 48, 235));
            g2.fillRoundRect(24, 14, panelW - 48, 30, 14, 14);
            g2.setColor(new Color(112, 190, 240));
            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2.drawString("Score " + String.format("%06d", score), 34, 34);
            g2.drawString("Coins " + String.format("%03d", coinsCollected), 140, 34);
            g2.drawString("Dist " + distance + "m", 232, 34);

            // Lives counter with hearts
            g2.setColor(new Color(255, 100, 130));
            if (heartImage != null) {
                g2.drawImage(heartImage, 310, 20, 16, 16, null);
                g2.drawString("x" + lives, 330, 34);
            } else {
                g2.drawString("❤️ " + lives, 326, 34);
            }
        }
        
        private void drawPowerUpTimers(Graphics2D g2) {
            int timerY = 60;
            if (shieldTimer > 0.0) {
                if (shieldImage != null) g2.drawImage(shieldImage, panelW - 130, timerY, 20, 20, null);
                g2.setColor(new Color(112, 196, 255));
                g2.fillRect(panelW - 100, timerY + 6, (int)((shieldTimer/10.0) * 70), 8);
                g2.setColor(Color.WHITE);
                g2.drawRect(panelW - 100, timerY + 6, 70, 8);
                timerY += 30;
            }
            if (magnetTimer > 0.0) {
                if (magnetImage != null) g2.drawImage(magnetImage, panelW - 130, timerY, 20, 20, null);
                g2.setColor(new Color(255, 150, 50));
                g2.fillRect(panelW - 100, timerY + 6, (int)((magnetTimer/10.0) * 70), 8);
                g2.setColor(Color.WHITE);
                g2.drawRect(panelW - 100, timerY + 6, 70, 8);
                timerY += 30;
            }
            if (boostTimer > 0.0) {
                if (boostImage != null) g2.drawImage(boostImage, panelW - 130, timerY, 20, 20, null);
                g2.setColor(new Color(255, 190, 60));
                g2.fillRect(panelW - 100, timerY + 6, (int)((boostTimer/6.0) * 70), 8);
                g2.setColor(Color.WHITE);
                g2.drawRect(panelW - 100, timerY + 6, 70, 8);
                timerY += 30;
            }
            if (doubleScoreTimer > 0.0) {
                if (doubleScoreImage != null) g2.drawImage(doubleScoreImage, panelW - 130, timerY, 20, 20, null);
                g2.setColor(new Color(190, 140, 255));
                g2.fillRect(panelW - 100, timerY + 6, (int)((doubleScoreTimer/10.0) * 70), 8);
                g2.setColor(Color.WHITE);
                g2.drawRect(panelW - 100, timerY + 6, 70, 8);
                timerY += 30;
            }
            if (jetpackTimer > 0.0) {
                if (jetpackImage != null) g2.drawImage(jetpackImage, panelW - 130, timerY, 20, 20, null);
                g2.setColor(new Color(120, 200, 255));
                g2.fillRect(panelW - 100, timerY + 6, (int)((jetpackTimer/7.0) * 70), 8);
                g2.setColor(Color.WHITE);
                g2.drawRect(panelW - 100, timerY + 6, 70, 8);
                timerY += 30;
            }
            if (slowMoTimer > 0.0) {
                if (slowMoImage != null) g2.drawImage(slowMoImage, panelW - 130, timerY, 20, 20, null);
                g2.setColor(new Color(90, 230, 220));
                g2.fillRect(panelW - 100, timerY + 6, (int)((slowMoTimer/6.0) * 70), 8);
                g2.setColor(Color.WHITE);
                g2.drawRect(panelW - 100, timerY + 6, 70, 8);
            }
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

            Image imgToDraw = playerRunImage;
            if (player.isOnGround() && Math.sin(runCycleTime * 0.04) > 0) {
                imgToDraw = playerRunImage2;
            }
            
            double targetX = laneCenterX[player.getLane()] - (PLAYER_WIDTH / 2.0);
            boolean sliding = player.isSliding();
            if (player.isFlying()) {
                imgToDraw = playerJumpImage; // tucked pose reads well while hovering
            } else if (!player.isOnGround()) {
                imgToDraw = playerJumpImage;
            } else if (sliding && playerSlideImage != null) {
                imgToDraw = playerSlideImage;
            } else if (player.getX() < targetX - 1.0) {
                imgToDraw = playerLeftImage;
            } else if (player.getX() > targetX + 1.0) {
                imgToDraw = playerRightImage;
            }

            if (imgToDraw != null) {
                if (sliding && imgToDraw == playerSlideImage) {
                    // The slide art is wide and low: keep it grounded and centred.
                    int slideW = (int) (PLAYER_WIDTH * 1.5);
                    int slideH = (int) (PLAYER_HEIGHT * SLIDE_HEIGHT_RATIO);
                    playerG2.drawImage(imgToDraw, -slideW / 2, -slideH, slideW, slideH, null);
                } else {
                    playerG2.drawImage(imgToDraw, drawX, drawY, PLAYER_WIDTH, PLAYER_HEIGHT, null);
                }
            } else {
                // Fallback drawing if images fail to load
                playerG2.setColor(new Color(52, 208, 255));
                playerG2.fillRoundRect(drawX, drawY, PLAYER_WIDTH, PLAYER_HEIGHT, 10, 10);
            }

            // Jetpack strapped to the player's back, with a flickering thrust flame
            if (player.isFlying()) {
                if (jetpackImage != null) {
                    playerG2.drawImage(jetpackImage, drawX - 8, drawY + 12, 18, 18, null);
                }
                double flicker = 0.6 + 0.4 * Math.abs(Math.sin(System.nanoTime() / 6e7));
                int flameH = (int) (16 * flicker);
                playerG2.setColor(new Color(255, 170, 40, 210));
                playerG2.fillOval(drawX + 6, -2, 9, flameH);
                playerG2.fillOval(drawX + PLAYER_WIDTH - 15, -2, 9, flameH);
                playerG2.setColor(new Color(255, 240, 160, 230));
                playerG2.fillOval(drawX + 8, -1, 5, flameH / 2);
                playerG2.fillOval(drawX + PLAYER_WIDTH - 13, -1, 5, flameH / 2);
            }

            playerG2.dispose();

        }

        
        private void drawObstacles(Graphics2D g2) {
            for (Obstacle obstacle : obstacles) {
                int x = (int) obstacle.getX();
                int y = (int) obstacle.getY();
                int w = (int) obstacle.getWidth();
                int h = (int) obstacle.getHeight();
                
                int type = obstacle.getType();
                Image obsImg = obstacleImages[type];

                if (isOncoming(type)) {
                    drawOncomingEffects(g2, obstacle, x, y, w, h);
                }

                if (obsImg != null) {
                    // Draw centered if original width differs
                    g2.drawImage(obsImg, x, y, w, h, null);
                } else {
                    // Fallback Base block
                    g2.setColor(new Color(240, 60, 60));
                    g2.fillRoundRect(x, y, w, h, 6, 6);
                }
            }
        }

        /**
         * Driving animation for oncoming traffic: speed streaks trailing behind the
         * vehicle, twin headlight cones sweeping ahead of it and a pulsing glow, so
         * the vehicle reads as actively driving rather than sliding down the road.
         */
        private void drawOncomingEffects(Graphics2D g2, Obstacle o, int x, int y, int w, int h) {
            Composite old = g2.getComposite();
            double spin = o.getWheelSpin();

            // Motion streaks behind the vehicle (it travels downward, so they trail upward)
            g2.setColor(new Color(255, 255, 255, 60));
            for (int i = 0; i < 3; i++) {
                int sx = x + (int) (w * (0.22 + 0.28 * i));
                int len = 16 + (int) (7 * Math.abs(Math.sin(spin * 14.0 + i)));
                g2.fillRect(sx, y - len, 2, len);
            }

            // Headlight beams projected ahead (down-screen) of the vehicle
            int beamLen = (int) (h * 0.85);
            int lx = x + (int) (w * 0.22);
            int rx = x + (int) (w * 0.78);
            int by = y + h;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.30f));
            g2.setColor(new Color(255, 246, 190));
            g2.fillPolygon(new int[]{lx, rx, rx + (int) (w * 0.42), lx - (int) (w * 0.42)},
                           new int[]{by - 4, by - 4, by + beamLen, by + beamLen}, 4);

            // Pulsing headlamps
            double pulse = 0.72 + 0.28 * Math.abs(Math.sin(spin * 9.0));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) pulse));
            g2.setColor(new Color(255, 252, 214));
            int r = Math.max(4, (int) (w * 0.17));
            g2.fillOval(lx - r / 2, by - r / 2 - 2, r, r);
            g2.fillOval(rx - r / 2, by - r / 2 - 2, r, r);

            g2.setComposite(old);
        }

        private void drawCoins(Graphics2D g2) {
            for (Coin coin : coins) {
                int x = (int) coin.getX();
                int y = (int) coin.getY();
                int size = (int) coin.getSize();
                
                int coinIndex = (int) (((System.currentTimeMillis() / 100) + Math.abs(coin.hashCode() % 8)) % 8);
                if (coinImages[coinIndex] != null) {
                    g2.drawImage(coinImages[coinIndex], x, y, size, size, null);
                } else {
                    g2.setColor(new Color(242, 206, 80));
                    g2.fillOval(x, y, size, size);
                }
            }
        }

        private void drawLifePowerUps(Graphics2D g2) {
            for (LifePowerUp lifePowerUp : lifePowerUps) {
                int x = (int) lifePowerUp.getX();
                int y = (int) lifePowerUp.getY();
                int size = (int) lifePowerUp.getSize();

                g2.setColor(new Color(255, 92, 132));
                if(heartImage!=null) g2.drawImage(heartImage,x,y,size,size,null); else g2.fillOval(x, y, size, size);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 14));
                g2.drawString("+", x + 6, y + 16);
            }
        }
        private static Image iconFor(PowerUp.Type type) {
            switch (type) {
                case SHIELD: return shieldImage;
                case MAGNET: return magnetImage;
                case BOOST: return boostImage;
                case DOUBLE_SCORE: return doubleScoreImage;
                case JETPACK: return jetpackImage;
                case SLOW_MO: return slowMoImage;
                default: return null;
            }
        }

        private void drawPowerUps(Graphics2D g2) {
            for (PowerUp pu : powerUps) {
                int x = (int) pu.getX();
                int y = (int) pu.getY();
                int size = (int) pu.getSize();
                Image img = iconFor(pu.getType());
                if (img != null) {
                    g2.drawImage(img, x, y, size, size, null);
                } else {
                    g2.setColor(Color.WHITE);
                    g2.fillOval(x, y, size, size);
                }
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
            g2.drawString("A/D: lanes | Space: jump | S: slide | P: pause | M: mute", 40, panelH - 16);
        }

        private void drawMobileControls(Graphics2D g2) {
            int buttonY = panelH - 86;
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
            g2.fillRect(0, 0, panelW, panelH);
            g2.setColor(new Color(255, 90, 90));
            g2.setFont(new Font("SansSerif", Font.BOLD, 30));
            drawCenteredText(g2, "GAME OVER", 320);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 18));
            drawCenteredText(g2, "Final Score: " + score, 355);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 15));
            drawCenteredText(g2, "Coins: " + coinsCollected + "  |  Distance: " + distance + "m", 380);

            if (!gameOverMessage.isEmpty()) {
                if (gameOverMessage.contains("not saved")) {
                    g2.setColor(new Color(255, 100, 100));
                } else if (gameOverMessage.contains("best")) {
                    g2.setColor(new Color(112, 255, 112));
                } else {
                    g2.setColor(new Color(150, 150, 150));
                }
                g2.setFont(new Font("SansSerif", Font.BOLD, 14));
                drawCenteredText(g2, gameOverMessage, 410);
            }

            g2.setColor(new Color(112, 196, 255));
            g2.fillRoundRect(110, 450, 200, 40, 20, 20);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 16));
            drawCenteredText(g2, "PLAY AGAIN", 475);

            g2.setColor(new Color(100, 100, 100));
            g2.fillRoundRect(110, 510, 200, 40, 20, 20);
            g2.setColor(Color.WHITE);
            drawCenteredText(g2, "HOME", 535);
        }

        private void drawPausedOverlay(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, panelW, panelH);
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
            g2.fillRoundRect(24, 48, panelW - 48, 140, 12, 12);
            g2.setColor(new Color(72, 180, 255));
            g2.drawRoundRect(24, 48, panelW - 48, 140, 12, 12);

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
            g2.drawString(text, (panelW - textWidth) / 2, baseline);
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
            private double x = 0;
            private double y = GROUND_Y;
            private double velocityY = 0.0;
            private boolean onGround = true;
            private double slideTimer = 0.0;
            private boolean flying = false;

            /** Duck under high obstacles; ignored while airborne. */
            void slide() {
                if (onGround && !flying) {
                    slideTimer = SLIDE_DURATION;
                }
            }

            /** Jetpack hover: lifts the player above the road while active. */
            void setFlying(boolean flying) {
                this.flying = flying;
                if (flying) slideTimer = 0.0;
            }

            boolean isFlying() {
                return flying;
            }

            boolean isSliding() {
                return slideTimer > 0.0;
            }

            void moveLeft() {
                lane = Math.max(0, lane - 1);
            }

            void moveRight() {
                lane = Math.min(2, lane + 1);
            }

            void jump() {
                if (onGround) {
                    onGround = false;
                    slideTimer = 0.0; // jumping cancels a slide
                    velocityY = -JUMP_STRENGTH;
                }
            }

            void update(double deltaTime) {
                if (slideTimer > 0.0) {
                    slideTimer = Math.max(0.0, slideTimer - deltaTime);
                }
                // Initialize if x is 0 and we have layout
                if (x == 0 && laneCenterX[lane] != 0) {
                    x = laneCenterX[lane] - (PLAYER_WIDTH / 2.0);
                }
                // Smooth lane movement interpolation (frame-rate independent)
                double lerpSpeed = 16.0;
                double targetXPos = laneCenterX[lane] - (PLAYER_WIDTH / 2.0);
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

            void reset() {
                lane = 1;
                x = laneCenterX[lane] - (PLAYER_WIDTH / 2.0);
                y = GROUND_Y;
                velocityY = 0.0;
                onGround = true;
                slideTimer = 0.0;
                flying = false;
            }

            void setX(double x) { this.x = x; }
            void setY(double y) { this.y = y; }
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
                return lane; // now we just return current lane target directly
            }

            boolean isJumping() {
                return !onGround;
            }

            boolean isOnGround() {
                return onGround;
            }

            Rectangle2D.Double getHitbox() {
                // Inset body hitbox to avoid unfair pixel collisions
                double padX = 8.0;
                double padY = 6.0;
                double jumpLeniency = onGround ? 0.0 : 24.0;
                double height = PLAYER_HEIGHT - padY * 2.0 - jumpLeniency;
                if (isSliding()) {
                    // Crouched: shorter box anchored to the feet so tall hazards pass overhead
                    double slideHeight = height * SLIDE_HEIGHT_RATIO;
                    return new Rectangle2D.Double(x + padX, y + padY + (height - slideHeight),
                        PLAYER_WIDTH - padX * 2.0, slideHeight);
                }
                return new Rectangle2D.Double(x + padX, y + padY,
                    PLAYER_WIDTH - padX * 2.0, height);
            }
        }

        private static class Obstacle {
            private final double x;
            private double y;
            private final int lane;
            private final double width;
            private final double height;
            private final int type;
            private boolean hit = false;
            private boolean passed = false;
            private double wheelSpin = 0.0;

            Obstacle(double startX, double startY, int lane, double w, double h, int type) {
                this.x = startX;
                this.y = startY;
                this.lane = lane;
                this.width = w;
                this.height = h;
                this.type = type;
            }
            
            int getType() { return type; }

            void update(double deltaTime, double speed) {
                // Oncoming vehicles add their own closing speed on top of the world scroll.
                y += (speed + oncomingClosingSpeed(type)) * deltaTime;
                if (isOncoming(type)) {
                    wheelSpin += deltaTime;
                }
            }

            /** Drives the driving animation (wheel blur / headlight flicker). */
            double getWheelSpin() { return wheelSpin; }

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

        private static class PowerUp {
            enum Type { SHIELD, MAGNET, BOOST, DOUBLE_SCORE, JETPACK, SLOW_MO }
            private final double x;
            private double y;
            private final int lane;
            private final double size = 26.0;
            private final Type type;

            PowerUp(double startX, double startY, int lane, Type type) {
                this.x = startX;
                this.y = startY;
                this.lane = lane;
                this.type = type;
            }

            void update(double deltaTime, double speed) {
                y += speed * deltaTime;
            }

            boolean isOffScreen() { return y > ROAD_Y + ROAD_H + 10; }
            double getX() { return x; }
            double getY() { return y; }
            int getLane() { return lane; }
            double getSize() { return size; }
            Type getType() { return type; }

            Rectangle2D.Double getHitbox() {
                return new Rectangle2D.Double(x, y, size, size);
            }
        }
        private static class Coin {
            private double x;
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

            void setX(double x) { this.x = x; }
            void setY(double y) { this.y = y; }
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
