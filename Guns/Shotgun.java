package Guns;

public class Shotgun extends AbstractGun {
    public Shotgun() {
        super();
        this.name = "Shotgun";
        this.maxAmmo = 5;
        this.ammo = this.maxAmmo;
        this.fireRate = 60;
        this.order = 4;
        this.spreadAngle = 45;
        this.totalAmmo = 5 * maxAmmo;
    }
}
