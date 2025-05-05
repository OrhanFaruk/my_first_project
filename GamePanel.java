import javax.swing.*;

import Guns.Bullet;
import Guns.Explosion;
import Zombies.AbstractZombie;
import Zombies.AcidSpitterZombie;
import Zombies.CrawlerZombie;
import Zombies.NormalZombie;
import Zombies.TankZombie;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.ConcurrentModificationException;
import java.io.*;

public class GamePanel extends JPanel implements Runnable, KeyListener, MouseListener, MouseMotionListener {

    private static final int PANEL_WIDTH = 1200;
    private static final int PANEL_HEIGHT = 800;
    private static final int FPS = 60;
    private static final long FRAME_TIME = 1000 / FPS;

    private static final int MINIMAP_SIZE = 150;
    private static final int MINIMAP_X = PANEL_WIDTH - MINIMAP_SIZE - 10;
    private static final int MINIMAP_Y = 10;
    private static final float MINIMAP_SCALE = 0.15f;

    private static final int SPAWN_MARGIN = 50;

    private boolean running;
    private Thread gameThread;
    private int score;
    private int wave;
    private int zombiesKilled;
    private int zombiesRemaining;
    private long waveStartTime;
    private long lastZombieSpawnTime;
    private static final long ZOMBIE_SPAWN_INTERVAL = 1000;
    private boolean gameOver;
    private boolean waveCompleted;
    private long waveCompletionTime;
    private static final long WAVE_TRANSITION_DELAY = 3000;
    private boolean debugMode = false;
    private boolean showRestartPrompt = false;
    private long gameOverTime;
    private boolean gameStarted = false;
    private boolean gamePaused = false;
    private boolean canShoot = false;
    private boolean showMainMenu = true;
    private int menuSelection = 0; // 0: Start Game, 1: Load Game, 2: Controls, 3: Quit Option
    private boolean showControls = false;

    // Game objects
    private Player player;
    private List<AbstractZombie> zombies;
    private List<Bullet> bullets;
    private List<Explosion> explosions;
    private List<AmmoDrop> ammoDrops;

    // Input state
    private boolean[] keys;
    private int mouseX, mouseY;
    private boolean leftMousePressed;

    // Random generator
    private Random random;

    // Add a method to show wave messages to the player
    private boolean showingWaveMessage = false;
    private String waveMessage = "";
    private long waveMessageStartTime;
    private long waveMessageDuration;

    public GamePanel() {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(Color.DARK_GRAY);
        setFocusable(true);

        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);

        keys = new boolean[256];

        random = new Random();
        initializeGame();

