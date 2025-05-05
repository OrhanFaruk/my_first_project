package Zombies;

public class TankZombie extends AbstractZombie {
    private static final int GROUND_POUND_RANGE = 150;
    private static final int GROUND_POUND_COOLDOWN = 5000;
    private static final int GROUND_POUND_DAMAGE = 15;
    private long lastGroundPoundTime;
    private boolean isGroundPounding;
    private long groundPoundStartTime;
    private static final long GROUND_POUND_DURATION = 1000;
    private boolean groundPoundDamageApplied;

    public TankZombie(int x, int y) {
        super(x, y);
        this.type = "Tank Zombie";
        this.health = HIGH_HEALTH;
        this.speed = SLOW_SPEED;
        this.damage = HIGH_DAMAGE;
        this.lastGroundPoundTime = 0;
        this.isGroundPounding = false;
        this.groundPoundDamageApplied = false;
    }

    @Override
    public void useSpecialAbility(int playerX, int playerY) {
        if (!isAlive || isStunned)
            return;

        if (isGroundPounding) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - groundPoundStartTime >= GROUND_POUND_DURATION) {
                isGroundPounding = false;
                groundPoundDamageApplied = false;
            }

            return;
        }

        double distance = Math.sqrt(Math.pow(playerX - x, 2) + Math.pow(playerY - y, 2));

        long currentTime = System.currentTimeMillis();
        if (distance <= GROUND_POUND_RANGE && currentTime - lastGroundPoundTime >= GROUND_POUND_COOLDOWN) {
            performGroundPound();
            lastGroundPoundTime = currentTime;
        }
    }

    private void performGroundPound() {
        isGroundPounding = true;
        groundPoundStartTime = System.currentTimeMillis();
        groundPoundDamageApplied = false;
        System.out.println("Tank zombie performed ground pound at (" + x + ", " + y + ")");
    }

    public boolean isGroundPounding() {
        return isGroundPounding;
    }

    public boolean isPlayerHitByGroundPound(int playerX, int playerY) {
        if (!isGroundPounding)
            return false;

        double distance = Math.sqrt(Math.pow(playerX - x, 2) + Math.pow(playerY - y, 2));

        if (distance < GROUND_POUND_RANGE && !groundPoundDamageApplied) {
            groundPoundDamageApplied = true;
            return true;
        }

        return false;
    }

    public boolean isPlayerInGroundPoundArea(int playerX, int playerY) {
        if (!isGroundPounding)
            return false;

        double distance = Math.sqrt(Math.pow(playerX - x, 2) + Math.pow(playerY - y, 2));
        return distance < GROUND_POUND_RANGE;
    }

    public int getGroundPoundDamage() {
        return GROUND_POUND_DAMAGE;
    }

    public int getGroundPoundRange() {
        return GROUND_POUND_RANGE;
    }

    @Override
    public void moveTowards(int playerX, int playerY) {
        if (isGroundPounding) {
            double dx = playerX - x;
            double dy = playerY - y;
            double length = Math.sqrt(dx * dx + dy * dy);

            if (length > 0) {
                this.dx = dx / length * (speed * 0.3);
                this.dy = dy / length * (speed * 0.3);
                x += Math.round(this.dx);
                y += Math.round(this.dy);
            }
            return;
        }

        super.moveTowards(playerX, playerY);
    }
}