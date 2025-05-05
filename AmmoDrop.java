import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class AmmoDrop {

    private int x, y;
    private long spawnTime;
    private static final long LIFE_TIME = 10000;
    private static final int PICKUP_RADIUS = 30;
    private static BufferedImage ammoImage;

    static {
        try {
            ammoImage = ImageIO.read(new File("bullet_box.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public AmmoDrop(int x, int y) {
        this.x = x;
        this.y = y;
        this.spawnTime = System.currentTimeMillis();
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - spawnTime > LIFE_TIME;
    }

    public long getLifeTime() {
        return System.currentTimeMillis() - spawnTime;
    }

    public double getLifeTimePercentage() {
        return 1.0 - (double) getLifeTime() / LIFE_TIME;
    }

    public void draw(Graphics2D g2d) {

        double lifeTimePercentage = getLifeTimePercentage();
        boolean shouldFlash = lifeTimePercentage < 0.2;
        boolean isVisible = !shouldFlash || (shouldFlash && System.currentTimeMillis() % 500 < 250);

        if (!isVisible) {
            return;
        }

        int alpha = Math.min(255, Math.max(0, (int) (255 * lifeTimePercentage)));

        if (ammoImage == null) {
            g2d.setColor(new Color(30, 144, 255, alpha));
            g2d.fillRect(x - 5, y - 5, PICKUP_RADIUS + 5, PICKUP_RADIUS + 5);
        } else {
            Composite originalComposite = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha / 255f));

            g2d.drawImage(ammoImage, x - PICKUP_RADIUS / 2, y - PICKUP_RADIUS / 2, PICKUP_RADIUS, PICKUP_RADIUS, null);

            g2d.setComposite(originalComposite);
        }

    }

    public int getAmmoAmount(int weaponSlot) {
        switch (weaponSlot) {
            case 2:
                return 20;
            case 3:
                return 5;
            case 4:
                return 3;
            case 5:
                return 1;
            default:
                return 0;
        }
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getRadius() {
        return PICKUP_RADIUS;
    }

}
