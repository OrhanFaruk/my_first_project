package Guns;

public class Bullet {
    private double x, y;
    private double dx, dy;
    private double speed;
    private int damage;
    private boolean isPenetrating; // sniper
    private boolean isExplosive; // rocket
    private int explosionRadius; // rocket

    public Bullet(int startX, int startY, double angle) {
        this.x = startX;
        this.y = startY;
        this.speed = 10.0;
        this.damage = 30;
        this.isPenetrating = false;
        this.isExplosive = false;
        this.explosionRadius = 0;

        this.dx = Math.cos(angle) * speed;
        this.dy = Math.sin(angle) * speed;
    }

    public void update() {
        x += dx;
        y += dy;
    }

    public int getX() {
        return (int) x;
    }

    public int getY() {
        return (int) y;
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public void setSpeed(double speed) {
        this.speed = speed;

        double angle = Math.atan2(dy, dx);
        this.dx = Math.cos(angle) * speed;
        this.dy = Math.sin(angle) * speed;
    }

    public void setPenetrating(boolean isPenetrating) {
        this.isPenetrating = isPenetrating;
    }

    public boolean isPenetrating() {
        return isPenetrating;
    }

    public void setExplosive(boolean isExplosive) {
        this.isExplosive = isExplosive;
    }

    public boolean isExplosive() {
        return isExplosive;
    }

    public void setExplosionRadius(int radius) {
        this.explosionRadius = radius;
    }

    public int getExplosionRadius() {
        return explosionRadius;
    }

}