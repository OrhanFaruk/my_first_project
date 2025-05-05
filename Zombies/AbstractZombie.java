package Zombies;

public abstract class AbstractZombie {
    protected int health;
    protected double speed;
    protected int damage;
    protected String type;
    protected boolean isAlive;
    protected int x, y;
    protected double dx, dy;
    protected int stuckCounter;
    protected int lastX, lastY;
    protected boolean isStunned;
    protected long stunEndTime;

    protected static final int MAP_MIN_X = 0;
    protected static final int MAP_MAX_X = 1200;
    protected static final int MAP_MIN_Y = 0;
    protected static final int MAP_MAX_Y = 800;

    public static final int LOW_HEALTH = 25;
    public static final int MEDIUM_HEALTH = 50;
    public static final int HIGH_HEALTH = 100;

    protected static final double VERY_SLOW_SPEED = 0.5;
    protected static final double SLOW_SPEED = 1.0;
    protected static final double FAST_SPEED = 1.8;

    protected static final int MEDIUM_DAMAGE = 10;
    protected static final int HIGH_DAMAGE = 20;

    public AbstractZombie(int x, int y) {
        this.x = x;
        this.y = y;
        this.lastX = x;
        this.lastY = y;
        this.isAlive = true;
        this.stuckCounter = 0;
        this.dx = 0;
        this.dy = 0;
        this.isStunned = false;
        this.stunEndTime = 0;
    }

    public void moveTowards(int playerX, int playerY) {
        if (!isAlive || isStunned)
            return;

        if (Math.abs(x - lastX) < 1 && Math.abs(y - lastY) < 1) {
            stuckCounter++;

            if (stuckCounter > 30) {
                unstuck();
                stuckCounter = 0;
                return;
            }
        } else {
            stuckCounter = 0;
        }

        lastX = x;
        lastY = y;

        double dx = playerX - x;
        double dy = playerY - y;
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length > 0) {

            this.dx = dx / length * speed;
            this.dy = dy / length * speed;

            x += Math.round(this.dx);
            y += Math.round(this.dy);

            x = Math.max(MAP_MIN_X, Math.min(MAP_MAX_X, x));
            y = Math.max(MAP_MIN_Y, Math.min(MAP_MAX_Y, y));
        }
    }

    private void unstuck() {

        double randomAngle = Math.random() * 2 * Math.PI;
        double unstuckDx = Math.cos(randomAngle) * speed * 10;
        double unstuckDy = Math.sin(randomAngle) * speed * 10;

        x += Math.round(unstuckDx);
        y += Math.round(unstuckDy);

        x = Math.max(MAP_MIN_X, Math.min(MAP_MAX_X, x));
        y = Math.max(MAP_MIN_Y, Math.min(MAP_MAX_Y, y));

        System.out.println("Zombie unstuck attempt at (" + x + "," + y + ")");
    }

    public int attack() {
        if (!isAlive)
            return 0;
        return damage;
    }

    public void takeDamage(int amount) {
        if (!isAlive)
            return;

        health -= amount;
        if (health <= 0) {
            die();
        }
    }

    public void die() {
        isAlive = false;
        onDeath();
    }

    protected void onDeath() {

    }

    public void useSpecialAbility(int playerX, int playerY) {

    }

    public void stun(int duration) {
        isStunned = true;
        stunEndTime = System.currentTimeMillis() + duration;
    }

    public boolean canBeStunned() {
        return true;
    }

    public void update() {
        if (isStunned && System.currentTimeMillis() >= stunEndTime) {
            isStunned = false;
        }
    }

    public int getHealth() {
        return health;
    }

    public double getSpeed() {
        return speed;
    }

    public int getDamage() {
        return damage;
    }

    public String getType() {
        return type;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }
}
