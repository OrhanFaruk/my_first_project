package Guns;

public class RocketLauncher extends AbstractGun {

    public RocketLauncher() {
        super();
        this.name = "RocketLauncher";
        this.maxAmmo = 1;
        this.ammo = this.maxAmmo;
        this.fireRate = 10;
        this.order = 11;
        this.canExplode = true;
        this.spreadAngle = 0;
        this.totalAmmo = 5 * maxAmmo;
    }
}