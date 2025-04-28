import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.List;

public class Dungeon extends JPanel implements Runnable, KeyListener {
    final int tileSize = 48;
    final int maxCol = 16;
    final int maxRow = 12;
    final int screenWidth = tileSize * maxCol;
    final int screenHeight = tileSize * maxRow;

    Thread gameThread;
    boolean upPressed, downPressed, leftPressed, rightPressed, spacePressed, ePressed;
    int playerX = tileSize * 7, playerY = tileSize * 9, playerSpeed = 4, playerHealth = 5, stoneCount = 0, carrotCount = 0;
    String direction = "down";
    int totalMovement = 0;
    boolean spawnDialogueShown = false;
    boolean moveDialogueShown = false;
    boolean sandraNearDialogueShown = false;
    boolean sandraDialogueShown = false;
    boolean bossSpawnDialogueShown = false;

    private BufferedImage wallImg, floorImg, doorImg, torchImg, soilImg;
    private BufferedImage playerUp, playerDown, playerLeft, playerRight;
    private BufferedImage heartImg, stoneImg, faceImg, npcImg, carrotImg, bossImg, projectileImg;
    private BufferedImage npcFaceImg, bazookaImg, bossFaceImg;
    private BufferedImage mainScreenImg, victoryImg, defeatImg, creditsImg;
    private Font pixelFont;
    String collectionMessage = "";
    int messageTimer = 0;
    int stoneCooldown = 0;
    final int stoneCooldownMax = 20;
    Random rand = new Random();
    int npcX = tileSize * 5;
    int npcY = tileSize * 6;
    boolean nearSandra = false, inDialogue = false;
    int currentDialogue = 0;

    boolean bossActive = false;
    int bossHealth = 20, bossX, bossY, bossWidth = tileSize * 2, bossHeight = tileSize * 2;
    int bossHitCounter = 0, bossJumpTimer = 0, bossJumpInterval = 120, bossSpeed = 2;
    int bossTargetX, bossTargetY;
    boolean bossMoving = false, bossDialogueShown = false;
    String bossDialogue = "Haha, you fell into my trap!";
    int bossDialogueTimer = 0;

    ArrayList<ThrownStone> thrownStones = new ArrayList<>();
    ArrayList<BossProjectile> bossProjectiles = new ArrayList<>();
    ArrayList<BazookaProjectile> bazookaProjectiles = new ArrayList<>();
    int webShootTimer = 0, webShootInterval = 60;

    class Bazooka {
        int damage;

        public Bazooka(int damage) {
            this.damage = damage;
        }

        public BazookaProjectile shoot(int x, int y, String direction) {
            return new BazookaProjectile(x, y, direction);
        }
    }

    class Stone {
        int x, y, amount;
        Rectangle hitbox;

        public Stone(int x, int y, int amount) {
            this.x = x;
            this.y = y;
            this.amount = amount;
            hitbox = new Rectangle(x, y, tileSize, tileSize);
        }

        public void draw(Graphics g) {
            g.drawImage(stoneImg, x, y, tileSize, tileSize, null);
        }
    }

    class ThrownStone {
        int x, y, speed = 10, distanceTraveled = 0;
        final int maxDistance = tileSize * 4;
        String dir;

        public ThrownStone(int x, int y, String dir) {
            this.x = x;
            this.y = y;
            this.dir = dir;
        }

        public boolean update() {
            int dx = 0, dy = 0;
            switch (dir) {
                case "up" -> dy = -speed;
                case "down" -> dy = speed;
                case "left" -> dx = -speed;
                case "right" -> dx = speed;
            }

            int nextX = x + dx;
            int nextY = y + dy;
            int tileCol = (nextX + 12) / tileSize;
            int tileRow = (nextY + 12) / tileSize;

            if (!isWalkable(tileRow, tileCol) || distanceTraveled >= maxDistance) {
                return false;
            }

            x = nextX;
            y = nextY;
            distanceTraveled += speed;
            return true;
        }

        public void draw(Graphics g) {
            g.drawImage(stoneImg, x, y, 24, 24, null);
        }
    }

