package Guns;

public class Sniper extends AbstractGun {
    public Sniper() {
        super();
        this.name = "Sniper";
        this.maxAmmo = 5;
        this.ammo = this.maxAmmo;
        this.fireRate = 30;
        this.order = 6;
        this.canPenetrate = true;
        this.spreadAngle = 0;
        this.totalAmmo = 5 * maxAmmo;
    }
}
