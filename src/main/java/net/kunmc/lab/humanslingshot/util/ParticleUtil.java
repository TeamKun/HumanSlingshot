package net.kunmc.lab.humanslingshot.util;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class ParticleUtil {
    public static void drawLine(Particle particle, Location from, Location to, double gap, int count, Object data) {
        World w = from.getWorld();
        Vector subtract = to.clone().subtract(from).toVector();
        Vector inc = subtract.clone().normalize().multiply(gap);

        Location start = from.clone();
        for (int i = 0; i < subtract.length() / inc.length(); i++) {
            w.spawnParticle(particle, start, count, data);
            start.add(inc);
        }
    }
}
