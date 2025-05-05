package Guns;

public class Pistol extends AbstractGun {

    public Pistol() {
        super();
        this.name = "Pistol";
        this.maxAmmo = 12;
        this.ammo = this.maxAmmo;
        this.fireRate = 120;
        this.order = 1;
        this.spreadAngle = 0;
        this.totalAmmo = Integer.MAX_VALUE;
        this.canPenetrate = false;
        this.canExplode = false;
    }

    @Override
    public void setTotalAmmo(int totalAmmo) {
        this.totalAmmo = Integer.MAX_VALUE;
    }
}