        showMainMenu = true;
        gameStarted = false;
        startGame();
    }

    private void initializeGame() {
        player = new Player(PANEL_WIDTH / 2, PANEL_HEIGHT / 2);
        zombies = new ArrayList<>();
        bullets = new ArrayList<>();
        explosions = new ArrayList<>();
        ammoDrops = new ArrayList<>();

        score = 0;
        wave = 1;
        System.out.println("Game initialized with wave set to " + wave);
        zombiesKilled = 0;
        zombiesRemaining = 0;
        lastZombieSpawnTime = 0;
        gameOver = false;
        waveCompleted = false;
        gameStarted = false;
    }

    public void startGame() {
        if (gameThread == null || !running) {
            running = true;
            gameThread = new Thread(this);
            gameThread.start();

            if (!showMainMenu && !gameStarted) {
                gameStarted = true;

                if (wave > 0) {
                    System.out.println("Continuing from wave " + wave);
                    startCurrentWave();
                } else {
                    startNextWave();
                }
            }
        }
    }

    private void startNextWave() {
        wave++;
        System.out.println("Starting wave " + wave);
        waveStartTime = System.currentTimeMillis();
        lastZombieSpawnTime = waveStartTime;
        zombiesKilled = 0;
        waveCompleted = false;
        player.setX(PANEL_WIDTH / 2);
        player.setY(PANEL_HEIGHT / 2);

        zombies.clear();
        bullets.clear();
        explosions.clear();
        ammoDrops.clear();

        int totalZombiesForWave;
        if (wave == 1) {
            totalZombiesForWave = 3;
        } else {
            totalZombiesForWave = 3 + (wave * 2);
        }

        int initialSpawn = Math.min(2, totalZombiesForWave);
        for (int i = 0; i < initialSpawn; i++) {
            spawnZombie();
        }

        zombiesRemaining = totalZombiesForWave - initialSpawn;

        System.out.println("Wave " + wave + " started with " + initialSpawn + " zombies, " + zombiesRemaining
                + " remaining to spawn");

        showWaveMessage("WAVE " + wave + " STARTING", 3000);

        if (wave == 2) {
            // Give rifle
            player.addWeapon(2, new Guns.Rifle());
            System.out.println("Rifle unlocked! Press 2 to equip.");
        } else if (wave == 4) {
            // Give shotgun
            player.addWeapon(3, new Guns.Shotgun());
            System.out.println("Shotgun unlocked! Press 3 to equip.");
        } else if (wave == 6) {
            // Give sniper
            player.addWeapon(4, new Guns.Sniper());
            System.out.println("Sniper unlocked! Press 4 to equip.");
        } else if (wave == 10) {
            // Give rocket launcher
            player.addWeapon(5, new Guns.RocketLauncher());
            System.out.println("Rocket Launcher unlocked! Press 5 to equip.");
        }
    }

    private void startCurrentWave() {
        System.out.println("Continuing wave " + wave + " without incrementing");
        waveStartTime = System.currentTimeMillis();
        lastZombieSpawnTime = waveStartTime;
        zombiesKilled = 0;
        waveCompleted = false;

        zombies.clear();
        bullets.clear();
        explosions.clear();

        int totalZombiesForWave;
        if (wave == 1) {
            totalZombiesForWave = 3;
        } else {
            totalZombiesForWave = 3 + (wave * 2);
        }

        int initialSpawn = Math.min(2, totalZombiesForWave);
        for (int i = 0; i < initialSpawn; i++) {
            spawnZombie();
        }

        zombiesRemaining = totalZombiesForWave - initialSpawn;

        System.out.println("Wave " + wave + " continued with " + initialSpawn + " zombies, " + zombiesRemaining
                + " remaining to spawn");

        showWaveMessage("WAVE " + wave + " STARTING", 3000);
    }

    private void spawnZombie() {
        if (wave < 1) {
            System.out.println("WARNING: Attempted to spawn zombie with invalid wave number: " + wave);
            wave = 1;
        }

        int x, y;
        int side = random.nextInt(4);

        switch (side) {
            case 0:
                x = random.nextInt(PANEL_WIDTH);
                y = SPAWN_MARGIN;
                break;
            case 1:
                x = PANEL_WIDTH - SPAWN_MARGIN;
                y = random.nextInt(PANEL_HEIGHT);
                break;
            case 2:
                x = random.nextInt(PANEL_WIDTH);
                y = PANEL_HEIGHT - SPAWN_MARGIN;
                break;
            case 3:
                x = SPAWN_MARGIN;
                y = random.nextInt(PANEL_HEIGHT);
                break;
            default:
                x = SPAWN_MARGIN;
                y = SPAWN_MARGIN;
        }

        AbstractZombie zombie;
        int zombieType = random.nextInt(100);

        if (wave == 1) {
            zombie = new NormalZombie(x, y);
            System.out.println("Spawned Normal Zombie at (" + x + "," + y + ")");
        } else if (wave == 2) {
            if (zombieType < 20) {
                zombie = new CrawlerZombie(x, y);
                System.out.println("Spawned Crawler Zombie at (" + x + "," + y + ")");
            } else {
                zombie = new NormalZombie(x, y);
                System.out.println("Spawned Normal Zombie at (" + x + "," + y + ")");
            }
        } else if (wave >= 3 && wave <= 4) {
            if (zombieType < 15) {
                zombie = new TankZombie(x, y);
                System.out.println("Spawned Tank Zombie at (" + x + "," + y + ")");
            } else if (zombieType < 35) {
                zombie = new CrawlerZombie(x, y);
                System.out.println("Spawned Crawler Zombie at (" + x + "," + y + ")");
            } else {
                zombie = new NormalZombie(x, y);
                System.out.println("Spawned Normal Zombie at (" + x + "," + y + ")");
            }
        } else {
            if (wave >= 5 && zombieType < 15 + (wave - 5) * 2) {
                zombie = new AcidSpitterZombie(x, y);
                System.out.println("Spawned Acid Spitter Zombie at (" + x + "," + y + ")");
            } else if (zombieType < 30 + (wave - 3) * 3) {
                zombie = new TankZombie(x, y);
                System.out.println("Spawned Tank Zombie at (" + x + "," + y + ")");
            } else if (zombieType < 50) {
                zombie = new CrawlerZombie(x, y);
                System.out.println("Spawned Crawler Zombie at (" + x + "," + y + ")");
            } else {
                zombie = new NormalZombie(x, y);
                System.out.println("Spawned Normal Zombie at (" + x + "," + y + ")");
            }
        }

        zombies.add(zombie);
    }

    @Override
    public void run() {
        long lastTime = System.currentTimeMillis();

        while (running) {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - lastTime;

            update(elapsedTime);

            repaint();

            try {
                long sleepTime = Math.max(0, FRAME_TIME - (System.currentTimeMillis() - currentTime));
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            lastTime = currentTime;
        }
    }

    private void update(long elapsedTime) {
        if (showMainMenu) {
            return;
        }

        if (gamePaused) {
            return;
        }

        updatePlayer();

        if (!gameStarted) {
            updateBullets();
            updateExplosions();
            return;
        }

        if (player.getHealth() <= 0) {
            if (!gameOver) {
                gameOver = true;
                gameOverTime = System.currentTimeMillis();
                showRestartPrompt = true;
            }
            return;
        }

        if (wave < 1 && gameStarted) {
            System.out.println("WARNING: Wave number is invalid during gameplay: " + wave);
            wave = 1;
            startCurrentWave();
            return;
        }

        if (showingWaveMessage) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - waveMessageStartTime >= waveMessageDuration) {
                showingWaveMessage = false;
            }
        }

        if (waveCompleted) {
            long currentTime = System.currentTimeMillis();
            long timeElapsed = currentTime - waveCompletionTime;

            if (timeElapsed >= WAVE_TRANSITION_DELAY) {
                System.out.println("Starting next wave after transition delay");
                startNextWave();
            } else {
                updateZombies();
                updateBullets();
                updateExplosions();
            }
        } else {
            updateZombies();
            updateBullets();
            updateExplosions();
            updateAmmoDrops();

            checkWaveCompletion();

            if (zombiesRemaining > 0) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastZombieSpawnTime >= ZOMBIE_SPAWN_INTERVAL) {
                    spawnZombie();
                    zombiesRemaining--;
                    lastZombieSpawnTime = currentTime;
                    System.out.println("Spawned zombie, " + zombiesRemaining + " remaining to spawn");
                }
            }
        }
    }

    private void checkWaveCompletion() {
        if (!waveCompleted && zombies.isEmpty() && zombiesRemaining <= 0) {
            waveCompleted = true;
            waveCompletionTime = System.currentTimeMillis();

            System.out.println("Wave " + wave + " completed! Next wave starting in " + (WAVE_TRANSITION_DELAY / 1000)
                    + " seconds...");

            int waveBonus = wave * 50;
            score += waveBonus;
            System.out.println("Wave completion bonus: " + waveBonus + " points");

            if (wave % 3 == 0) {
                player.heal(30);
                System.out.println("Wave " + wave + " completed! Player healed by 30 health points. Current health: "
                        + player.getHealth());
            }

            if (wave >= 5) {
                player.heal(20);
                System.out.println("Wave " + wave + " completed! Player healed by 20 health points. Current health: "
                        + player.getHealth());
            }

            showWaveMessage("WAVE " + wave + " COMPLETED!", WAVE_TRANSITION_DELAY);
        }
    }

    private void updatePlayer() {
        player.update();

        if (gameOver) {
            return;
        }

        canShoot = gameStarted && !gameOver && !gamePaused && !showingWaveMessage && !waveCompleted;

        int dx = 0, dy = 0;

        if (gameStarted && !showingWaveMessage && !waveCompleted && !gamePaused) {
            if (keys[KeyEvent.VK_W] || keys[KeyEvent.VK_UP]) {
                dy -= 1;
            }
            if (keys[KeyEvent.VK_S] || keys[KeyEvent.VK_DOWN]) {
                dy += 1;
            }
            if (keys[KeyEvent.VK_A] || keys[KeyEvent.VK_LEFT]) {
                dx -= 1;
            }
            if (keys[KeyEvent.VK_D] || keys[KeyEvent.VK_RIGHT]) {
                dx += 1;
            }

            player.move(dx, dy, PANEL_WIDTH, PANEL_HEIGHT);
        }

        if (leftMousePressed && player.canShoot() && canShoot) {
            double baseAngle = Math.atan2(mouseY - player.getY(), mouseX - player.getX());

            String gunType = player.getCurrentWeaponName();

            if (gunType.equals("Shotgun")) {
                int pelletCount = 9;
                double spreadPerPellet = 5.0;
                double startAngle = baseAngle - Math.toRadians((pelletCount - 1) * spreadPerPellet / 2);

                for (int i = 0; i < pelletCount; i++) {
                    double pelletAngle = startAngle + Math.toRadians(i * spreadPerPellet);
                    Bullet bullet = new Bullet(player.getX(), player.getY(), pelletAngle);
                    bullet.setDamage(15);
                    bullets.add(bullet);
                }
            } else if (gunType.equals("RocketLauncher")) {
                Bullet rocket = new Bullet(player.getX(), player.getY(), baseAngle);
                rocket.setDamage(150);
                rocket.setSpeed(5.0);
                rocket.setExplosive(true);
                rocket.setExplosionRadius(300);
                bullets.add(rocket);
            } else if (gunType.equals("Sniper")) {
                Bullet bullet = new Bullet(player.getX(), player.getY(), baseAngle);
                bullet.setDamage(200);
                bullet.setPenetrating(true);
                bullets.add(bullet);
            } else {
                int deviationAngle = player.getCurrentGun().getSpreadAngle();

                double finalAngle = baseAngle;
                if (deviationAngle > 0) {
                    double randomDeviation = (random.nextDouble() - 0.5) * 2 * deviationAngle;
                    finalAngle = baseAngle + Math.toRadians(randomDeviation);
                }

                Bullet bullet = new Bullet(player.getX(), player.getY(), finalAngle);

                if (gunType.equals("Rifle")) {
                    bullet.setDamage(30);
                } else {
                    bullet.setDamage(50);
                }

                bullets.add(bullet);
            }
            player.shoot();
        }
    }

    private void updateZombies() {
        try {
            if (showingWaveMessage || gameOver || gamePaused || !gameStarted) {
                return;
            }

            Iterator<AbstractZombie> it = zombies.iterator();
            while (it.hasNext()) {
                AbstractZombie zombie = it.next();

                if (zombie.isAlive()) {
                    zombie.update();

                    zombie.moveTowards(player.getX(), player.getY());

                    zombie.useSpecialAbility(player.getX(), player.getY());

                    if (isColliding(zombie.getX(), zombie.getY(), 30, player.getX(), player.getY(), 30)) {
                        player.takeDamage(zombie.attack());
                        pushPlayerBack(zombie);
                    }

                    if (zombie instanceof AcidSpitterZombie) {
                        AcidSpitterZombie acidSpitter = (AcidSpitterZombie) zombie;
                        if (acidSpitter.isPlayerHitByAcid(player.getX(), player.getY(), 30)) {
                            player.takeDamage(acidSpitter.getAcidDamage());
                            System.out.println("Player hit by acid! Damage: " + acidSpitter.getAcidDamage());
                        }
                    } else if (zombie instanceof TankZombie) {
                        TankZombie tank = (TankZombie) zombie;
                        if (tank.isPlayerHitByGroundPound(player.getX(), player.getY())) {
                            player.takeDamage(tank.getGroundPoundDamage());
                            System.out.println("Player hit by ground pound! Damage: " + tank.getGroundPoundDamage());

                            double angle = Math.atan2(player.getY() - tank.getY(), player.getX() - tank.getX());
                            int knockbackForce = 15;
                            int knockbackX = (int) (Math.cos(angle) * knockbackForce);
                            int knockbackY = (int) (Math.sin(angle) * knockbackForce);

                            for (int i = 0; i < 5; i++) {
                                player.move(knockbackX, knockbackY, PANEL_WIDTH, PANEL_HEIGHT);
                            }
                        }

                        if (tank.isPlayerInGroundPoundArea(player.getX(), player.getY())) {
                            player.setSlowed(true);

                            if (random.nextInt(5) == 0) {
                                explosions.add(new Explosion(player.getX(), player.getY()));
                            }
                        } else {
                            player.setSlowed(false);
                        }
                    }

                    if (isZombieOutOfBounds(zombie)) {
                        respawnZombie(zombie);
                    }

                    preventZombieStacking(zombie);
                } else {
                    if (zombie instanceof AcidSpitterZombie) {
                        AcidSpitterZombie acidSpitter = (AcidSpitterZombie) zombie;
                        if (acidSpitter.isPlayerHitByDeathExplosion(player.getX(), player.getY())) {
                            player.takeDamage(acidSpitter.getDeathExplosionDamage());
                            System.out.println(
                                    "Player hit by acid explosion! Damage: " + acidSpitter.getDeathExplosionDamage());
                        }
                    }

                    it.remove();
                    zombiesKilled++;
                    score += 10;
                    System.out.println("Zombie killed, total killed: " + zombiesKilled);
                    if (Math.random() < 0.2 && (zombie instanceof NormalZombie)) { // 20% chance to drop ammo from
                                                                                   // NormalZombie
                        ammoDrops.add(new AmmoDrop(zombie.getX(), zombie.getY()));
                        System.out.println("Zombie drop ammo" + zombie.getX() + " " + zombie.getY());
                    } else if ((Math.random() < 0.35 && !(zombie instanceof NormalZombie))) { // %35 chance to drop ammo
                                                                                              // from
                                                                                              // other zombies
                        ammoDrops.add(new AmmoDrop(zombie.getX(), zombie.getY()));
                        System.out.println("Zombie drop ammo" + zombie.getX() + " " + zombie.getY());
                    }
                }
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("Warning: Concurrent modification in updateZombies");
        }
    }

    private void pushPlayerBack(AbstractZombie zombie) {
        double dx = player.getX() - zombie.getX();
        double dy = player.getY() - zombie.getY();
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length > 0) {
            dx = dx / length;
            dy = dy / length;

            int moveX = (int) Math.signum(dx);
            int moveY = (int) Math.signum(dy);

            for (int i = 0; i < 3; i++) {
                player.move(moveX, moveY, PANEL_WIDTH, PANEL_HEIGHT);
            }
        }
    }

    private void preventZombieStacking(AbstractZombie zombie1) {
        for (AbstractZombie zombie2 : zombies) {
            if (zombie1 == zombie2 || !zombie2.isAlive()) {
                continue;
            }

            if (isColliding(zombie1.getX(), zombie1.getY(), 25, zombie2.getX(), zombie2.getY(), 25)) {
                double dx = zombie1.getX() - zombie2.getX();
                double dy = zombie1.getY() - zombie2.getY();
                double length = Math.sqrt(dx * dx + dy * dy);

                if (length > 0) {
                    dx = dx / length * 2;
                    dy = dy / length * 2;

                    zombie1.setX(zombie1.getX() + (int) dx);
                    zombie1.setY(zombie1.getY() + (int) dy);

                    int x = Math.max(-50, Math.min(PANEL_WIDTH + 50, zombie1.getX()));
                    int y = Math.max(-50, Math.min(PANEL_HEIGHT + 50, zombie1.getY()));
                    zombie1.setX(x);
                    zombie1.setY(y);
                }
            }
        }
    }

    private boolean isZombieOutOfBounds(AbstractZombie zombie) {
        int x = zombie.getX();
        int y = zombie.getY();

        return x < -50 || x > PANEL_WIDTH + 50 || y < -50 || y > PANEL_HEIGHT + 50;
    }

    private void respawnZombie(AbstractZombie zombie) {
        int side = random.nextInt(4); // 0: top, 1: right, 2: bottom, 3: left
        int x, y;

        switch (side) {
            case 0: // Top
                x = random.nextInt(PANEL_WIDTH);
                y = SPAWN_MARGIN;
                break;
            case 1: // Right
                x = PANEL_WIDTH - SPAWN_MARGIN;
                y = random.nextInt(PANEL_HEIGHT);
                break;
            case 2: // Bottom
                x = random.nextInt(PANEL_WIDTH);
                y = PANEL_HEIGHT - SPAWN_MARGIN;
                break;
            case 3: // Left
                x = SPAWN_MARGIN;
                y = random.nextInt(PANEL_HEIGHT);
                break;
            default:
                x = SPAWN_MARGIN;
                y = SPAWN_MARGIN;
        }

        int playerX = player.getX();
        int playerY = player.getY();
        double distanceToPlayer = Math.sqrt(Math.pow(x - playerX, 2) + Math.pow(y - playerY, 2));

        if (distanceToPlayer < 250) {
            double angle = Math.atan2(y - playerY, x - playerX);
            x = (int) (playerX + Math.cos(angle) * 300);
            y = (int) (playerY + Math.sin(angle) * 300);

            x = Math.max(SPAWN_MARGIN, Math.min(PANEL_WIDTH - SPAWN_MARGIN, x));
            y = Math.max(SPAWN_MARGIN, Math.min(PANEL_HEIGHT - SPAWN_MARGIN, y));
        }

        zombie.setX(x);
        zombie.setY(y);
        System.out.println("Respawned zombie at (" + x + "," + y + ")");
    }

    private void updateBullets() {
        try {
            Iterator<Bullet> it = bullets.iterator();
            while (it.hasNext()) {
                Bullet bullet = it.next();
                bullet.update();

                if (bullet.getX() < 0 || bullet.getX() > PANEL_WIDTH ||
                        bullet.getY() < 0 || bullet.getY() > PANEL_HEIGHT) {
                    it.remove();
                    continue;
                }

                boolean bulletHit = false;
                List<AbstractZombie> hitZombies = new ArrayList<>();

                for (AbstractZombie zombie : zombies) {
                    if (zombie.isAlive()
                            && isColliding(bullet.getX(), bullet.getY(), 5, zombie.getX(), zombie.getY(), 30)) {
                        zombie.takeDamage(bullet.getDamage());

                        hitZombies.add(zombie);

                        explosions.add(new Explosion(bullet.getX(), bullet.getY()));

                        if (!bullet.isPenetrating()) {
                            bulletHit = true;
                            break;
                        }
                    }
                }

                if (bullet.isExplosive() && (bulletHit || !hitZombies.isEmpty())) {
                    Explosion bigExplosion = new Explosion(bullet.getX(), bullet.getY(), bullet.getDamage());
                    explosions.add(bigExplosion);
                }

                if (bulletHit || (!hitZombies.isEmpty() && !bullet.isPenetrating())) {
                    it.remove();
                }
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("Warning: Concurrent modification in updateBullets");
        }
    }

    private void updateExplosions() {
        Iterator<Explosion> it = explosions.iterator();
        while (it.hasNext()) {
            Explosion explosion = it.next();
            explosion.update();

            if (!explosion.isDamageApplied() && explosion.getDamage() > 0) {
                applyExplosionDamage(explosion);
                explosion.setDamageApplied(true);
            }

            if (explosion.isFinished()) {
                it.remove();
            }
        }
    }

    private void applyExplosionDamage(Explosion explosion) {
        int explosionX = explosion.getX();
        int explosionY = explosion.getY();
        int explosionRadius = explosion.getSize() * 6;
        int baseDamage = explosion.getDamage();

        for (AbstractZombie zombie : zombies) {
            if (!zombie.isAlive())
                continue;

            double dx = zombie.getX() - explosionX;
            double dy = zombie.getY() - explosionY;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance <= explosionRadius) {
                double falloff = Math.pow(1.0 - (distance / explosionRadius), 3);

                int actualDamage = (int) (baseDamage * falloff);
                actualDamage = Math.max((int) (baseDamage * 0.15), actualDamage);

                zombie.takeDamage(actualDamage);

                if (zombie.isAlive()) {
                    double angle = Math.atan2(dy, dx);
                    int knockbackForce = (int) (60 * falloff);
                    int knockbackX = (int) (Math.cos(angle) * knockbackForce);
                    int knockbackY = (int) (Math.sin(angle) * knockbackForce);

                    zombie.setX(zombie.getX() + knockbackX);
                    zombie.setY(zombie.getY() + knockbackY);

                    if (zombie.canBeStunned()) {
                        int stunDuration = (int) (1000 * falloff);
                        zombie.stun(stunDuration);
                    }
                }

                if (debugMode) {
                    System.out.println(
                            "Explosion damage: " + actualDamage + " to zombie at distance " + distance + " (falloff: "
                                    + String.format("%.2f", falloff) + ")");
                }
            }
        }

        double playerDx = player.getX() - explosionX;
        double playerDy = player.getY() - explosionY;
        double playerDistance = Math.sqrt(playerDx * playerDx + playerDy * playerDy);

        if (playerDistance <= explosionRadius) {
            double falloff = Math.pow(1.0 - (playerDistance / explosionRadius), 3);
            int actualDamage = (int) (baseDamage * falloff * 0.20);

            actualDamage = Math.max(3, actualDamage);

            player.takeDamage(actualDamage);

            double angle = Math.atan2(playerDy, playerDx);
            int knockbackForce = (int) (40 * falloff);
            int knockbackX = (int) (Math.cos(angle) * knockbackForce);
            int knockbackY = (int) (Math.sin(angle) * knockbackForce);

            for (int i = 0; i < 3; i++) {
                player.move(knockbackX, knockbackY, PANEL_WIDTH, PANEL_HEIGHT);
            }

            if (debugMode) {
                System.out
                        .println("Explosion self-damage: " + actualDamage + " to player at distance " + playerDistance);
            }
        }
    }

    private void drawExplosions(Graphics2D g2d) {
        try {
            for (Explosion explosion : explosions) {
                g2d.setColor(new Color(255, 100, 0, explosion.getAlpha()));
                g2d.fillOval(explosion.getX() - explosion.getSize() / 2,
                        explosion.getY() - explosion.getSize() / 2,
                        explosion.getSize(), explosion.getSize());

                if (explosion.getDamage() > 0) {
                    g2d.setColor(new Color(255, 200, 0, explosion.getAlpha() / 2));
                    int outerSize = explosion.getSize() * 3;
                    g2d.fillOval(explosion.getX() - outerSize / 2,
                            explosion.getY() - outerSize / 2,
                            outerSize, outerSize);

                    g2d.setColor(new Color(255, 255, 255, explosion.getAlpha() / 3));
                    g2d.setStroke(new BasicStroke(2));
                    int shockwaveSize = explosion.getSize() * 5;
                    g2d.drawOval(explosion.getX() - shockwaveSize / 2,
                            explosion.getY() - shockwaveSize / 2,
                            shockwaveSize, shockwaveSize);

                    g2d.setColor(new Color(100, 100, 100, explosion.getAlpha()));
                    Random random = new Random(explosion.getX() * explosion.getY());
                    for (int i = 0; i < 10; i++) {
                        double angle = random.nextDouble() * Math.PI * 2;
                        double distance = random.nextDouble() * explosion.getSize();
                        int debrisX = (int) (explosion.getX() + Math.cos(angle) * distance);
                        int debrisY = (int) (explosion.getY() + Math.sin(angle) * distance);
                        int debrisSize = 2 + random.nextInt(4);
                        g2d.fillRect(debrisX, debrisY, debrisSize, debrisSize);
                    }
                }
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("Warning: Concurrent modification in drawExplosions");
        }
    }

    private boolean isColliding(int x1, int y1, int r1, int x2, int y2, int r2) {
        int dx = x1 - x2;
        int dy = y1 - y2;
        int distance = (int) Math.sqrt(dx * dx + dy * dy);
        return distance < (r1 + r2);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (showMainMenu) {
            drawMainMenu(g2d);
            return;
        }

        if (showControls) {
            drawControlsScreen(g2d);
            return;
        }

        drawZombies(g2d);
        drawBullets(g2d);
        drawExplosions(g2d);
        drawPlayer(g2d);
        drawAmmoDrops(g2d);

        drawUI(g2d);

        if (gameStarted) {
            drawMinimap(g2d);
        }

        if (debugMode) {
            drawDebugInfo(g2d);
        }

        if (!gameStarted) {
            drawPregameMessage(g2d);
        }

        if (gamePaused) {
            drawPauseMenu(g2d);
        }
    }

    private void drawPlayer(Graphics2D g2d) {
        g2d.setColor(Color.BLUE);
        g2d.fillOval(player.getX() - 15, player.getY() - 15, 30, 30);

        g2d.setColor(Color.GRAY);
        double angle = Math.atan2(mouseY - player.getY(), mouseX - player.getX());
        int gunLength = 20;
        int gunEndX = player.getX() + (int) (Math.cos(angle) * gunLength);
        int gunEndY = player.getY() + (int) (Math.sin(angle) * gunLength);
        g2d.setStroke(new BasicStroke(5));
        g2d.drawLine(player.getX(), player.getY(), gunEndX, gunEndY);

        if (player.isSlowed()) {
            g2d.setColor(new Color(255, 0, 0, 80));
            g2d.fillOval(player.getX() - 20, player.getY() - 20, 40, 40);

            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString("SLOWED", player.getX() - 25, player.getY() - 25);
        }

        // Draw health bar under player
        int healthBarWidth = 40;
        int healthBarHeight = 5;
        int healthBarX = player.getX() - healthBarWidth / 2;
        int healthBarY = player.getY() + 20;

        // Draw background (red)
        g2d.setColor(Color.RED);
        g2d.fillRect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);

        // Draw health (green)
        g2d.setColor(Color.GREEN);
        int currentHealthWidth = (int) Math.min(healthBarWidth * (player.getHealth() / (double) player.getMaxHealth()),
                healthBarWidth);
        g2d.fillRect(healthBarX, healthBarY, currentHealthWidth, healthBarHeight);

        // Draw border
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);

        ////////////////////////////////////

        // Draw armor bar under player
        int armorBarWidth = 40;
        int armorBarHeight = 5;
        int armorBarX = player.getX() - armorBarWidth / 2;
        int armorBarY = player.getY() + 27;

        // Draw background (backgroundColor)
        g2d.setColor(getBackground());
        g2d.fillRect(armorBarX, armorBarY, armorBarWidth, armorBarHeight);

        // Draw armor (blue)
        g2d.setColor(Color.BLUE);
        int currentArmorWidth = (int) (armorBarWidth * ((player.getHealth() / (double) player.getMaxHealth()) - 1));
        g2d.fillRect(armorBarX, armorBarY, currentArmorWidth, armorBarHeight);

        // Draw border
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRect(armorBarX, armorBarY, armorBarWidth, armorBarHeight);

    }

    private void drawZombies(Graphics2D g2d) {
        try {
            for (AbstractZombie zombie : zombies) {
                if (zombie.isAlive()) {
                    if (zombie instanceof NormalZombie) {
                        g2d.setColor(Color.GREEN);
                    } else if (zombie instanceof CrawlerZombie) {
                        g2d.setColor(Color.YELLOW);
                    } else if (zombie instanceof TankZombie) {
                        g2d.setColor(Color.RED);
                    } else if (zombie instanceof AcidSpitterZombie) {
                        g2d.setColor(Color.MAGENTA);
                    }

                    g2d.fillOval(zombie.getX() - 15, zombie.getY() - 15, 30, 30);

                    g2d.setColor(Color.RED);
                    g2d.fillRect(zombie.getX() - 15, zombie.getY() - 25, 30, 5);

                    g2d.setColor(Color.GREEN);

                    int maxHealth = AbstractZombie.HIGH_HEALTH;
                    if (zombie instanceof NormalZombie) {
                        maxHealth = AbstractZombie.MEDIUM_HEALTH;
                    } else if (zombie instanceof CrawlerZombie) {
                        maxHealth = AbstractZombie.LOW_HEALTH;
                    } else if (zombie instanceof TankZombie) {
                        maxHealth = AbstractZombie.HIGH_HEALTH;
                    } else if (zombie instanceof AcidSpitterZombie) {
                        maxHealth = AbstractZombie.LOW_HEALTH;
                    }

                    int healthWidth = (int) (30 * (zombie.getHealth() / (double) maxHealth));
                    g2d.fillRect(zombie.getX() - 15, zombie.getY() - 25, healthWidth, 5);

                    if (zombie instanceof AcidSpitterZombie) {
                        AcidSpitterZombie acidSpitter = (AcidSpitterZombie) zombie;
                        if (acidSpitter.isAcidSpitActive()) {
                            // acid
                            g2d.setColor(new Color(0, 255, 0, 150));
                            g2d.fillOval(acidSpitter.getAcidX() - 20, acidSpitter.getAcidY() - 20, 40, 40);

                            // acid splash
                            g2d.setColor(new Color(0, 255, 0, 100));
                            for (int i = 0; i < 8; i++) {
                                double angle = i * Math.PI / 4;
                                int particleX = acidSpitter.getAcidX() + (int) (Math.cos(angle) * 25);
                                int particleY = acidSpitter.getAcidY() + (int) (Math.sin(angle) * 25);
                                g2d.fillOval(particleX - 5, particleY - 5, 10, 10);
                            }
                        }
                    } else if (zombie instanceof CrawlerZombie) {
                        CrawlerZombie crawler = (CrawlerZombie) zombie;
                        if (crawler.isJumping()) {
                            // jump
                            g2d.setColor(new Color(0, 0, 0, 80));
                            g2d.fillOval(zombie.getX() - 15, zombie.getY() - 5, 30, 10);

                            g2d.setColor(Color.WHITE);
                            g2d.setStroke(new BasicStroke(2));
                            for (int i = 0; i < 4; i++) {
                                double angle = i * Math.PI / 2;
                                int startX = zombie.getX() + (int) (Math.cos(angle) * 20);
                                int startY = zombie.getY() + (int) (Math.sin(angle) * 20);
                                int endX = zombie.getX() + (int) (Math.cos(angle) * 30);
                                int endY = zombie.getY() + (int) (Math.sin(angle) * 30);
                                g2d.drawLine(startX, startY, endX, endY);
                            }
                            g2d.setColor(new Color(255, 255, 0, 50));
                            g2d.fillOval(zombie.getX() - 25, zombie.getY() - 25, 50, 50);
                        }
                    } else if (zombie instanceof TankZombie) {
                        TankZombie tank = (TankZombie) zombie;

                        // tank zombie
                        g2d.setColor(new Color(100, 0, 0, 100));
                        g2d.fillOval(zombie.getX() - 20, zombie.getY() - 20, 40, 40);

                        // ground pound effect
                        if (tank.isGroundPounding()) {
                            // shockwave circles
                            g2d.setColor(new Color(255, 0, 0, 150));
                            g2d.setStroke(new BasicStroke(3));

                            int range = tank.getGroundPoundRange();
                            g2d.drawOval(zombie.getX() - range, zombie.getY() - range, range * 2, range * 2);

                            // inner shockwaves
                            g2d.setColor(new Color(255, 0, 0, 80));
                            g2d.setStroke(new BasicStroke(2));
                            g2d.drawOval(zombie.getX() - range / 2, zombie.getY() - range / 2, range, range);
                            g2d.drawOval(zombie.getX() - range / 4, zombie.getY() - range / 4, range / 2, range / 2);

                            // cracks
                            g2d.setColor(new Color(139, 69, 19));
                            g2d.setStroke(new BasicStroke(3));
                            for (int i = 0; i < 8; i++) {
                                double angle = i * Math.PI / 4;
                                int startX = zombie.getX();
                                int startY = zombie.getY();
                                int endX = zombie.getX() + (int) (Math.cos(angle) * range / 2);
                                int endY = zombie.getY() + (int) (Math.sin(angle) * range / 2);
                                g2d.drawLine(startX, startY, endX, endY);
                            }
                        }
                    }
                }
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("Warning: Concurrent modification in drawZombies");
        }
    }

    private void drawBullets(Graphics2D g2d) {
        try {
            for (Bullet bullet : bullets) {
                // different bullet type
                if (bullet.isExplosive()) {
                    // Rocket
                    g2d.setColor(Color.RED);
                    g2d.fillOval(bullet.getX() - 4, bullet.getY() - 4, 8, 8);

                    g2d.setColor(new Color(255, 140, 0, 150));
                    double angle = Math.atan2(bullet.getY() - player.getY(), bullet.getX() - player.getX());
                    int trailLength = 15;
                    int trailX = (int) (bullet.getX() - Math.cos(angle) * trailLength);
                    int trailY = (int) (bullet.getY() - Math.sin(angle) * trailLength);
                    g2d.setStroke(new BasicStroke(3));
                    g2d.drawLine(bullet.getX(), bullet.getY(), trailX, trailY);

                    g2d.setColor(new Color(255, 255, 0, 100));
                    for (int i = 0; i < 3; i++) {
                        int particleSize = 4 - i;
                        int particleDistance = 5 + (i * 4);
                        int particleX = (int) (bullet.getX() - Math.cos(angle) * particleDistance);
                        int particleY = (int) (bullet.getY() - Math.sin(angle) * particleDistance);
                        g2d.fillOval(particleX - particleSize / 2, particleY - particleSize / 2, particleSize,
                                particleSize);
                    }
                } else if (bullet.isPenetrating()) {
                    // Sniper
                    g2d.setColor(new Color(255, 255, 100));

                    double angle = Math.atan2(bullet.getY() - player.getY(), bullet.getX() - player.getX());
                    int bulletLength = 12;
                    int endX = (int) (bullet.getX() + Math.cos(angle) * bulletLength);
                    int endY = (int) (bullet.getY() + Math.sin(angle) * bulletLength);

                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawLine(bullet.getX(), bullet.getY(), endX, endY);

                    g2d.fillOval(bullet.getX() - 2, bullet.getY() - 2, 4, 4);
                } else {
                    // Standard
                    g2d.setColor(Color.ORANGE);
                    g2d.fillOval(bullet.getX() - 2, bullet.getY() - 2, 5, 5);
                }
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("Warning: Concurrent modification in drawBullets");
        }
    }

    private void drawMinimap(Graphics2D g2d) {
        // minimap
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(MINIMAP_X, MINIMAP_Y, MINIMAP_SIZE, MINIMAP_SIZE);

        // minimap border
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(MINIMAP_X, MINIMAP_Y, MINIMAP_SIZE, MINIMAP_SIZE);

        int minimapCenterX = MINIMAP_X + MINIMAP_SIZE / 2;
        int minimapCenterY = MINIMAP_Y + MINIMAP_SIZE / 2;

        // Draw player on minimap
        g2d.setColor(Color.BLUE);
        int playerMinimapX = minimapCenterX;
        int playerMinimapY = minimapCenterY;
        g2d.fillOval(playerMinimapX - 3, playerMinimapY - 3, 6, 6);

        // Draw zombies on minimap
        try {
            for (AbstractZombie zombie : zombies) {
                if (zombie.isAlive()) {
                    int relX = zombie.getX() - player.getX();
                    int relY = zombie.getY() - player.getY();

                    int zombieMinimapX = minimapCenterX + (int) (relX * MINIMAP_SCALE);
                    int zombieMinimapY = minimapCenterY + (int) (relY * MINIMAP_SCALE);

                    if (zombie instanceof NormalZombie) {
                        g2d.setColor(Color.GREEN);
                    } else if (zombie instanceof CrawlerZombie) {
                        g2d.setColor(Color.YELLOW);
                    } else if (zombie instanceof TankZombie) {
                        g2d.setColor(Color.RED);
                    } else if (zombie instanceof AcidSpitterZombie) {
                        g2d.setColor(Color.MAGENTA);
                    }

                    if (zombieMinimapX >= MINIMAP_X && zombieMinimapX <= MINIMAP_X + MINIMAP_SIZE &&
                            zombieMinimapY >= MINIMAP_Y && zombieMinimapY <= MINIMAP_Y + MINIMAP_SIZE) {
                        g2d.fillOval(zombieMinimapX - 2, zombieMinimapY - 2, 4, 4);
                    }
                }
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("Warning: Concurrent modification in drawMinimap");
        }
    }

    private void drawHealth(Graphics2D g2d) {
        g2d.setColor(Color.RED);
        g2d.fillRect(20, 75, 100, 20);
        g2d.setColor(Color.GREEN);
        int healthWidth = (int) (100 * (player.getHealth() / (double) player.getMaxHealth()));
        g2d.fillRect(20, 75, healthWidth, 20);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(20, 75, 100, 20);
        if (player.getHealth() > 100) {
            g2d.drawString("HP: " + 100, 40, 90);
        } else {
            g2d.drawString("HP: " + player.getHealth(), 40, 90);
        }

    }

    private void drawArmor(Graphics2D g2d) {
        if (player.getHealth() > 100) {
            int playerArmor = player.getHealth() - 100;
            g2d.setColor(getBackground());
            g2d.fillRect(120, 75, 100, 20);
            g2d.setColor(Color.BLUE);
            int armorWidth = (int) (100 * (playerArmor / 100.0));
            g2d.fillRect(120, 75, armorWidth, 20);
            g2d.setColor(Color.WHITE);
            g2d.drawRect(120, 75, 100, 20);
            g2d.drawString("Armor: " + playerArmor, 140, 90);
        }

    }

    private void drawUI(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));

        if (gameStarted) {
            g2d.drawString("Score: " + score, 20, 30);
            g2d.drawString("Wave: " + wave, 20, 50);
            g2d.drawString("Zombies: " + (zombies.size() + zombiesRemaining), 20, 70);
            drawHealth(g2d);
            drawArmor(g2d);
            if (player.getCurrentWeaponName() == "Pistol") {
                g2d.drawString("Ammo: " + player.getAmmo() + "/∞", 20, 110);
            } else {
                g2d.drawString("Ammo: " + player.getAmmo() + "/" + player.getTotalAmmo(), 20, 110);
            }

            g2d.drawString("Weapon: " + player.getCurrentWeaponName(), 20, 130);
        }

        if (player.isReloading()) {
            g2d.setColor(Color.YELLOW);
            g2d.drawString("RELOADING...", PANEL_WIDTH / 2 - 50, 30);
        }

        if (gameStarted && waveCompleted) {
            g2d.setColor(Color.DARK_GRAY);
            g2d.fillRect(PANEL_WIDTH / 2 - 200, PANEL_HEIGHT / 2 - 60, 400, 150);

            g2d.setColor(Color.GREEN);
            g2d.setFont(new Font("Arial", Font.BOLD, 28));

            if (wave % 3 == 0 && wave >= 5) {
                g2d.drawString("WAVE " + wave + " COMPLETED!", PANEL_WIDTH / 2 - 160, PANEL_HEIGHT / 2 - 30);
                g2d.setFont(new Font("Arial", Font.BOLD, 24));
                g2d.drawString("+50 HEALTH!", PANEL_WIDTH / 2 - 140, PANEL_HEIGHT / 2 + 10);
            } else if (wave % 3 == 0) {
                g2d.drawString("WAVE " + wave + " COMPLETED!", PANEL_WIDTH / 2 - 160, PANEL_HEIGHT / 2 - 30);
                g2d.setFont(new Font("Arial", Font.BOLD, 24));
                g2d.drawString("+30 HEALTH!", PANEL_WIDTH / 2 - 80, PANEL_HEIGHT / 2 + 10);
            } else if (wave >= 5) {
                g2d.drawString("WAVE " + wave + " COMPLETED!", PANEL_WIDTH / 2 - 160, PANEL_HEIGHT / 2 - 30);
                g2d.setFont(new Font("Arial", Font.BOLD, 24));
                g2d.drawString("+20 HEALTH!", PANEL_WIDTH / 2 - 80, PANEL_HEIGHT / 2 + 10);
            } else {
                g2d.drawString("WAVE " + wave + " COMPLETED!", PANEL_WIDTH / 2 - 160, PANEL_HEIGHT / 2 - 30);
                g2d.setFont(new Font("Arial", Font.BOLD, 24));
            }

            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            long timeElapsed = System.currentTimeMillis() - waveCompletionTime;
            long timeLeft = Math.max(0, (WAVE_TRANSITION_DELAY - timeElapsed) / 1000);
            g2d.drawString("Next wave in " + timeLeft + " seconds", PANEL_WIDTH / 2 - 120, PANEL_HEIGHT / 2 + 50);

            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.setColor(Color.YELLOW);
            g2d.drawString("Press R to reload | 1-5 to switch weapons", PANEL_WIDTH / 2 - 150, PANEL_HEIGHT / 2 + 80);
        } else if (showingWaveMessage) {
            g2d.setColor(Color.DARK_GRAY);
            g2d.fillRect(PANEL_WIDTH / 2 - 200, PANEL_HEIGHT / 2 - 50, 400, 100);

            g2d.setColor(Color.GREEN);
            g2d.setFont(new Font("Arial", Font.BOLD, 28));
            g2d.drawString(waveMessage, PANEL_WIDTH / 2 - 150, PANEL_HEIGHT / 2);

            if (waveMessage.contains("COMPLETED")) {
                g2d.setFont(new Font("Arial", Font.BOLD, 18));
                g2d.drawString("Next wave in " + ((WAVE_TRANSITION_DELAY -
                        (System.currentTimeMillis() - waveMessageStartTime)) / 1000) + " seconds",
                        PANEL_WIDTH / 2 - 120, PANEL_HEIGHT / 2 + 30);
            }
        }

        if (gameOver) {
            g2d.setColor(new Color(0, 0, 0, 200));
            g2d.fillRect(PANEL_WIDTH / 2 - 150, PANEL_HEIGHT / 2 - 50, 300, 150);

            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 36));
            g2d.drawString("GAME OVER", PANEL_WIDTH / 2 - 120, PANEL_HEIGHT / 2);

            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            g2d.drawString("Final Score: " + score, PANEL_WIDTH / 2 - 70, PANEL_HEIGHT / 2 + 40);

            if (showRestartPrompt && System.currentTimeMillis() - gameOverTime > 2000) {
                g2d.setColor(Color.WHITE);
                g2d.drawString("Press 'R' to play again", PANEL_WIDTH / 2 - 100, PANEL_HEIGHT / 2 + 80);
            }
        }
    }

    private void drawDebugInfo(Graphics2D g2d) {
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Monospaced", Font.PLAIN, 12));

        int y = 150;
        g2d.drawString("DEBUG INFO:", 20, y);
        y += 15;
        g2d.drawString("Wave: " + wave, 20, y);
        y += 15;
        g2d.drawString("Zombies Active: " + zombies.size(), 20, y);
        y += 15;
        g2d.drawString("Zombies Remaining: " + zombiesRemaining, 20, y);
        y += 15;

        if (waveCompleted) {
            long timeElapsed = System.currentTimeMillis() - waveCompletionTime;
            long timeLeft = WAVE_TRANSITION_DELAY - timeElapsed;
            g2d.drawString("Next Wave In: " + (timeLeft / 1000) + "." + ((timeLeft % 1000) / 100) + " seconds", 20, y);
            y += 15;
        } else {
            long waveElapsed = System.currentTimeMillis() - waveStartTime;
            g2d.drawString("Wave Time: " + (waveElapsed / 1000) + " seconds", 20, y);
            y += 15;
        }

        g2d.drawString("Available Weapons:", 20, y);
        y += 15;
        int[] weaponSlots = player.getAvailableWeaponSlots();
        for (int slot : weaponSlots) {
            String weaponInfo = slot + ": " + (player.hasWeapon(slot) ? "Yes" : "No");
            g2d.drawString(weaponInfo, 20, y);
            y += 15;
        }
        g2d.drawString("Current Weapon: " + player.getCurrentWeaponName(), 20, y);
        y += 15;

        g2d.setColor(new Color(255, 255, 255, 50)); // Very transparent white
        g2d.drawRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
    }

    private void drawPregameMessage(Graphics2D g2d) {
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(PANEL_WIDTH / 2 - 200, PANEL_HEIGHT / 2 - 100, 400, 220);

        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        g2d.drawString("ZOMBIE SHOOTER", PANEL_WIDTH / 2 - 160, PANEL_HEIGHT / 2 - 50);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.drawString("Press SPACE to start the game", PANEL_WIDTH / 2 - 130, PANEL_HEIGHT / 2);

        g2d.setColor(Color.WHITE);
        g2d.drawString("WASD or Arrow Keys to move", PANEL_WIDTH / 2 - 130, PANEL_HEIGHT / 2 + 60);
        g2d.drawString("Mouse to aim and shoot", PANEL_WIDTH / 2 - 130, PANEL_HEIGHT / 2 + 90);
        g2d.drawString("R to reload, 1-5 to switch weapons", PANEL_WIDTH / 2 - 130, PANEL_HEIGHT / 2 + 120);
    }

    private void drawPauseMenu(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        g2d.drawString("GAME PAUSED", PANEL_WIDTH / 2 - 140, PANEL_HEIGHT / 2 - 100);

        g2d.setFont(new Font("Arial", Font.BOLD, 24));

        int resumeY = PANEL_HEIGHT / 2 - 20;
        if (isPauseMenuOptionHovered(mouseX, mouseY, resumeY)) {
            g2d.setColor(new Color(255, 255, 0, 200)); // Brighter yellow when hovered
        } else {
            g2d.setColor(Color.YELLOW);
        }
        g2d.drawString("Resume Game (ESC or Click)", PANEL_WIDTH / 2 - 150, resumeY);

        int saveY = PANEL_HEIGHT / 2 + 20;
        if (isPauseMenuOptionHovered(mouseX, mouseY, saveY)) {
            g2d.setColor(new Color(0, 255, 0, 200)); // Brighter green when hovered
        } else {
            g2d.setColor(Color.GREEN);
        }
        g2d.drawString("Save Game (S or Click)", PANEL_WIDTH / 2 - 120, saveY);

        int loadY = PANEL_HEIGHT / 2 + 60;
        if (isPauseMenuOptionHovered(mouseX, mouseY, loadY)) {
            g2d.setColor(new Color(0, 255, 255, 200)); // Brighter cyan when hovered
        } else {
            g2d.setColor(Color.CYAN);
        }
        g2d.drawString("Load Game (L or Click)", PANEL_WIDTH / 2 - 120, loadY);

        int menuY = PANEL_HEIGHT / 2 + 100;
        if (isPauseMenuOptionHovered(mouseX, mouseY, menuY)) {
            g2d.setColor(new Color(255, 0, 255, 200)); // Brighter magenta when hovered
        } else {
            g2d.setColor(Color.MAGENTA);
        }
        g2d.drawString("Return to Main Menu (M or Click)", PANEL_WIDTH / 2 - 170, menuY);

        int quitY = PANEL_HEIGHT / 2 + 140;
        if (isPauseMenuOptionHovered(mouseX, mouseY, quitY)) {
            g2d.setColor(new Color(255, 0, 0, 200)); // Brighter red when hovered
        } else {
            g2d.setColor(Color.RED);
        }
        g2d.drawString("Quit Game (Q or Click)", PANEL_WIDTH / 2 - 120, quitY);
    }

    private boolean isPauseMenuOptionHovered(int mouseX, int mouseY, int optionY) {
        int menuX = PANEL_WIDTH / 2 - 170; // Wider area to account for longer text
        int menuWidth = 340;

        return mouseX >= menuX && mouseX <= menuX + menuWidth &&
                mouseY >= optionY - 25 && mouseY <= optionY + 5;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        keys[e.getKeyCode()] = true;

        if (showMainMenu) {
            handleMainMenuInput(e);
            return;
        }

        if (showControls) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                showControls = false;
                showMainMenu = true;
            }
            return;
        }

        if (e.getKeyCode() == KeyEvent.VK_F3) {
            debugMode = !debugMode;
            System.out.println("Debug mode " + (debugMode ? "enabled" : "disabled"));
        }

        if (e.getKeyCode() == KeyEvent.VK_ESCAPE && gameStarted && !gameOver) {
            gamePaused = !gamePaused;
            System.out.println("Game " + (gamePaused ? "paused" : "resumed"));
            return;
        }

        if (gamePaused) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_S: // Save game
                    saveGame();
                    return;
                case KeyEvent.VK_L: // Load game
                    loadGame();
                    return;
                case KeyEvent.VK_M: // main menu
                    showMainMenu = true;
                    gamePaused = false;
                    return;
                case KeyEvent.VK_Q: // Quit game
                    System.exit(0);
                    return;
            }
        }

        if (!gameStarted && e.getKeyCode() == KeyEvent.VK_SPACE) {
            gameStarted = true;
            showMainMenu = false;
            System.out.println("Game started by pressing SPACE");

            if (wave > 0) {
                System.out.println("Starting wave " + wave);
                startCurrentWave();
            } else {
                startNextWave();
            }
        }

        if (gameOver && showRestartPrompt && e.getKeyCode() == KeyEvent.VK_R) {
            restartGame();
            return;
        }

        if (!gameOver && !gamePaused && e.getKeyCode() == KeyEvent.VK_R
                && player.getAmmo() != player.getCurrentGun().getMaxAmmo() && !player.isReloading()) {
            System.out.println("Reloading");
            player.reload();
        }

        if (!gameOver && !gamePaused) {
            if (e.getKeyCode() >= KeyEvent.VK_1 && e.getKeyCode() <= KeyEvent.VK_5) {
                int weaponSlot = e.getKeyCode() - KeyEvent.VK_0;
                if (player.hasWeapon(weaponSlot)) {
                    if (player.switchWeapon(weaponSlot)) {
                        System.out.println("Switched to " + player.getCurrentWeaponName());
                    }
                }
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keys[e.getKeyCode()] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            leftMousePressed = true;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            leftMousePressed = false;
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (showMainMenu) {
            updateMenuSelectionFromMouse(e.getX(), e.getY());
            selectMenuOption();
        } else if (showControls) {
            int backY = 580;
            int backButtonX = PANEL_WIDTH / 2 - 180;
            int backButtonWidth = 360;

            if (e.getX() >= backButtonX && e.getX() <= backButtonX + backButtonWidth &&
                    e.getY() >= backY - 30 && e.getY() <= backY + 10) {
                showControls = false;
                showMainMenu = true;
            }
        } else if (gamePaused) {
            handlePauseMenuClick(e.getX(), e.getY());
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();

        if (showMainMenu) {
            updateMenuSelectionFromMouse(mouseX, mouseY);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        if ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
            leftMousePressed = true;
        }
    }

    private void restartGame() {
        gameOver = false;
        showRestartPrompt = false;
        score = 0;
        wave = 0;
        zombiesKilled = 0;
        zombiesRemaining = 0;

        zombies.clear();
        bullets.clear();
        explosions.clear();
        ammoDrops.clear();

        player = new Player(PANEL_WIDTH / 2, PANEL_HEIGHT / 2);

        gameStarted = true;

        System.out.println("Restarting game with wave reset to " + wave);
        startNextWave();

        System.out.println("Game restarted");
    }

    private void saveGame() {
        try {
            File saveDir = new File("saves");
            if (!saveDir.exists()) {
                saveDir.mkdir();
            }

            FileOutputStream fileOut = new FileOutputStream("saves/game_save.dat");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);

            GameState gameState = new GameState();
            gameState.score = this.score;
            gameState.wave = this.wave;
            gameState.zombiesKilled = this.zombiesKilled;
            gameState.zombiesRemaining = this.zombiesRemaining;
            gameState.playerHealth = player.getHealth();
            gameState.playerX = player.getX();
            gameState.playerY = player.getY();
            gameState.playerWeapons = player.getAvailableWeaponSlots();
            gameState.currentWeaponSlot = player.getCurrentWeaponSlot();

            out.writeObject(gameState);
            out.close();
            fileOut.close();

            System.out.println("Game saved successfully");
        } catch (IOException e) {
            System.out.println("Error saving game: " + e.getMessage());
        }
    }

    private void loadGame() {
        try {
            File saveFile = new File("saves/game_save.dat");
            if (!saveFile.exists()) {
                System.out.println("No save file found");
                if (showMainMenu) {
                    return;
                }
                return;
            }

            FileInputStream fileIn = new FileInputStream("saves/game_save.dat");
            ObjectInputStream in = new ObjectInputStream(fileIn);

            GameState gameState = (GameState) in.readObject();
            in.close();
            fileIn.close();

            this.score = gameState.score;
            this.wave = gameState.wave;
            System.out.println("Loaded game with wave " + this.wave);
            this.zombiesKilled = gameState.zombiesKilled;
            this.zombiesRemaining = gameState.zombiesRemaining;

            zombies.clear();
            bullets.clear();
            explosions.clear();
            ammoDrops.clear();

            player = new Player(gameState.playerX, gameState.playerY);
            player.setHealth(gameState.playerHealth);

            for (int slot : gameState.playerWeapons) {
                switch (slot) {
                    case 2:
                        player.addWeapon(2, new Guns.Rifle());
                        break;
                    case 3:
                        player.addWeapon(3, new Guns.Shotgun());
                        break;
                    case 4:
                        player.addWeapon(4, new Guns.Sniper());
                        break;
                    case 5:
                        player.addWeapon(5, new Guns.RocketLauncher());
                        break;
                }
            }

            player.switchWeapon(gameState.currentWeaponSlot);

            gameStarted = true;
            gamePaused = false;
            gameOver = false;
            waveCompleted = false;
            showMainMenu = false;
            showControls = false;

            waveStartTime = System.currentTimeMillis();
            lastZombieSpawnTime = waveStartTime;

            startCurrentWave();

            System.out.println("Game loaded successfully");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error loading game: " + e.getMessage());

            if (showMainMenu) {
                return;
            }
        }
    }

    private void showWaveMessage(String message, long duration) {
        showingWaveMessage = true;
        waveMessage = message;
        waveMessageStartTime = System.currentTimeMillis();
        waveMessageDuration = duration;
    }

    static class GameState implements Serializable {
        private static final long serialVersionUID = 1L;

        int score;
        int wave;
        int zombiesKilled;
        int zombiesRemaining;
        int playerHealth;
        int playerX;
        int playerY;
        int[] playerWeapons;
        int currentWeaponSlot;
    }

    private void drawMainMenu(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0));
        g2d.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Arial", Font.BOLD, 60));
        g2d.drawString("ZOMBIE SHOOTER", PANEL_WIDTH / 2 - 250, 150);

        g2d.setFont(new Font("Arial", Font.BOLD, 30));

        int menuX = PANEL_WIDTH / 2 - 100;
        int menuWidth = 200;
        int menuHeight = 40;

        int startGameY = 280;
        if (menuSelection == 0) {
            g2d.setColor(new Color(100, 100, 0));
            g2d.fillRect(menuX - 10, startGameY - 25, menuWidth, menuHeight);
            g2d.setColor(Color.YELLOW);
        } else {
            g2d.setColor(Color.WHITE);
        }
        g2d.drawString("Start Game", menuX, startGameY);

        int loadGameY = 330;
        if (menuSelection == 1) {
            g2d.setColor(new Color(100, 100, 0));
            g2d.fillRect(menuX - 10, loadGameY - 25, menuWidth, menuHeight);
            g2d.setColor(Color.YELLOW);
        } else {
            g2d.setColor(Color.WHITE);
        }
        g2d.drawString("Load Game", menuX, loadGameY);

        int controlsY = 380;
        if (menuSelection == 2) {
            g2d.setColor(new Color(100, 100, 0));
            g2d.fillRect(menuX - 10, controlsY - 25, menuWidth, menuHeight);
            g2d.setColor(Color.YELLOW);
        } else {
            g2d.setColor(Color.WHITE);
        }
        g2d.drawString("Controls", menuX, controlsY);

        int quitY = 430;
        if (menuSelection == 3) {
            g2d.setColor(new Color(100, 100, 0));
            g2d.fillRect(menuX - 10, quitY - 25, menuWidth, menuHeight);
            g2d.setColor(Color.YELLOW);
        } else {
            g2d.setColor(Color.WHITE);
        }
        g2d.drawString("Quit", menuX, quitY);

        g2d.setColor(Color.GRAY);
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        g2d.drawString("Use UP/DOWN arrows or mouse to navigate", PANEL_WIDTH / 2 - 180, 550);
        g2d.drawString("Press ENTER or click to select", PANEL_WIDTH / 2 - 130, 580);

        g2d.setColor(Color.GRAY);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("Version 1.0", 20, PANEL_HEIGHT - 20);
    }

    private void drawControlsScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0));
        g2d.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD, 40));
        g2d.drawString("CONTROLS", PANEL_WIDTH / 2 - 100, 100);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));

        int y = 180;
        int spacing = 40;

        g2d.drawString("Movement: WASD or Arrow Keys", PANEL_WIDTH / 2 - 200, y);
        y += spacing;

        g2d.drawString("Aim: Mouse", PANEL_WIDTH / 2 - 200, y);
        y += spacing;

        g2d.drawString("Shoot: Left Mouse Button", PANEL_WIDTH / 2 - 200, y);
        y += spacing;

        g2d.drawString("Reload: R", PANEL_WIDTH / 2 - 200, y);
        y += spacing;

        g2d.drawString("Switch Weapons: 1-5 Number Keys", PANEL_WIDTH / 2 - 200, y);
        y += spacing;

        g2d.drawString("Pause: ESC", PANEL_WIDTH / 2 - 200, y);
        y += spacing;

        g2d.drawString("Save Game: S (while paused)", PANEL_WIDTH / 2 - 200, y);
        y += spacing;

        g2d.drawString("Load Game: L (while paused)", PANEL_WIDTH / 2 - 200, y);
        y += spacing;

        g2d.drawString("Toggle Debug: F3", PANEL_WIDTH / 2 - 200, y);
        y += spacing + 20;

        int backY = y + 40;

        g2d.setColor(new Color(50, 50, 50));
        int backButtonX = PANEL_WIDTH / 2 - 180;
        int backButtonWidth = 400;
        int backButtonHeight = 40;
        g2d.fillRect(backButtonX, backY - 30, backButtonWidth, backButtonHeight);

        g2d.setColor(Color.YELLOW);
        g2d.drawRect(backButtonX, backY - 30, backButtonWidth, backButtonHeight);

        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.drawString("Back to Main Menu (ESC or Click)", PANEL_WIDTH / 2 - 175, backY);
    }

    private void handleMainMenuInput(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                menuSelection = (menuSelection - 1 + 4) % 4;
                break;

            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                menuSelection = (menuSelection + 1) % 4;
                break;

            case KeyEvent.VK_ENTER:
            case KeyEvent.VK_SPACE:
                selectMenuOption();
                break;
        }
    }

    private void selectMenuOption() {
        switch (menuSelection) {
            case 0: // Start Game
                showMainMenu = false;
                gameStarted = false;
                initializeGame();
                break;

            case 1: // Load Game
                loadGame();
                showMainMenu = false;
                break;

            case 2: // Controls
                showMainMenu = false;
                showControls = true;
                break;

            case 3: // Quit
                System.exit(0);
                break;
        }
    }

    private boolean isMouseOverOption(int mouseX, int mouseY, int optionY) {
        int menuX = PANEL_WIDTH / 2 - 100;
        int menuWidth = 200;

        return mouseX >= menuX && mouseX <= menuX + menuWidth &&
                mouseY >= optionY - 30 && mouseY <= optionY + 10;
    }

    private void updateMenuSelectionFromMouse(int mouseX, int mouseY) {
        if (isMouseOverOption(mouseX, mouseY, 280)) {
            menuSelection = 0; // Start Game
        } else if (isMouseOverOption(mouseX, mouseY, 330)) {
            menuSelection = 1; // Load Game
        } else if (isMouseOverOption(mouseX, mouseY, 380)) {
            menuSelection = 2; // Controls
        } else if (isMouseOverOption(mouseX, mouseY, 430)) {
            menuSelection = 3; // Quit
        }
    }

    private void handlePauseMenuClick(int mouseX, int mouseY) {
        // Resume option
        if (isPauseMenuOptionHovered(mouseX, mouseY, PANEL_HEIGHT / 2 - 20)) {
            gamePaused = false;
            System.out.println("Game resumed via mouse click");
        }
        // Save option
        else if (isPauseMenuOptionHovered(mouseX, mouseY, PANEL_HEIGHT / 2 + 20)) {
            saveGame();
        }
        // Load option
        else if (isPauseMenuOptionHovered(mouseX, mouseY, PANEL_HEIGHT / 2 + 60)) {
            loadGame();
        }
        // Main menu option
        else if (isPauseMenuOptionHovered(mouseX, mouseY, PANEL_HEIGHT / 2 + 100)) {
            showMainMenu = true;
            gamePaused = false;
        }
        // Quit option
        else if (isPauseMenuOptionHovered(mouseX, mouseY, PANEL_HEIGHT / 2 + 140)) {
            System.exit(0);
        }
    }

    private void updateAmmoDrops() {
        if (showingWaveMessage || gameOver || gamePaused || !gameStarted) {
            return;
        }

        Iterator<AmmoDrop> it = ammoDrops.iterator();
        while (it.hasNext()) {
            AmmoDrop drop = it.next();
            if (drop.isExpired()) {
                it.remove();
                continue;
            }
            if (isColliding(drop.getX(), drop.getY(), drop.getRadius(), player.getX(), player.getY(), 30)) {
                boolean ammoAdded = false;
                for (int slot = 1; slot <= 5; slot++) {
                    if (player.hasWeapon(slot)) {
                        int ammoAmount = drop.getAmmoAmount(slot);
                        player.addAmmoToWeapon(slot, ammoAmount);
                        System.out.println("Added " + ammoAmount + " ammo to weapon in slot " + slot);
                        ammoAdded = true;
                    }
                }
                if (ammoAdded) {
                    it.remove();
                    score += 5;
                }
            }
        }
    }

    private void drawAmmoDrops(Graphics2D g2d) {
        for (AmmoDrop drop : ammoDrops) {
            drop.draw(g2d);
        }
    }
}