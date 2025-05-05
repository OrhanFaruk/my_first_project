package Guns;

public abstract class AbstractGun {
    protected int ammo;
    protected int maxAmmo;
    protected int totalAmmo;
    protected int fireRate;
    protected int order;
    protected int spreadAngle;
    protected boolean canPenetrate;
    protected boolean canExplode;
    protected String name;
    protected boolean isReloading;
    protected long lastShotTime;

    protected AbstractGun() {
        this.isReloading = false;
        this.lastShotTime = 0;
        this.canPenetrate = false;
        this.canExplode = false;
    }

    public boolean canShoot() {
        if (ammo <= 0 || isReloading) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long timeBetweenShots = (long) (60000.0 / fireRate);

        return currentTime - lastShotTime >= timeBetweenShots;
    }

    public void shoot() {
        if (canShoot()) {
            ammo--;
            lastShotTime = System.currentTimeMillis();
        }
    }

    public void reload() {
        if (!isReloading && ammo < maxAmmo && totalAmmo > 0) {
            isReloading = true;
        }
    }

    public void completeReload() {
        if (isReloading) {
            int currentAmmo = ammo;
            int neededAmmo = maxAmmo - currentAmmo;
            if (neededAmmo <= totalAmmo) {
                ammo += neededAmmo;
                totalAmmo -= neededAmmo;
            } else {
                ammo += totalAmmo;
                totalAmmo = 0;
            }
            isReloading = false;
        }
    }

    public int getAmmo() {
        return ammo;
    }

    public int getMaxAmmo() {
        return maxAmmo;
    }

    public int getTotalAmmo() {
        return totalAmmo;
    }

    public void setTotalAmmo(int totalAmmo) {
        this.totalAmmo = totalAmmo;
    }

    public int getFireRate() {
        return fireRate;
    }

    public int getSpreadAngle() {
        return spreadAngle;
    }

    public String getName() {
        return name;
    }

    public boolean isReloading() {
        return isReloading;
    }
}
