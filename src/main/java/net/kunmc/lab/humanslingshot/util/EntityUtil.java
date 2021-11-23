package net.kunmc.lab.humanslingshot.util;

import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class EntityUtil {
    public static void blowOff(Entity entity, Vector velocity, Plugin plugin) {
        System.out.println(velocity.length());
        new BukkitRunnable() {
            private int count = 0;
            private final int numberOfTimes = ((int) (velocity.length() / 4)) + 1;
            private double length = velocity.length();

            @Override
            public void run() {
                Vector v = velocity.clone().normalize();
                if (length > 4.0) {
                    v.multiply(4.0);
                    length -= 4.0;
                } else {
                    v.multiply(length);
                }
                System.out.println(v.length());
                entity.setVelocity(v);

                count++;
                if (count >= numberOfTimes) {
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 12);
    }
}
