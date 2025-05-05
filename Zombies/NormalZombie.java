package Zombies;

public class NormalZombie extends AbstractZombie {

    public NormalZombie(int x, int y) {
        super(x, y);
        this.type = "Normal Zombi";
        this.health = MEDIUM_HEALTH;
        this.speed = SLOW_SPEED;
        this.damage = MEDIUM_DAMAGE;
    }

}