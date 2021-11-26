package net.kunmc.lab.humanslingshot;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import net.kunmc.lab.humanslingshot.util.EntityUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.spigotmc.event.entity.EntityDismountEvent;

public class Bullet implements Listener {
    private final Player player;
    private final Config config;
    private final Plugin plugin;
    private Entity arrow;
    private double power;

    public Bullet(Player player, Config config, Plugin plugin) {
        this.player = player;
        this.config = config;
        this.plugin = plugin;

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void fire(Vector velocity) {
        power = velocity.length();

        player.leaveVehicle();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            arrow = player.getWorld().spawnEntity(player.getLocation(), EntityType.ARROW, CreatureSpawnEvent.SpawnReason.CUSTOM, e -> {
                e.addPassenger(player);
                EntityUtil.blowOff(e, velocity, plugin);
            });

            new DecideExplodeTask().runTaskTimerAsynchronously(plugin, 2, 0);
            new SendMountPacketTask().runTaskTimer(plugin, 0, 4);
        }, 1);
    }

    private void explode() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.getLocation().createExplosion(((float) power) * config.explosionMagnification.value(), false, false);
            player.setHealth(0.0);
        });

        remove();
    }

    private void remove() {
        arrow.remove();
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    private void onPassengerQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (p.equals(player)) {
            remove();
        }
    }

    @EventHandler
    private void onPassengerDismount(EntityDismountEvent e) {
        Entity vehicle = e.getDismounted();
        if (vehicle.equals(arrow)) {
            e.setCancelled(true);
        }
    }

    private class DecideExplodeTask extends BukkitRunnable {
        @Override
        public void run() {
            if (arrow.isDead()) {
                this.cancel();
                return;
            }

            if (arrow.isOnGround()) {
                explode();
                this.cancel();
            }
        }
    }

    private class SendMountPacketTask extends BukkitRunnable {
        final ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        final PacketContainer packet;

        public SendMountPacketTask() {
            packet = protocolManager.createPacket(PacketType.Play.Server.MOUNT);
            packet.getIntegers().write(0, arrow.getEntityId());
            packet.getIntegerArrays().write(0, new int[]{player.getEntityId()});
        }

        @Override
        public void run() {
            if (arrow.isDead()) {
                this.cancel();
                return;
            }

            try {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    protocolManager.sendServerPacket(p, packet);
                }
            } catch (Exception ignored) {
            }
        }
    }
}
