package Zombies;

public class CrawlerZombie extends AbstractZombie {
    private static final int JUMP_DISTANCE = 120;
    private static final int JUMP_COOLDOWN = 3500;
    private long lastJumpTime;
    private boolean isJumping;
    private long jumpStartTime;
    private static final long JUMP_ANIMATION_DURATION = 300;

    public CrawlerZombie(int x, int y) {
        super(x, y);
        this.type = "Crawler Zombie";
        this.health = LOW_HEALTH;
        this.speed = FAST_SPEED;
        this.damage = MEDIUM_DAMAGE;
        this.lastJumpTime = 0;
        this.isJumping = false;
    }

    @Override
    public void useSpecialAbility(int playerX, int playerY) {
        if (!isAlive || isStunned)
            return;

        if (isJumping) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - jumpStartTime >= JUMP_ANIMATION_DURATION) {
                isJumping = false;
            }
        }

        double distance = Math.sqrt(Math.pow(playerX - x, 2) + Math.pow(playerY - y, 2));

        long currentTime = System.currentTimeMillis();
        if (distance <= JUMP_DISTANCE && currentTime - lastJumpTime >= JUMP_COOLDOWN) {
            jump(playerX, playerY);
            lastJumpTime = currentTime;
        }
    }

    private void jump(int playerX, int playerY) {

        double dx = playerX - x;
        double dy = playerY - y;
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length > 0) {
            dx = dx / length * (length * 0.5);
            dy = dy / length * (length * 0.5);

            x += (int) dx;
            y += (int) dy;

            isJumping = true;
            jumpStartTime = System.currentTimeMillis();

            System.out.println("Crawler zombie jumped to (" + x + ", " + y + ")");
        }
    }

    public boolean isJumping() {
        return isJumping;
    }

    @Override
    public int attack() {
        if (!isAlive)
            return 0;

        return isJumping ? (int) (damage * 1.25) : damage;
    }
}