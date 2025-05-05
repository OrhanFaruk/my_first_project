package Zombies;

public class AcidSpitterZombie extends AbstractZombie {
    private static final int SPIT_RANGE = 200;
    private static final int SPIT_COOLDOWN = 2000;
    private static final int SPIT_DAMAGE = 4;
    private static final int DEATH_EXPLOSION_RANGE = 100;
    private static final int DEATH_EXPLOSION_DAMAGE = 15;

    private long lastSpitTime;
    private boolean acidSpitActive = false;
    private int acidSpitX, acidSpitY;
    private long acidSpitStartTime;
    private static final long ACID_SPIT_DURATION = 1000;

    public AcidSpitterZombie(int x, int y) {
        super(x, y);
        this.type = "Acid Spitter Zombie";
        this.health = LOW_HEALTH;
        this.speed = SLOW_SPEED;
        this.damage = MEDIUM_DAMAGE;
        this.lastSpitTime = 0;
    }

    @Override
    public void useSpecialAbility(int playerX, int playerY) {
        if (!isAlive)
            return;

        if (acidSpitActive) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - acidSpitStartTime >= ACID_SPIT_DURATION) {
                acidSpitActive = false;
            }
        }

        double distance = Math.sqrt(Math.pow(playerX - x, 2) + Math.pow(playerY - y, 2));

        long currentTime = System.currentTimeMillis();
        if (distance <= SPIT_RANGE && currentTime - lastSpitTime >= SPIT_COOLDOWN) {
            spitAcid(playerX, playerY);
            lastSpitTime = currentTime;
        }
    }

    private void spitAcid(int targetX, int targetY) {

        double dx = targetX - x;
        double dy = targetY - y;
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length > 0) {

            dx = dx / length;
            dy = dy / length;
            acidSpitX = x + (int) (dx * SPIT_RANGE * 0.75);
            acidSpitY = y + (int) (dy * SPIT_RANGE * 0.75);

            acidSpitActive = true;
            acidSpitStartTime = System.currentTimeMillis();

            System.out.println("Acid Spitter zombie spits acid towards (" + targetX + ", " + targetY + ")");
        }
    }

    public boolean isPlayerHitByAcid(int playerX, int playerY, int playerRadius) {
        if (!acidSpitActive)
            return false;

        double distance = Math.sqrt(Math.pow(playerX - acidSpitX, 2) + Math.pow(playerY - acidSpitY, 2));
        return distance < playerRadius + 20;
    }

    public int getAcidDamage() {
        return SPIT_DAMAGE;
    }

    public int getAcidX() {
        return acidSpitX;
    }

    public int getAcidY() {
        return acidSpitY;
    }

    public boolean isAcidSpitActive() {
        return acidSpitActive;
    }

    @Override
    protected void onDeath() {
        explodeAcid();
    }

    private void explodeAcid() {
        System.out.println("Acid Spitter zombie explodes on death, affecting area within " +
                DEATH_EXPLOSION_RANGE + " units and dealing " +
                DEATH_EXPLOSION_DAMAGE + " damage");
    }

    public boolean isPlayerHitByDeathExplosion(int playerX, int playerY) {
        if (isAlive)
            return false;

        double distance = Math.sqrt(Math.pow(playerX - x, 2) + Math.pow(playerY - y, 2));
        return distance < DEATH_EXPLOSION_RANGE;
    }

    public int getDeathExplosionDamage() {
        return DEATH_EXPLOSION_DAMAGE;
    }
}