    class BossProjectile {
        int x, y, speed = 8, direction;
        Rectangle hitbox;

        public BossProjectile(int x, int y, int direction) {
            this.x = x;
            this.y = y;
            this.direction = direction;
            hitbox = new Rectangle(x, y, tileSize / 2, tileSize / 2);
        }

        public void update() {
            switch (direction) {
                case 0 -> y -= speed;
                case 1 -> y += speed;
                case 2 -> x -= speed;
                case 3 -> x += speed;
            }
            hitbox.setLocation(x, y);
        }

        public void draw(Graphics g) {
            g.drawImage(projectileImg, x, y, tileSize / 2, tileSize / 2, null);
        }

        public boolean isOutOfBounds() {
            return x < 0 || x > screenWidth || y < 0 || y > screenHeight;
        }
    }

    class BazookaProjectile {
        int x, y, speed = 15;
        String direction;
        Rectangle hitbox;

        public BazookaProjectile(int x, int y, String direction) {
            this.x = x;
            this.y = y;
            this.direction = direction;
            hitbox = new Rectangle(x, y, tileSize, tileSize);
        }

        public void update() {
            switch (direction) {
                case "up" -> y -= speed;
                case "down" -> y += speed;
                case "left" -> x -= speed;
                case "right" -> x += speed;
            }
            hitbox.setLocation(x, y);
        }

        public void draw(Graphics g) {
            g.drawImage(projectileImg, x, y, tileSize, tileSize, null);
        }

        public boolean isOutOfBounds() {
            return x < 0 || x > screenWidth || y < 0 || y > screenHeight;
        }
    }

    ArrayList<Stone> stonePiles = new ArrayList<>();
    int currentMapIndex = 0;

    String[][] maps = {
            {
                "WTWWWWWWWWWDDWTW",
                "W..............W",
                "W..............W",
                "W..............W",
                "W..............W",
                "W..............W",
                "W..............W",
                "W..............W",
                "W..............W",
                "W..............W",
                "W..............W",
                "WWWWWWWWWWWWWWWW"
            },
            {
                "WWWWWWWWWWWWWWWW",
                "W..............W",
                "T.....C........T",
                "W..............W",
                "W..............W",
                "W..............W",
                "W..............W",
                "W..............W",
                "W..............W",
                "T.............ST",
                "W..............W",
                "WWWWWWWWWWWWWWWW"
            }
    };

    String[] map = maps[currentMapIndex];
    boolean isPlayerDead = false;
    Bazooka bazooka;
    boolean hasBazooka = false;

    enum GameState { MAIN_SCREEN, PLAYING, VICTORY, DEFEAT, CREDITS }
    private GameState gameState = GameState.MAIN_SCREEN;

    public Dungeon() {
        setPreferredSize(new Dimension(screenWidth, screenHeight));
        setBackground(Color.black);
        setDoubleBuffered(true);
        addKeyListener(this);
        setFocusable(true);
        loadImages();
        loadFont();
        spawnStones();
    }

    public void loadImages() {
        try {
            wallImg = loadImage("wall.png");
            floorImg = loadImage("floor.png");
            doorImg = loadImage("door.png");
            torchImg = loadImage("torch.png");
            soilImg = loadImage("soil.png");
            playerUp = loadImage("player_up.png");
            playerDown = loadImage("player_down.png");
            playerLeft = loadImage("player_left.png");
            playerRight = loadImage("player_right.png");
            heartImg = loadImage("heart.png");
            stoneImg = loadImage("stone.png");
            faceImg = loadImage("face.png");
            npcImg = loadImage("sandra.png");
            carrotImg = loadImage("carrot.png");
            bossImg = loadImage("boss.png");
            projectileImg = loadImage("bazooka_projectile.png");
            npcFaceImg = loadImage("sandra_face.png");
            bazookaImg = loadImage("bazooka.png");
            bossFaceImg = loadImage("boss_face.png");
            mainScreenImg = loadImage("MainScreen.png");
            victoryImg = loadImage("Victory.png");
            defeatImg = loadImage("Defeat.png");
            creditsImg = loadImage("Credits.png");
        } catch (IOException e) {
            System.out.println("Image loading failed: " + e.getMessage());
        }
    }

