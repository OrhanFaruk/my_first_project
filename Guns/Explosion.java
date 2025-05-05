package Guns;

public class Explosion {
    private int x, y;
    private int size;
    private int maxSize;
    private int alpha;
    private int duration;
    private int age;
    private int damage;
    private boolean damageApplied;

    public Explosion(int x, int y) {
        this(x, y, 0);
    }

    public Explosion(int x, int y, int damage) {
        this.x = x;
        this.y = y;
        this.size = 10;
        this.maxSize = 30;
        this.alpha = 255;
        this.duration = 20;
        this.age = 0;
        this.damageApplied = false;
        this.damage = damage;
    }

    public void update() {
        age++;

        if (age < duration / 2) {
            size = size + (maxSize - size) / 5;
        }

        alpha = 255 - (255 * age / duration);
        if (alpha < 0)
            alpha = 0;
    }

    public boolean isFinished() {
        return age >= duration;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getSize() {
        return size;
    }

    public int getAlpha() {
        return alpha;
    }

    public int getDamage() {
        return damage;
    }

    public boolean isDamageApplied() {
        return damageApplied;
    }

    public void setDamageApplied(boolean applied) {
        this.damageApplied = applied;
    }
}