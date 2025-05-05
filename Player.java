import Guns.AbstractGun;
import Guns.Pistol;

import java.util.HashMap;
import java.util.Map;

public class Player {
    private int x, y;
    private int health;
    private int maxHealth;
    private double speed;
    private double baseSpeed;
    private boolean isSlowed;
    private boolean isReloading;
    private long reloadStartTime;
    private static final long RELOAD_TIME = 1500;
    private boolean isDead = false;

    private Map<Integer, AbstractGun> weapons;
    private AbstractGun currentGun;
    private int currentWeaponSlot;

    public Player(int x, int y) {
        this.x = x;
        this.y = y;
        this.maxHealth = 100;
        this.health = maxHealth;
        this.baseSpeed = 3.0;
        this.speed = this.baseSpeed;
        this.isSlowed = false;
        this.isReloading = false;

        this.weapons = new HashMap<>();

        this.weapons.put(1, new Pistol());
        this.currentWeaponSlot = 1;
        this.currentGun = weapons.get(currentWeaponSlot);
    }

    public void addWeapon(int slot, AbstractGun gun) {
        weapons.put(slot, gun);
    }

    public boolean switchWeapon(int slot) {
        if (isReloading) {
            return false;
        }

        if (weapons.containsKey(slot) && slot != currentWeaponSlot) {
            currentWeaponSlot = slot;
            currentGun = weapons.get(slot);
            return true;
        }
        return false;
    }

    public boolean hasWeapon(int slot) {
        return weapons.containsKey(slot);
    }

    public String getCurrentWeaponName() {
        return currentGun.getName();
    }

    public int[] getAvailableWeaponSlots() {
        return weapons.keySet().stream().mapToInt(Integer::intValue).toArray();
    }

    public void move(int dx, int dy, int maxWidth, int maxHeight) {
        if (isDead)
            return;

        if (dx != 0 && dy != 0) {
            double length = Math.sqrt(dx * dx + dy * dy);
            dx = (int) (dx / length * speed);
            dy = (int) (dy / length * speed);
        } else if (dx != 0) {
            dx = (int) (dx * speed);
        } else if (dy != 0) {
            dy = (int) (dy * speed);
        }

        x += dx;
        y += dy;

        x = Math.max(15, Math.min(maxWidth - 15, x));
        y = Math.max(15, Math.min(maxHeight - 15, y));
    }

    public boolean canShoot() {
        if (isDead)
            return false;

        return !isReloading && currentGun.canShoot();
    }

    public void shoot() {
        if (!isReloading) {
            currentGun.shoot();

            if (currentGun.getAmmo() <= 0 && currentGun.getTotalAmmo() > 0) {
                reload();
            }
        }
    }

    public void reload() {
        if (!isReloading && currentGun.getAmmo() < currentGun.getMaxAmmo() && currentGun.getTotalAmmo() > 0) {
            isReloading = true;
            reloadStartTime = System.currentTimeMillis();
            currentGun.reload();
        }
    }

    public void update() {
        if (isReloading) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - reloadStartTime >= RELOAD_TIME) {
                currentGun.completeReload();
                isReloading = false;
            }
        }
    }

    public void takeDamage(int amount) {
        health -= amount;
        if (health <= 0) {
            health = 0;
            die();
        }
    }

    public void die() {
        isDead = true;
    }

    public boolean isDead() {
        return isDead;
    }

    public void reset() {
        health = maxHealth;
        isDead = false;
        isReloading = false;

        weapons.clear();
        weapons.put(1, new Pistol());
        currentWeaponSlot = 1;
        currentGun = weapons.get(currentWeaponSlot);
    }

    public void heal(int amount) {
        health += amount;
        if (health > 2 * maxHealth) {
            health = 2 * maxHealth;
        }
    }

    public void switchGun(AbstractGun gun) {
        this.currentGun = gun;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public void setHealth(int health) {
        this.health = Math.max(0, Math.min(maxHealth, health));
    }

    public int getAmmo() {
        return currentGun.getAmmo();
    }

    public int getTotalAmmo() {
        return currentGun.getTotalAmmo();
    }

    public boolean isReloading() {
        return isReloading;
    }

    public AbstractGun getCurrentGun() {
        return currentGun;
    }

    public int getCurrentWeaponSlot() {
        return currentWeaponSlot;
    }

    public void addTotalAmmo(int amount) {
        if (currentGun != null) {
            int currentTotal = currentGun.getTotalAmmo();
            currentGun.setTotalAmmo(currentTotal + amount);
        }
    }

    public boolean addAmmoToWeapon(int slot, int amount) {
        if (weapons.containsKey(slot)) {
            AbstractGun gun = weapons.get(slot);
            int currentTotal = gun.getTotalAmmo();
            gun.setTotalAmmo(currentTotal + amount);
            System.out.println("[DEBUG] Added " + amount + " ammo to " + gun.getName() +
                    " (Slot " + slot + "). New total: " + gun.getTotalAmmo());
            return true;
        }
        System.out.println("[DEBUG] Failed to add ammo to slot " + slot + " - weapon not found");
        return false;
    }

    public void setSlowed(boolean slowed) {
        this.isSlowed = slowed;

        if (slowed) {
            this.speed = this.baseSpeed * 0.5; // 50% speed when slowed
        } else {
            this.speed = this.baseSpeed; // Normal speed
        }
    }

    /**
     * Checks if the player is currently slowed
     * 
     * @return Whether the player is slowed
     */
    public boolean isSlowed() {
        return isSlowed;
    }
}