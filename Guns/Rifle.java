package Guns;

public class Rifle extends AbstractGun {

    public Rifle() {
        super();
        this.name = "Rifle";
        this.maxAmmo = 30;
        this.ammo = this.maxAmmo;
        this.fireRate = 600;
        this.order = 2;
        this.spreadAngle = 15;
        this.totalAmmo = 15 * maxAmmo;
    }
}
