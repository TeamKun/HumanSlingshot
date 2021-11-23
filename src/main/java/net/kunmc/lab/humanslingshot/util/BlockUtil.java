package net.kunmc.lab.humanslingshot.util;

import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

public class BlockUtil {
    public static List<Block> getAdjacentBlocks(Block block) {
        List<Block> list = new ArrayList<>();
        Location center = block.getBoundingBox().getCenter().toLocation(block.getWorld());

        list.add(center.clone().add(0, 1, 0).getBlock());
        list.add(center.clone().add(0, -1, 0).getBlock());
        list.add(center.clone().add(1, 0, 0).getBlock());
        list.add(center.clone().add(-1, 0, 0).getBlock());
        list.add(center.clone().add(0, 0, 1).getBlock());
        list.add(center.clone().add(0, 0, -1).getBlock());

        return list;
    }

    public static boolean isAdjacent(Block block1, Block block2) {
        return getAdjacentBlocks(block1).stream().anyMatch(b -> b.equals(block2));
    }
}
