package net.kunmc.lab.humanslingshot;

import net.kunmc.lab.humanslingshot.util.EntityUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class Bullet {
    private final Player player;
    private final Plugin plugin;
    private double power;

    public Bullet(Player player, Plugin plugin) {
        this.player = player;
        this.plugin = plugin;
    }

    public void fire(Vector velocity) {
        power = velocity.length();

        player.leaveVehicle();
        Bukkit.getScheduler().runTaskLater(plugin, () -> EntityUtil.blowOff(player, velocity, plugin), 1);

        long delay = 0;
        System.out.println(velocity);
        if (velocity.getY() < -0.2) {
            delay = 20;
        }
        new DetectCollisionTask().runTaskTimerAsynchronously(plugin, delay, 0);
    }

    private void explode() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.getLocation().createExplosion(((float) power), false, false);
            player.setHealth(0.0);
        });
    }

    private class DetectCollisionTask extends BukkitRunnable {
        @Override
        public void run() {
            RayTraceResult rayTraceResult = rayTrace();
            if (rayTraceResult == null || rayTraceResult.getHitBlock() == null) {
                return;
            }

            Block b = rayTraceResult.getHitBlock();
            if (player.getBoundingBox().expand(1.0).overlaps(b.getBoundingBox()) || player.isOnGround()) {
                explode();
                this.cancel();
            }
        }

        private RayTraceResult rayTrace() {
            Vector direction = player.getVelocity().normalize();
            if (direction.length() == 0) {
                return null;
            }

            BoundingBox boundingBox = player.getBoundingBox();
            World w = player.getWorld();

            RayTraceResult top = w.rayTraceBlocks(boundingBox.getMax().toLocation(w), direction, 4);
            if (top != null) {
                return top;
            }

            RayTraceResult center = w.rayTraceBlocks(boundingBox.getCenter().toLocation(w), direction, 4);
            if (center != null) {
                return center;
            }

            return w.rayTraceBlocks(boundingBox.getMin().toLocation(w), direction, 4);
        }
    }
}
