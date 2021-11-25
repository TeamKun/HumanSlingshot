package net.kunmc.lab.humanslingshot;

import net.kunmc.lab.humanslingshot.util.EntityUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Bullet {
    private final Player player;
    private final Config config;
    private final Plugin plugin;
    private Entity arrow;
    private double power;

    public Bullet(Player player, Config config, Plugin plugin) {
        this.player = player;
        this.config = config;
        this.plugin = plugin;
    }


    public void fire(Vector velocity) {
        power = velocity.length();

        player.leaveVehicle();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            arrow = player.getWorld().spawnEntity(player.getLocation(), EntityType.ARROW, CreatureSpawnEvent.SpawnReason.CUSTOM, e -> {
                e.addPassenger(player);
                EntityUtil.blowOff(e, velocity, plugin);
            });
        }, 1);

        new DetectCollisionTask().runTaskTimerAsynchronously(plugin, 2, 0);
    }

    private void explode() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.getLocation().createExplosion(((float) power) * config.explosionMagnification.value(), false, false);
            player.setHealth(0.0);
        });
    }

    private class DetectCollisionTask extends BukkitRunnable {
        @Override
        public void run() {
            if (arrow.isOnGround()) {
                explode();
                this.cancel();
            }
        }
    }
}
