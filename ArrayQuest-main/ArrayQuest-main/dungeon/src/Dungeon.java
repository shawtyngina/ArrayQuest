import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

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
    boolean spawnDialogueShown, moveDialogueShown, sandraNearDialogueShown, sandraDialogueShown, bossSpawnDialogueShown;

    private BufferedImage wallImg, floorImg, doorImg, torchImg, soilImg;
    private BufferedImage playerUp, playerDown, playerLeft, playerRight;
    private BufferedImage heartImg, stoneImg, faceImg, npcImg, carrotImg, bossImg, projectileImg;
    private BufferedImage npcFaceImg, bazookaImg, bossFaceImg;
    private BufferedImage mainScreenImg, victoryImg, defeatImg, creditsImg;
    private Font pixelFont;
    String collectionMessage = "";
    int messageTimer = 0;
    ArrayList<Message> messageQueue = new ArrayList<>();
    int stoneCooldown = 0;
    final int stoneCooldownMax = 20;
    Random rand = new Random();
    int npcX = tileSize * 5, npcY = tileSize * 6;
    boolean nearSandra, inDialogue;

    boolean bossActive;
    int bossHealth = 10, bossX, bossY, bossWidth = tileSize * 2, bossHeight = tileSize * 2;
    int bossHitCounter, bossJumpTimer, bossJumpInterval = 120, bossSpeed = 2;
    int bossTargetX, bossTargetY;
    boolean bossMoving, bossDialogueShown;
    String bossDialogue = "Haha, you fell into my trap!";
    int bossDialogueTimer;

    ArrayList<ThrownStone> thrownStones = new ArrayList<>();
    ArrayList<BossProjectile> bossProjectiles = new ArrayList<>();
    ArrayList<BazookaProjectile> bazookaProjectiles = new ArrayList<>();
    int webShootTimer, webShootInterval = 60;

    class Message {
        String text;
        boolean isInstructional;

        Message(String text, boolean isInstructional) {
            this.text = text;
            this.isInstructional = isInstructional;
        }
    }

    class Bazooka {
        int damage = 10;

        BazookaProjectile shoot(int x, int y, String direction) {
            return new BazookaProjectile(x, y, direction);
        }
    }

    class Stone {
        int x, y, amount;
        Rectangle hitbox;

        Stone(int x, int y, int amount) {
            this.x = x;
            this.y = y;
            this.amount = amount;
            hitbox = new Rectangle(x, y, tileSize, tileSize);
        }
    }

    class ThrownStone {
        int x, y, speed = 10, distanceTraveled;
        final int maxDistance = tileSize * 4;
        String dir;

        ThrownStone(int x, int y, String dir) {
            this.x = x;
            this.y = y;
            this.dir = dir;
        }

        boolean update() {
            int dx = 0, dy = 0;
            switch (dir) {
                case "up" -> dy = -speed;
                case "down" -> dy = speed;
                case "left" -> dx = -speed;
                case "right" -> dx = speed;
            }
            int nextX = x + dx, nextY = y + dy;
            int tileCol = (nextX + 12) / tileSize, tileRow = (nextY + 12) / tileSize;
            if (!isWalkable(tileRow, tileCol) || distanceTraveled >= maxDistance) return false;
            x = nextX;
            y = nextY;
            distanceTraveled += speed;
            return true;
        }
    }

    class BossProjectile {
        int x, y, speed = 8, direction;
        Rectangle hitbox;

        BossProjectile(int x, int y, int direction) {
            this.x = x;
            this.y = y;
            this.direction = direction;
            hitbox = new Rectangle(x, y, tileSize / 2, tileSize / 2);
        }

        void update() {
            switch (direction) {
                case 0 -> y -= speed;
                case 1 -> y += speed;
                case 2 -> x -= speed;
                case 3 -> x += speed;
            }
            hitbox.setLocation(x, y);
        }

        boolean isOutOfBounds() {
            return x < 0 || x > screenWidth || y < 0 || y > screenHeight;
        }
    }

    class BazookaProjectile {
        int x, y, speed = 15;
        String direction;
        Rectangle hitbox;

        BazookaProjectile(int x, int y, String direction) {
            this.x = x;
            this.y = y;
            this.direction = direction;
            hitbox = new Rectangle(x, y, tileSize, tileSize);
        }

        void update() {
            switch (direction) {
                case "up" -> y -= speed;
                case "down" -> y += speed;
                case "left" -> x -= speed;
                case "right" -> x += speed;
            }
            hitbox.setLocation(x, y);
        }

        boolean isOutOfBounds() {
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
    boolean isPlayerDead;
    Bazooka bazooka;
    boolean hasBazooka;

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
        if (name.equals("MainScreen.png") || name.equals("Victory.png") ||
            name.equals("Defeat.png") || name.equals("Credits.png")) {
            return original;
        }
        BufferedImage scaled = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.drawImage(original, 0, 0, tileSize, tileSize, null);
        g2.dispose();
        return scaled;
    }

    public void loadFont() {
        try {
            pixelFont = Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("/font/PressStart2P-Regular.ttf")).deriveFont(18f);
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
            stonePiles.add(new Stone(col * tileSize, row * tileSize, rand.nextInt(3) + 3));
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
        if (gameState != GameState.PLAYING) return;

        if (playerHealth <= 0) {
            isPlayerDead = true;
            gameState = GameState.DEFEAT;
            return;
        }

        if (messageTimer <= 0 && !messageQueue.isEmpty()) {
            Message next = messageQueue.remove(0);
            collectionMessage = next.text;
            messageTimer = 200;
        }

        if (!spawnDialogueShown) {
            messageQueue.add(0, new Message("How did I get here? I should walk around and explore. **Move around with ASWD keys.**", true));
            spawnDialogueShown = true;
        }

        if (!inDialogue) {
            int nextX = playerX, nextY = playerY;
            if (upPressed) { nextY -= playerSpeed; direction = "up"; totalMovement += playerSpeed; }
            if (downPressed) { nextY += playerSpeed; direction = "down"; totalMovement += playerSpeed; }
            if (leftPressed) { nextX -= playerSpeed; direction = "left"; totalMovement += playerSpeed; }
            if (rightPressed) { nextX += playerSpeed; direction = "right"; totalMovement += playerSpeed; }

            int leftTile = (nextX + 8) / tileSize, rightTile = (nextX + tileSize - 8) / tileSize;
            int topTile = (nextY + 8) / tileSize, bottomTile = (nextY + tileSize - 8) / tileSize;
            if (isWalkable(topTile, leftTile) && isWalkable(topTile, rightTile) &&
                isWalkable(bottomTile, leftTile) && isWalkable(bottomTile, rightTile)) {
                playerX = nextX;
                playerY = nextY;
            }

            if (!moveDialogueShown && totalMovement >= 144) {
                messageQueue.add(0, new Message("I should probably collect these rocks, they may be useful.", true));
                moveDialogueShown = true;
            }

            Rectangle playerRect = new Rectangle(playerX, playerY, tileSize, tileSize);
            nearSandra = currentMapIndex == 0 && playerRect.intersects(new Rectangle(npcX, npcY, tileSize, tileSize));

            if (!sandraNearDialogueShown && nearSandra) {
                messageQueue.add(0, new Message("Oh look! A lady, let me go to her and ask where am I. **Interact with the lady using E.**", true));
                sandraNearDialogueShown = true;
            }

            int centerTileCol = (playerX + tileSize / 2) / tileSize, centerTileRow = (playerY + tileSize / 2) / tileSize;
            char currentTile = map[centerTileRow].charAt(centerTileCol);

            if (ePressed && currentTile == 'S' && carrotCount >= 5 && !hasBazooka) {
                carrotCount -= 5;
                bazooka = new Bazooka();
                hasBazooka = true;
                messageQueue.add(new Message("You obtained a Bazooka!", false));
                ePressed = false;
            }

            if (ePressed && currentTile == 'C' && carrotCount > 0) {
                carrotCount--;
                messageQueue.add(new Message("You planted a carrot!", false));
                map[centerTileRow] = map[centerTileRow].substring(0, centerTileCol) + 'c' + map[centerTileRow].substring(centerTileCol + 1);
                if (!bossActive) {
                    bossActive = true;
                    bossHealth = 10;
                    bossHitCounter = 0;
                    bossX = tileSize * (maxCol / 2 - 1);
                    bossY = tileSize * (maxRow / 2 - 1);
                    bossDialogueShown = true;
                    if (!bossSpawnDialogueShown) {
                        messageQueue.add(0, new Message("Oh no! I can use my rocks to fight the boss. **Press SPACE and aim at the boss to deal it damage.**", true));
                        bossSpawnDialogueShown = true;
                    }
                }
                ePressed = false;
            }

            stonePiles.removeIf(stone -> {
                if (playerRect.intersects(stone.hitbox)) {
                    stoneCount += stone.amount;
                    messageQueue.add(new Message("You collected a stone x" + stone.amount, false));
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
                        messageQueue.add(new Message("Boss hit! Health: " + bossHealth + "/10", false));
                        if (bossHealth <= 0) {
                            bossActive = false;
                            messageQueue.add(new Message("Boss defeated!", false));
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
                    bazookaProjectiles.add(bazooka.shoot(playerX + tileSize / 4, playerY + tileSize / 4, direction));
                } else if (stoneCount > 0 && stoneCooldown == 0) {
                    thrownStones.add(new ThrownStone(playerX + tileSize / 4, playerY + tileSize / 4, direction));
                    stoneCount--;
                    stoneCooldown = stoneCooldownMax;
                }
                spacePressed = false;
            }

            if (ePressed && nearSandra) {
                carrotCount++;
                messageQueue.add(new Message("Sandra gave you a carrot!", false));
                inDialogue = true;
                ePressed = false;
            }

            if (messageTimer > 0) {
                messageTimer--;
                if (messageTimer <= 0) collectionMessage = "";
            }

            if (currentTile == 'D') switchMap((currentMapIndex + 1) % maps.length);

            if (bossActive) {
                if (bossDialogueShown) {
                    bossDialogueTimer++;
                    if (bossDialogueTimer >= 60) bossDialogueShown = false;
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
        } else if (ePressed) {
            ePressed = false;
            inDialogue = false;
            if (!sandraDialogueShown) {
                messageQueue.add(0, new Message("I should go through that door and explore some more.", true));
                sandraDialogueShown = true;
            }
        }

        for (int i = bazookaProjectiles.size() - 1; i >= 0; i--) {
            BazookaProjectile bp = bazookaProjectiles.get(i);
            bp.update();
            if (bp.isOutOfBounds()) {
                bazookaProjectiles.remove(i);
            } else if (bossActive && new Rectangle(bossX, bossY, bossWidth, bossHeight).intersects(bp.hitbox)) {
                bossHealth -= 5;
                messageQueue.add(new Message("Boss hit! Health: " + bossHealth + "/20", false));
                bazookaProjectiles.remove(i);
                if (bossHealth <= 0) {
                    bossActive = false;
                    messageQueue.add(new Message("Boss defeated!", false));
                    gameState = GameState.VICTORY;
                }
            }
        }

        for (int i = bossProjectiles.size() - 1; i >= 0; i--) {
            BossProjectile bp = bossProjectiles.get(i);
            bp.update();
            if (bp.isOutOfBounds()) {
                bossProjectiles.remove(i);
            } else if (new Rectangle(playerX, playerY, tileSize, tileSize).intersects(bp.hitbox)) {
                playerHealth--;
                messageQueue.add(new Message("You were hit by a projectile!", false));
                bossProjectiles.remove(i);
            }
        }
    }

    public void shootProjectiles() {
        if (bossActive && webShootTimer <= 0) {
            for (int i = 0; i < 3; i++) {
                bossProjectiles.add(new BossProjectile(bossX + bossWidth / 2, bossY + bossHeight / 2, rand.nextInt(4)));
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
        messageQueue.add(new Message("Entered new map!", false));
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (gameState == GameState.MAIN_SCREEN) {
            g.drawImage(mainScreenImg, 0, 0, screenWidth, screenHeight, null);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 24));
            String instruction = "Press SPACE to Start";
            FontMetrics fm = g.getFontMetrics();
            g.drawString(instruction, (screenWidth - fm.stringWidth(instruction)) / 2, screenHeight - 50);
            return;
        }

        if (gameState == GameState.VICTORY) {
            g.drawImage(victoryImg, 0, 0, screenWidth, screenHeight, null);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 24));
            String instruction = "Press SPACE for Credits";
            FontMetrics fm = g.getFontMetrics();
            g.drawString(instruction, (screenWidth - fm.stringWidth(instruction)) / 2, screenHeight - 50);
            return;
        }

        if (gameState == GameState.DEFEAT) {
            g.drawImage(defeatImg, 0, 0, screenWidth, screenHeight, null);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 24));
            String instruction = "Press SPACE for Credits";
            FontMetrics fm = g.getFontMetrics();
            g.drawString(instruction, (screenWidth - fm.stringWidth(instruction)) / 2, screenHeight - 50);
            return;
        }

        if (gameState == GameState.CREDITS) {
            g.drawImage(creditsImg, 0, 0, screenWidth, screenHeight, null);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 24));
            String instruction = "Press SPACE to Play Again";
            FontMetrics fm = g.getFontMetrics();
            g.drawString(instruction, (screenWidth - fm.stringWidth(instruction)) / 2, screenHeight - 50);
            return;
        }

        // Render map
        for (int row = 0; row < map.length; row++) {
            for (int col = 0; col < map[row].length(); col++) {
                int x = col * tileSize, y = row * tileSize;
                char tile = map[row].charAt(col);
                switch (tile) {
                    case 'W' -> g.drawImage(wallImg, x, y, null);
                    case '.' -> g.drawImage(floorImg, x, y, null);
                    case 'C' -> { g.drawImage(floorImg, x, y, null); g.drawImage(soilImg, x, y, null); }
                    case 'D' -> { g.drawImage(floorImg, x, y, null); g.drawImage(doorImg, x, y, null); }
                    case 'T' -> { g.drawImage(floorImg, x, y, null); g.drawImage(torchImg, x, y, null); }
                    case 'c' -> { g.drawImage(floorImg, x, y, null); g.drawImage(carrotImg, x, y, null); }
                    case 'S' -> g.drawImage(floorImg, x, y, null);
                }
            }
        }

        // Render player
        BufferedImage playerImg = switch (direction) {
            case "up" -> playerUp;
            case "down" -> playerDown;
            case "left" -> playerLeft;
            case "right" -> playerRight;
            default -> playerDown;
        };
        g.drawImage(playerImg, playerX, playerY, tileSize, tileSize, null);
        if (hasBazooka) g.drawImage(bazookaImg, playerX + tileSize + 10, playerY, tileSize / 2, tileSize / 2, null);

        // Render NPC
        if (currentMapIndex == 0) g.drawImage(npcImg, npcX, npcY, tileSize, tileSize, null);

        // Render stones
        for (Stone stone : stonePiles) g.drawImage(stoneImg, stone.x, stone.y, tileSize, tileSize, null);

        // Render thrown stones
        for (ThrownStone stone : thrownStones) g.drawImage(stoneImg, stone.x, stone.y, 24, 24, null);

        // Render boss and projectiles
        if (bossActive) {
            g.drawImage(bossImg, bossX, bossY, bossWidth, bossHeight, null);
            g.setColor(Color.GRAY);
            g.fillRect(bossX, bossY - 10, bossWidth, 8);
            g.setColor(Color.RED);
            g.fillRect(bossX, bossY - 10, (int) ((double) bossHealth / 10 * bossWidth), 8);
        }
        for (BazookaProjectile bp : bazookaProjectiles) g.drawImage(projectileImg, bp.x, bp.y, tileSize, tileSize, null);
        for (BossProjectile bp : bossProjectiles) g.drawImage(projectileImg, bp.x, bp.y, tileSize / 2, tileSize / 2, null);

        // Render UI
        g.drawImage(faceImg, 10, 10, 80, 80, null);
        int heartStartX = 100, heartY = 20;
        for (int i = 0; i < playerHealth; i++) g.drawImage(heartImg, heartStartX + (i * 20), heartY, 32, 32, null);
        int stoneY = heartY + 40;
        g.drawImage(stoneImg, heartStartX, stoneY, 24, 24, null);
        g.setColor(Color.WHITE);
        g.setFont(pixelFont);
        g.drawString("x" + stoneCount, heartStartX + 30, stoneY + 16);
        int carrotY = stoneY + 40;
        g.drawImage(carrotImg, heartStartX, carrotY, 24, 24, null);
        g.drawString("x" + carrotCount, heartStartX + 30, carrotY + 16);
        if (hasBazooka) g.drawImage(bazookaImg, heartStartX, carrotY + 40, 32, 32, null);

        // Render dialogues
        if (inDialogue) {
            g.setColor(new Color(0, 0, 0, 200));
            g.fillRect(0, screenHeight - 150, screenWidth, 150);
            g.setColor(Color.WHITE);
            g.drawRect(10, screenHeight - 140, screenWidth - 20, 130);
            g.drawImage(npcImg, 20, screenHeight - 130, 80, 80, null);
            g.drawImage(npcFaceImg, 20, screenHeight - 130, 80, 80, null);
            g.setFont(pixelFont.deriveFont(14f));
            g.setColor(Color.WHITE);
            String[] lines = {
                "Sandra: Can you plant mysterious carrots?",
                "They only grow in special soil.",
                "Press E near soil to plant one."
            };
            for (int i = 0; i < lines.length; i++) g.drawString(lines[i], 120, screenHeight - 130 + (i + 1) * 30);
            g.setFont(pixelFont.deriveFont(12f));
            g.drawString("Press E to continue...", screenWidth - 200, screenHeight - 30);
        } else if (bossActive && bossDialogueShown) {
            g.setColor(new Color(0, 0, 0, 200));
            g.fillRect(10, screenHeight - 150, screenWidth - 20, 130);
            g.setColor(Color.WHITE);
            g.drawRect(10, screenHeight - 150, screenWidth - 20, 130);
            g.drawImage(bossFaceImg, 20, screenHeight - 140, 80, 80, null);
            g.setFont(pixelFont.deriveFont(14f));
            g.drawString(bossDialogue, 120, screenHeight - 110);
            g.setFont(pixelFont.deriveFont(12f));
            g.drawString("Press E to continue...", screenWidth - 200, screenHeight - 30);
        }

        // Render collection messages
        if (messageTimer > 0 && !collectionMessage.isEmpty()) {
            Font regularFont = pixelFont.deriveFont(20f), boldFont = pixelFont.deriveFont(Font.BOLD, 20f);
            g.setColor(Color.YELLOW);
            FontMetrics fmRegular = g.getFontMetrics(regularFont), fmBold = g.getFontMetrics(boldFont);
            int maxWidth = screenWidth - 40;
            ArrayList<String> lines = new ArrayList<>();
            StringBuilder currentLine = new StringBuilder();
            for (String word : collectionMessage.split(" ")) {
                String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
                if (fmRegular.stringWidth(testLine.replaceAll("\\*\\*", "")) <= maxWidth) {
                    currentLine.append(currentLine.length() == 0 ? word : " " + word);
                } else {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                }
            }
            if (currentLine.length() > 0) lines.add(currentLine.toString());
            int lineHeight = fmRegular.getHeight(), totalHeight = lineHeight * lines.size();
            int startY = screenHeight - 40 - (totalHeight - lineHeight) / 2;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                int x = (screenWidth - fmRegular.stringWidth(line.replaceAll("\\*\\*", ""))) / 2;
                int y = startY + i * lineHeight;
                String[] segments = line.split("(\\*\\*)");
                int currentX = x;
                boolean isBold = false;
                for (String segment : segments) {
                    if (segment.isEmpty()) { isBold = !isBold; continue; }
                    g.setFont(isBold ? boldFont : regularFont);
                    FontMetrics fm = isBold ? fmBold : fmRegular;
                    g.drawString(segment, currentX, y);
                    currentX += fm.stringWidth(segment);
                    isBold = !isBold;
                }
            }
        }

        if (isPlayerDead) {
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRect(0, 0, screenWidth, screenHeight);
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 64));
            FontMetrics fm = g.getFontMetrics();
            String message = "YOU DIED";
            g.drawString(message, (screenWidth - fm.stringWidth(message)) / 2, screenHeight / 2 + fm.getAscent() / 2);
        }
    }

    public void keyPressed(KeyEvent e) {
        switch (gameState) {
            case MAIN_SCREEN -> {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) gameState = GameState.PLAYING;
            }
            case VICTORY, DEFEAT -> {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) gameState = GameState.CREDITS;
            }
            case CREDITS -> {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    gameState = GameState.MAIN_SCREEN;
                    playerHealth = 5;
                    stoneCount = 0;
                    carrotCount = 0;
                    hasBazooka = false;
                    bazooka = null;
                    bossActive = false;
                    bossHealth = 10;
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
                    nearSandra = false;
                    webShootTimer = 0;
                    totalMovement = 0;
                    spawnDialogueShown = false;
                    moveDialogueShown = false;
                    sandraNearDialogueShown = false;
                    sandraDialogueShown = false;
                    bossSpawnDialogueShown = false;
                    messageQueue.clear();
                    messageTimer = 0;
                    collectionMessage = "";
                }
            }
            case PLAYING -> {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W -> upPressed = true;
                    case KeyEvent.VK_S -> downPressed = true;
                    case KeyEvent.VK_A -> leftPressed = true;
                    case KeyEvent.VK_D -> rightPressed = true;
                    case KeyEvent.VK_SPACE -> spacePressed = true;
                    case KeyEvent.VK_E -> ePressed = true;
                }
            }
        }
    }

    public void keyReleased(KeyEvent e) {
        if (gameState != GameState.PLAYING) return;
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