    public BufferedImage loadImage(String name) throws IOException {
        BufferedImage original = ImageIO.read(getClass().getResourceAsStream("/image/" + name));
        BufferedImage scaled = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.drawImage(original, 0, 0, tileSize, tileSize, null);
        g2.dispose();
        return scaled;
    }

    public void loadFont() {
        try {
            pixelFont = Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("/font/PressStart2P-Regular.ttf"))
                    .deriveFont(18f);
        } catch (Exception e) {
            pixelFont = new Font("Monospaced", Font.PLAIN, 18);
        }
    }

    public void spawnStones() {
        stonePiles.clear();
        int piles = rand.nextInt(2) + 3;

        for (int i = 0; i < piles; i++) {
            int col, row;
            do {
                col = rand.nextInt(maxCol);
                row = rand.nextInt(maxRow);
            } while (map[row].charAt(col) != '.');

            int x = col * tileSize;
            int y = row * tileSize;
            int amount = rand.nextInt(3) + 3;
            stonePiles.add(new Stone(x, y, amount));
        }
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void run() {
        double interval = 1000000000.0 / 60;
        double delta = 0;
        long lastTime = System.nanoTime();

        while (true) {
            long now = System.nanoTime();
            delta += (now - lastTime) / interval;
            lastTime = now;

            if (delta >= 1) {
                update();
                repaint();
                delta--;
            }
        }
    }

    public void update() {
        if (isPlayerDead) return;

        if (gameState == GameState.PLAYING) {
            if (playerHealth <= 0) {
                isPlayerDead = true;
                gameState = GameState.DEFEAT;
                return;
            }

            if (!spawnDialogueShown) {
                if (messageTimer <= 0) {
                    collectionMessage = "How did I get here? I should walk around and explore. Move around with ASWD keys.";
                    messageTimer = 600;
                    spawnDialogueShown = true;
                }
            }

            if (!inDialogue) {
                int nextX = playerX;
                int nextY = playerY;

                if (upPressed) {
                    nextY -= playerSpeed;
                    direction = "up";
                    totalMovement += playerSpeed;
                }
                if (downPressed) {
                    nextY += playerSpeed;
                    direction = "down";
                    totalMovement += playerSpeed;
                }
                if (leftPressed) {
                    nextX -= playerSpeed;
                    direction = "left";
                    totalMovement += playerSpeed;
                }
                if (rightPressed) {
                    nextX += playerSpeed;
                    direction = "right";
                    totalMovement += playerSpeed;
                }

                int leftTile = (nextX + 8) / tileSize;
                int rightTile = (nextX + tileSize - 8) / tileSize;
                int topTile = (nextY + 8) / tileSize;
                int bottomTile = (nextY + tileSize - 8) / tileSize;

                if (isWalkable(topTile, leftTile) && isWalkable(topTile, rightTile) &&
                    isWalkable(bottomTile, leftTile) && isWalkable(bottomTile, rightTile)) {
                    playerX = nextX;
                    playerY = nextY;
                }

                if (!moveDialogueShown && totalMovement >= 144 && messageTimer <= 0) {
                    collectionMessage = "I should probably collect these rocks, they may be useful.";
                    messageTimer = 600;
                    moveDialogueShown = true;
                }

                Rectangle playerRect = new Rectangle(playerX, playerY, tileSize, tileSize);
                if (currentMapIndex == 0) {
                    Rectangle npcRect = new Rectangle(npcX, npcY, tileSize, tileSize);
                    nearSandra = playerRect.intersects(npcRect);
                } else {
                    nearSandra = false;
                }

                if (!sandraNearDialogueShown && nearSandra && messageTimer <= 0) {
                    collectionMessage = "Oh look! A lady, let me go to her and ask where am I. Interact with the lady using E.";
                    messageTimer = 600;
                    sandraNearDialogueShown = true;
                }

                int centerTileCol = (playerX + tileSize / 2) / tileSize;
                int centerTileRow = (playerY + tileSize / 2) / tileSize;
                char currentTile = map[centerTileRow].charAt(centerTileCol);

                if (ePressed && currentTile == 'S' && carrotCount >= 5 && !hasBazooka) {
                    carrotCount -= 5;
                    bazooka = new Bazooka(10);
                    hasBazooka = true;
                    if (messageTimer <= 0) {
                        collectionMessage = "You obtained a Bazooka!";
                        messageTimer = 600;
                    }
                    ePressed = false;
                }

                if (ePressed && currentTile == 'C' && carrotCount > 0) {
                    carrotCount--;
                    if (messageTimer <= 0) {
                        collectionMessage = "You planted a carrot!";
                        messageTimer = 600;
                    }

                    StringBuilder modifiedRow = new StringBuilder(map[centerTileRow]);
                    modifiedRow.setCharAt(centerTileCol, 'c');
                    map[centerTileRow] = modifiedRow.toString();

                    if (!bossActive) {
                        bossActive = true;
                        bossHealth = 20;
                        bossHitCounter = 0;
                        bossX = tileSize * (maxCol / 2 - 1);
                        bossY = tileSize * (maxRow / 2 - 1);
                        bossDialogueShown = true;
                        if (!bossSpawnDialogueShown && messageTimer <= 0) {
                            collectionMessage = "Oh no! I can use my rocks to fight the boss. Press SPACE and aim at the boss to deal it damage.";
                            messageTimer = 600;
                            bossSpawnDialogueShown = true;
                        }
                    }
                    ePressed = false;
                }

                stonePiles.removeIf(stone -> {
                    if (playerRect.intersects(stone.hitbox)) {
                        stoneCount += stone.amount;
                        if (messageTimer <= 0) {
                            collectionMessage = "You collected a stone x" + stone.amount;
                            messageTimer = 600;
                        }
                        return true;
                    }
                    return false;
                });

                if (bossActive) {
                    Rectangle bossRect = new Rectangle(bossX, bossY, bossWidth, bossHeight);
                    thrownStones.removeIf(stone -> {
                        if (bossRect.intersects(new Rectangle(stone.x, stone.y, 24, 24))) {
                            bossHitCounter++;
                            bossHealth--;
                            if (messageTimer <= 0) {
                                collectionMessage = "Boss hit! Health: " + bossHealth + "/20";
                                messageTimer = 600;
                            }
                            if (bossHealth <= 0) {
                                bossActive = false;
                                if (messageTimer <= 0) {
                                    collectionMessage = "Boss defeated!";
                                    messageTimer = 600;
                                }
                                gameState = GameState.VICTORY;
                            }
                            return true;
                        }
                        return false;
                    });
                }

                thrownStones.removeIf(stone -> !stone.update());

                if (stoneCooldown > 0) stoneCooldown--;

                if (spacePressed) {
                    if (hasBazooka) {
                        BazookaProjectile projectile = bazooka.shoot(playerX + tileSize / 4, playerY + tileSize / 4, direction);
                        bazookaProjectiles.add(projectile);
                        spacePressed = false;
                    } else if (stoneCount > 0 && stoneCooldown == 0) {
                        thrownStones.add(new ThrownStone(playerX + tileSize / 4, playerY + tileSize / 4, direction));
                        stoneCount--;
                        stoneCooldown = stoneCooldownMax;
                        spacePressed = false;
                    }
                }

                if (ePressed && nearSandra) {
                    carrotCount++;
                    if (messageTimer <= 0) {
                        collectionMessage = "Sandra gave you a carrot!";
                        messageTimer = 600;
                    }
                    inDialogue = true;
                    currentDialogue = 0;
                    ePressed = false;
                }

                if (messageTimer > 0) messageTimer--;
                else collectionMessage = "";

                if (currentTile == 'D') {
                    switchMap((currentMapIndex + 1) % maps.length);
                }

                if (bossActive) {
                    if (bossDialogueShown) {
                        bossDialogueTimer++;
                        if (bossDialogueTimer >= 60) {
                            bossDialogueShown = false;
                        }
                    }

                    bossJumpTimer++;
                    if (bossJumpTimer >= bossJumpInterval) {
                        bossTargetX = tileSize * (rand.nextInt(maxCol - 2) + 1);
                        bossTargetY = tileSize * (rand.nextInt(maxRow - 2) + 1);
                        bossJumpTimer = 0;
                        bossMoving = true;
                    }

                    if (bossMoving) {
                        if (bossX < bossTargetX) bossX += bossSpeed;
                        else if (bossX > bossTargetX) bossX -= bossSpeed;

                        if (bossY < bossTargetY) bossY += bossSpeed;
                        else if (bossY > bossTargetY) bossY -= bossSpeed;

                        if (Math.abs(bossX - bossTargetX) < bossSpeed && Math.abs(bossY - bossTargetY) < bossSpeed) {
                            bossX = bossTargetX;
                            bossY = bossTargetY;
                            bossMoving = false;
                        }
                    }

                    shootProjectiles();
                }
            } else {
                if (ePressed) {
                    ePressed = false;
                    inDialogue = false;
                    if (!sandraDialogueShown && messageTimer <= 0) {
                        collectionMessage = "I should go through that door and explore some more.";
                        messageTimer = 600;
                        sandraDialogueShown = true;
                    }
                }
            }

            for (int i = bazookaProjectiles.size() - 1; i >= 0; i--) {
                BazookaProjectile bp = bazookaProjectiles.get(i);
                bp.update();

                if (bp.isOutOfBounds()) {
                    bazookaProjectiles.remove(i);
                } else {
                    Rectangle bossRect = new Rectangle(bossX, bossY, bossWidth, bossHeight);
                    if (bossRect.intersects(bp.hitbox)) {
                        bossHealth -= 5;
                        if (messageTimer <= 0) {
                            collectionMessage = "Boss hit! Health: " + bossHealth + "/20";
                            messageTimer = 600;
                        }

                        bazookaProjectiles.remove(i);
                        if (bossHealth <= 0) {
                            bossActive = false;
                            if (messageTimer <= 0) {
                                collectionMessage = "Boss defeated!";
                                messageTimer = 600;
                            }
                            gameState = GameState.VICTORY;
                        }
                    }
                }
            }

            for (int i = 0; i < bossProjectiles.size(); i++) {
                BossProjectile bp = bossProjectiles.get(i);
                bp.update();

                if (bp.isOutOfBounds()) {
                    bossProjectiles.remove(i);
                    i--;
                } else {
                    Rectangle playerRect = new Rectangle(playerX, playerY, tileSize, tileSize);
                    if (playerRect.intersects(bp.hitbox)) {
                        if (messageTimer <= 0) {
                            collectionMessage = "You were hit by a projectile!";
                            messageTimer = 600;
                        }
                        playerHealth--;
                        bossProjectiles.remove(bp);
                        break;
                    }
                }
            }
        }
    }

    public void shootProjectiles() {
        if (bossActive && webShootTimer <= 0) {
            for (int i = 0; i < 3; i++) {
                int direction = rand.nextInt(4);
                bossProjectiles.add(new BossProjectile(bossX + bossWidth / 2, bossY + bossHeight / 2, direction));
            }
            webShootTimer = webShootInterval;
        } else {
            webShootTimer--;
        }
    }

    public boolean isWalkable(int row, int col) {
        if (row < 0 || row >= map.length || col < 0 || col >= map[0].length()) return false;
        char tile = map[row].charAt(col);
        return tile != 'W' && tile != 'T';
    }

    public void switchMap(int nextMapIndex) {
        if (nextMapIndex >= maps.length) return;

        currentMapIndex = nextMapIndex;
        map = maps[currentMapIndex];

        playerX = tileSize * 2;
        playerY = tileSize * 2;

        spawnStones();
        if (messageTimer <= 0) {
            collectionMessage = "Entered new map!";
            messageTimer = 600;
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (gameState == GameState.MAIN_SCREEN) {
            g.drawImage(mainScreenImg, 0, 0, screenWidth, screenHeight, null);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 24));
            String instruction = "Press SPACE to Start";
            FontMetrics fm = g.getFontMetrics();
            int instrWidth = fm.stringWidth(instruction);
            g.drawString(instruction, (screenWidth - instrWidth) / 2, screenHeight - 50);
            return;
        }

        if (gameState == GameState.VICTORY) {
            g.drawImage(victoryImg, 0, 0, screenWidth, screenHeight, null);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 24));
            String instruction = "Press SPACE for Credits";
            FontMetrics fm = g.getFontMetrics();
            int instrWidth = fm.stringWidth(instruction);
            g.drawString(instruction, (screenWidth - instrWidth) / 2, screenHeight - 50);
            return;
        }

        if (gameState == GameState.DEFEAT) {
            g.drawImage(defeatImg, 0, 0, screenWidth, screenHeight, null);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 24));
            String instruction = "Press SPACE for Credits";
            FontMetrics fm = g.getFontMetrics();
            int instrWidth = fm.stringWidth(instruction);
            g.drawString(instruction, (screenWidth - instrWidth) / 2, screenHeight - 50);
            return;
        }

        if (gameState == GameState.CREDITS) {
            g.drawImage(creditsImg, 0, 0, screenWidth, screenHeight, null);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 24));
            String instruction = "Press SPACE to Play Again";
            FontMetrics fm = g.getFontMetrics();
            int instrWidth = fm.stringWidth(instruction);
            g.drawString(instruction, (screenWidth - instrWidth) / 2, screenHeight - 50);
            return;
        }

        drawMap(g);
        drawPlayer(g);
        drawNPC(g);
        drawStones(g);
        drawThrownStones(g);

        for (BazookaProjectile bp : bazookaProjectiles) {
            bp.draw(g);
        }

        if (bossActive) {
            g.drawImage(bossImg, bossX, bossY, bossWidth, bossHeight, null);
            drawBossHealthBar(g);

            if (bossDialogueShown) {
                drawBossDialogueBox(g);
            }
        }

        for (BossProjectile bp : bossProjectiles) {
            bp.draw(g);
        }

        drawUI(g);

        if (isPlayerDead) {
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRect(0, 0, screenWidth, screenHeight);
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 64));
            FontMetrics fm = g.getFontMetrics();
            String message = "YOU DIED";
            int textWidth = fm.stringWidth(message);
            int x = (screenWidth - textWidth) / 2;
            int y = (screenHeight - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(message, x, y);
        }

        if (inDialogue) {
            drawDialogueBox(g);
        }
    }

    public void drawMap(Graphics g) {
        for (int row = 0; row < map.length; row++) {
            for (int col = 0; col < map[row].length(); col++) {
                char tile = map[row].charAt(col);
                int x = col * tileSize;
                int y = row * tileSize;

                if (tile == 'W') g.drawImage(wallImg, x, y, null);
                else if (tile == '.') g.drawImage(floorImg, x, y, null);
                else if (tile == 'C') {
                    g.drawImage(floorImg, x, y, null);
                    g.drawImage(soilImg, x, y, null);
                } else if (tile == 'D') {
                    g.drawImage(floorImg, x, y, null);
                    g.drawImage(doorImg, x, y, null);
                } else if (tile == 'T') {
                    g.drawImage(floorImg, x, y, null);
                    g.drawImage(torchImg, x, y, null);
                } else if (tile == 'c') {
                    g.drawImage(floorImg, x, y, null);
                    g.drawImage(carrotImg, x, y, null);
                } else if (tile == 'S') {
                    g.drawImage(floorImg, x, y, null);
                }
            }
        }
    }

    public void drawPlayer(Graphics g) {
        BufferedImage img = switch (direction) {
            case "up" -> playerUp;
            case "down" -> playerDown;
            case "left" -> playerLeft;
            case "right" -> playerRight;
            default -> playerDown;
        };

        g.drawImage(img, playerX, playerY, tileSize, tileSize, null);

        if (hasBazooka) {
            g.drawImage(bazookaImg, playerX + tileSize + 10, playerY, tileSize / 2, tileSize / 2, null);
        }
    }

    public void drawNPC(Graphics g) {
        if (currentMapIndex == 0) {
            g.drawImage(npcImg, npcX, npcY, tileSize, tileSize, null);
        }
    }

    public void drawStones(Graphics g) {
        for (Stone stone : stonePiles) stone.draw(g);
    }

    public void drawThrownStones(Graphics g) {
        for (ThrownStone stone : thrownStones) stone.draw(g);
    }

    public void drawBossHealthBar(Graphics g) {
        g.setColor(Color.GRAY);
        g.fillRect(bossX, bossY - 10, bossWidth, 8);
        g.setColor(Color.RED);
        int healthWidth = (int) ((double) bossHealth / 20 * bossWidth);
        g.fillRect(bossX, bossY - 10, healthWidth, 8);
    }

    public void drawBossDialogueBox(Graphics g) {
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(10, screenHeight - 150, screenWidth - 20, 130);
        g.setColor(Color.WHITE);
        g.drawRect(10, screenHeight - 150, screenWidth - 20, 130);
        g.drawImage(bossFaceImg, 20, screenHeight - 140, 80, 80, null);
        g.setFont(pixelFont.deriveFont(14f));
        g.setColor(Color.WHITE);
        String[] lines = {bossDialogue};
        int yPosition = screenHeight - 130 + 20;
        for (String line : lines) {
            g.drawString(line, 120, yPosition);
            yPosition += 30;
        }
        g.setFont(pixelFont.deriveFont(12f));
        g.drawString("Press E to continue...", screenWidth - 200, screenHeight - 30);
    }

    public void drawUI(Graphics g) {
        g.drawImage(faceImg, 10, 10, 80, 80, null);

        int heartWidth = 32;
        int heartHeight = 32;
        int heartSpacing = 20;
        int heartStartX = 10 + 80 + 10;
        int heartY = 20;

        for (int i = 0; i < playerHealth; i++) {
            g.drawImage(heartImg, heartStartX + (i * heartSpacing), heartY, heartWidth, heartHeight, null);
        }

        int stoneImageX = heartStartX;
        int stoneY = heartY + heartHeight + 8;
        g.drawImage(stoneImg, stoneImageX, stoneY, 24, 24, null);
        g.setColor(Color.WHITE);
        g.setFont(pixelFont);
        g.drawString("x" + stoneCount, stoneImageX + 30, stoneY + 16);

        int carrotImageX = heartStartX;
        int carrotY = stoneY + 40;
        g.drawImage(carrotImg, carrotImageX, carrotY, 24, 24, null);
        g.drawString("x" + carrotCount, carrotImageX + 30, carrotY + 16);

        if (hasBazooka) {
            g.drawImage(bazookaImg, carrotImageX, carrotY + 40, 32, 32, null);
        }

        if (messageTimer > 0 && !collectionMessage.isEmpty()) {
            g.setFont(pixelFont.deriveFont(20f));
            g.setColor(Color.YELLOW);
            FontMetrics fm = g.getFontMetrics();
            int maxWidth = screenWidth - 40;
            List<String> lines = new ArrayList<>();
            String[] words = collectionMessage.split(" ");
            StringBuilder currentLine = new StringBuilder();
            for (String word : words) {
                String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
                if (fm.stringWidth(testLine) <= maxWidth) {
                    currentLine.append(currentLine.length() == 0 ? word : " " + word);
                } else {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                }
            }
            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }

            int lineHeight = fm.getHeight();
            int totalHeight = lineHeight * lines.size();
            int startY = screenHeight - 40 - (totalHeight - lineHeight) / 2;

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                int textWidth = fm.stringWidth(line);
                int x = (screenWidth - textWidth) / 2;
                g.drawString(line, x, startY + i * lineHeight);
            }
        }
    }

    public void drawDialogueBox(Graphics g) {
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, screenHeight - 150, screenWidth, 150);
        g.setColor(Color.WHITE);
        g.drawRect(10, screenHeight - 140, screenWidth - 20, 130);
        g.drawImage(npcImg, 20, screenHeight - 130, 80, 80, null);
        g.drawImage(npcFaceImg, 20, screenHeight - 130, 80, 80, null);
        g.setFont(pixelFont.deriveFont(14f));
        g.setColor(Color.WHITE);

        String[] currentDialogueLines = {
            "Sandra: Can you plant mysterious carrots?",
            "They only grow in special soil.",
            "Press E near soil to plant one."
        };

        for (int i = 0; i < currentDialogueLines.length; i++) {
            g.drawString(currentDialogueLines[i], 120, screenHeight - 130 + (i + 1) * 30);
        }

        g.setFont(pixelFont.deriveFont(12f));
        g.drawString("Press E to continue...", screenWidth - 200, screenHeight - 30);
    }

    public void keyPressed(KeyEvent e) {
        if (gameState == GameState.MAIN_SCREEN) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                gameState = GameState.PLAYING;
            }
            return;
        }

        if (gameState == GameState.VICTORY || gameState == GameState.DEFEAT) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                gameState = GameState.CREDITS;
            }
            return;
        }

        if (gameState == GameState.CREDITS) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                gameState = GameState.MAIN_SCREEN;
                playerHealth = 5;
                stoneCount = 0;
                carrotCount = 0;
                hasBazooka = false;
                bazooka = null;
                bossActive = false;
                bossHealth = 20;
                bossHitCounter = 0;
                bossX = 0;
                bossY = 0;
                bossMoving = false;
                bossDialogueShown = false;
                bossDialogueTimer = 0;
                currentMapIndex = 0;
                maps = new String[][] {
                    {
                        "WTWWWWWWWWWDDWTW",
                        "W..............W",
                        "W..............W",
                        "W..............W",
                        "W..............W",
                        "W..............W",
                        "W..............W",
                        "W..............W",
                        "W..............W",
                        "W..............W",
                        "W..............W",
                        "WWWWWWWWWWWWWWWW"
                    },
                    {
                        "WWWWWWWWWWWWWWWW",
                        "W..............W",
                        "T.....C........T",
                        "W..............W",
                        "W..............W",
                        "W..............W",
                        "W..............W",
                        "W..............W",
                        "W..............W",
                        "T.............ST",
                        "W..............W",
                        "WWWWWWWWWWWWWWWW"
                    }
                };
                map = maps[currentMapIndex];
                playerX = tileSize * 7;
                playerY = tileSize * 9;
                direction = "down";
                isPlayerDead = false;
                thrownStones.clear();
                bazookaProjectiles.clear();
                bossProjectiles.clear();
                stonePiles.clear();
                spawnStones();
                stoneCooldown = 0;
                inDialogue = false;
                currentDialogue = 0;
                nearSandra = false;
                webShootTimer = 0;
                totalMovement = 0;
                spawnDialogueShown = false;
                moveDialogueShown = false;
                sandraNearDialogueShown = false;
                sandraDialogueShown = false;
                bossSpawnDialogueShown = false;
            }
            return;
        }

        switch (e.getKeyCode()) {
            case KeyEvent.VK_W -> upPressed = true;
            case KeyEvent.VK_S -> downPressed = true;
            case KeyEvent.VK_A -> leftPressed = true;
            case KeyEvent.VK_D -> rightPressed = true;
            case KeyEvent.VK_SPACE -> spacePressed = true;
            case KeyEvent.VK_E -> ePressed = true;
        }
    }

    public void keyReleased(KeyEvent e) {
        if (gameState == GameState.MAIN_SCREEN || gameState == GameState.VICTORY || gameState == GameState.DEFEAT || gameState == GameState.CREDITS) return;

        switch (e.getKeyCode()) {
            case KeyEvent.VK_W -> upPressed = false;
            case KeyEvent.VK_S -> downPressed = false;
            case KeyEvent.VK_A -> leftPressed = false;
            case KeyEvent.VK_D -> rightPressed = false;
            case KeyEvent.VK_E -> ePressed = false;
        }
    }

    public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        JFrame window = new JFrame("Dungeon");
        Dungeon game = new Dungeon();
        window.add(game);
        window.pack();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setLocationRelativeTo(null);
        window.setVisible(true);
        game.startGameThread();
    }
}
