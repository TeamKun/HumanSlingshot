package net.kunmc.lab.humanslingshot.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public class LocationUtil {
    public static Location midway(Block block1, Block block2) {
        World w = block1.getWorld();
        return midway(block1.getBoundingBox().getCenter().toLocation(w),
                block2.getBoundingBox().getCenter().toLocation(w));
    }

    public static Location midway(Location loc1, Location loc2) {
        Location subtract = loc1.clone().subtract(loc2);
        return loc2.clone().add(subtract.multiply(0.5));
    }
}
