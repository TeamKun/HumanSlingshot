package net.kunmc.lab.humanslingshot;

import net.kunmc.lab.humanslingshot.util.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Wall;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class Pole implements Listener {
    public static String metadataKey = "Pole";
    private final Block top;
    private final Block center;
    private final Block bottom;
    private final BukkitTask task;
    private final Plugin plugin;
    private boolean unassigned = false;

    public static boolean isAssignable(Block top) {
        if (top == null || !(top.getBlockData() instanceof Wall)) {
            return false;
        }
        if (top.hasMetadata(metadataKey)) {
            return false;
        }
        Wall topData = ((Wall) top.getBlockData());
        if (isAdjacent(topData)) {
            return false;
        }

        Location loc = top.getLocation();

        Block above = loc.clone().add(0, 1.3, 0).getBlock();
        if (above.getType() != Material.AIR) {
            return false;
        }

        Block center = loc.clone().add(0, -0.3, 0).getBlock();
        if (!(center.getBlockData() instanceof Wall)) {
            return false;
        }
        Wall centerData = ((Wall) center.getBlockData());
        if (isAdjacent(centerData)) {
            return false;
        }

        Block bottom = loc.clone().add(0, -1.3, 0).getBlock();
        if (!(bottom.getBlockData() instanceof Wall)) {
            return false;
        }
        Wall bottomData = ((Wall) bottom.getBlockData());
        if (isAdjacent(bottomData)) {
            return false;
        }

        Block ground = loc.clone().add(0, -2.3, 0).getBlock();
        if (ground.getBlockData() instanceof Wall) {
            return false;
        }

        return true;
    }

    private static boolean isAdjacent(Wall wall) {
        if (wall.getHeight(BlockFace.NORTH) != Wall.Height.NONE) {
            return true;
        }
        if (wall.getHeight(BlockFace.EAST) != Wall.Height.NONE) {
            return true;
        }
        if (wall.getHeight(BlockFace.SOUTH) != Wall.Height.NONE) {
            return true;
        }
        if (wall.getHeight(BlockFace.WEST) != Wall.Height.NONE) {
            return true;
        }

        return false;
    }

    public static Pole of(Block top, Plugin plugin) {
        if (!isAssignable(top)) {
            return null;
        }

        return new Pole(top, plugin);
    }

    private Pole(Block top, Plugin plugin) {
        Location loc = top.getLocation();

        this.top = top;
        this.center = loc.clone().add(0, -0.5, 0).getBlock();
        this.bottom = loc.clone().add(0, -1.5, 0).getBlock();
        this.plugin = plugin;

        top.setMetadata(metadataKey, new FixedMetadataValue(plugin, null));
        center.setMetadata(metadataKey, new FixedMetadataValue(plugin, null));
        bottom.setMetadata(metadataKey, new FixedMetadataValue(plugin, null));

        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isWall(top) || !isWall(center) || !isWall(bottom)) {
                    unassign();
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0, 0);

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public Block top() {
        return top;
    }

    public Location topLocation() {
        return top.getLocation();
    }

    public World world() {
        return top.getWorld();
    }

    private boolean isWall(Block block) {
        return block.getBlockData() instanceof Wall;
    }

    public void unassign() {
        if (!unassigned) {
            top.removeMetadata(metadataKey, plugin);
            center.removeMetadata(metadataKey, plugin);
            bottom.removeMetadata(metadataKey, plugin);

            task.cancel();
            HandlerList.unregisterAll(this);

            unassigned = true;
        }
    }

    public boolean isUnassigned() {
        return unassigned;
    }

    @EventHandler
    private void onBlockPlace(BlockPlaceEvent e) {
        Block b = e.getBlockPlaced();
        if (BlockUtil.isAdjacent(b, top)) {
            e.setCancelled(true);
            return;
        }
        if (BlockUtil.isAdjacent(b, center)) {
            e.setCancelled(true);
            return;
        }
        if (BlockUtil.isAdjacent(b, bottom)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    private void onInteractTop(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = e.getItem();
        if (item == null || !item.getType().equals(Material.SHEARS)) {
            return;
        }

        Block b = e.getClickedBlock();
        if (top.equals(b)) {
            unassign();
        }
    }

    @EventHandler
    private void onPluginDisable(PluginDisableEvent e) {
        if (e.getPlugin().equals(plugin)) {
            unassign();
        }
    }
}
