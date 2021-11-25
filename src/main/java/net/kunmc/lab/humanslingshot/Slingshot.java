package net.kunmc.lab.humanslingshot;

import net.kunmc.lab.humanslingshot.util.LocationUtil;
import net.kunmc.lab.humanslingshot.util.ParticleUtil;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftArmorStand;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class Slingshot implements Listener {
    private final Config config;
    private final Plugin plugin;
    private final ArmorStand seat;
    private final Pole pole1;
    private final Pole pole2;
    private final Location center;
    private Player puller;
    private PullState pullState = PullState.WEAK;
    private final List<BukkitTask> taskList = new ArrayList<>();

    public static boolean isValidPosition(Pole pole1, Pole pole2) {
        Location loc1 = pole1.topLocation();
        Location loc2 = pole2.topLocation();

        if (loc1.getBlockY() != loc2.getBlockY()) {
            return false;
        }

        return loc1.distance(loc2) <= 6;
    }

    public static Slingshot of(Pole pole1, Pole pole2, Config config, Plugin plugin) {
        if (!isValidPosition(pole1, pole2)) {
            return null;
        }

        return new Slingshot(pole1, pole2, config, plugin);
    }

    private Slingshot(Pole pole1, Pole pole2, Config config, Plugin plugin) {
        this.plugin = plugin;
        this.config = config;
        this.pole1 = pole1;
        this.pole2 = pole2;
        this.seat = ((ArmorStand) pole1.world().spawnEntity(LocationUtil.midway(pole1.top(), pole2.top()), EntityType.ARMOR_STAND, CreatureSpawnEvent.SpawnReason.CUSTOM, e -> {
            ArmorStand stand = ((ArmorStand) e);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setMarker(true);
            stand.setCanMove(true);
            stand.setInvisible(true);
        }));
        seat.teleport(seat.getLocation().subtract(0, 0.5, 0));
        this.center = seat.getLocation();

        taskList.add(new ParticleTask().runTaskTimerAsynchronously(plugin, 0, 0));
        taskList.add(new PullingTask().runTaskTimerAsynchronously(plugin, 0, 0));
        taskList.add(new FixPullerLocationTask().runTaskTimer(plugin, 0, 1));

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }


    public void unassign() {
        pole1.unassign();
        pole2.unassign();

        seat.remove();

        taskList.forEach(BukkitTask::cancel);
        HandlerList.unregisterAll(this);
    }

    private void fire() {
        if (seat.getPassengers().isEmpty()) {
            return;
        }
        Entity passenger = seat.getPassengers().get(0);

        Vector direction = center.clone().subtract(seat.getLocation()).toVector().normalize();
        direction.setY(passenger.getLocation().getDirection().getY());
        double distance = center.distance(seat.getLocation());

        new Bullet(((Player) passenger), config, plugin).fire(direction.multiply(distance / 12.0 * 8 * config.launchPowerMagnification.value()));

        puller = null;
        teleportSeat(center);
    }

    private void teleportSeat(Location to) {
        try {
            ((CraftArmorStand) seat).getHandle().teleportAndSync(to.getX(), to.getY(), to.getZ());
        } catch (Exception ignored) {
        }
    }

    @EventHandler
    private void onInteractParticle(PlayerInteractEvent e) {
        if (e.getAction() != Action.LEFT_CLICK_AIR) {
            return;
        }

        if (!seat.getPassengers().isEmpty()) {
            return;
        }

        Player p = e.getPlayer();
        Vector direction = p.getLocation().getDirection();

        BoundingBox boundingBox = pole1.top().getBoundingBox().expandDirectional(pole2.topLocation().subtract(pole1.topLocation()).toVector());
        if (boundingBox.overlaps(p.getEyeLocation().toVector(), p.getEyeLocation().add(direction.multiply(4)).toVector())) {
            seat.addPassenger(e.getPlayer());
        }
    }

    @EventHandler
    private void onInteractPassenger(PlayerInteractEntityEvent e) {
        if (e.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        if (seat.getPassengers().isEmpty()) {
            return;
        }
        Entity passenger = seat.getPassengers().get(0);

        if (!e.getRightClicked().equals(passenger)) {
            return;
        }

        if (puller == null) {
            puller = e.getPlayer();
        } else if (e.getPlayer().equals(puller)) {
            fire();
        }
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent e) {
        unassign();
    }

    @EventHandler
    private void onPassengerQuit(PlayerQuitEvent e) {
        Entity passenger = seat.getPassengers().get(0);
        if (e.getPlayer().equals(passenger)) {
            passenger.leaveVehicle();
        }
    }

    private class ParticleTask extends BukkitRunnable {
        @Override
        public void run() {
            if (pole1.isUnassigned() || pole2.isUnassigned()) {
                unassign();
                return;
            }

            Location center = seat.getBoundingBox().getCenter().toLocation(seat.getWorld()).add(0, 0.65, 0);
            Particle.DustOptions dustOptions = new Particle.DustOptions(pullState.color, 0.85F);

            ParticleUtil.drawLine(Particle.REDSTONE, pole1.particleLocation(), center, 0.25, 1, dustOptions);
            ParticleUtil.drawLine(Particle.REDSTONE, pole2.particleLocation(), center, 0.25, 1, dustOptions);
        }
    }

    private class FixPullerLocationTask extends BukkitRunnable {
        @Override
        public void run() {
            if (puller == null) {
                return;
            }

            if (pullState.shouldSneaking && !puller.isSneaking()) {
                fix();
            }

            if (center.distance(puller.getLocation()) > config.maxPullDistance.value()) {
                fix();
            }
        }

        private void fix() {
            Vector velocity = puller.getLocation().subtract(center).toVector().normalize().multiply(-1);
            if (puller.isSneaking()) {
                velocity.multiply(1.31 / 20);
            } else if (puller.isSprinting()) {
                velocity.multiply(5.612 / 20);
            } else {
                velocity.multiply(4.317 / 20);
            }
            puller.setVelocity(puller.getVelocity().add(velocity));
        }
    }

    private class PullingTask extends BukkitRunnable {
        @Override
        public void run() {
            if (seat.getPassengers().isEmpty()) {
                pullState = PullState.WEAK;
                puller = null;
                teleportSeat(center);
                return;
            }

            if (puller == null) {
                pullState = PullState.WEAK;
                return;
            }

            updateState(center.distance(puller.getLocation()));

            Location loc = puller.getEyeLocation();
            loc.add(loc.getDirection().setY(-0.75));
            teleportSeat(loc);
        }

        private void updateState(double distance) {
            if (distance <= 5.5) {
                pullState = PullState.WEAK;
            } else if (distance <= 9.0) {
                pullState = PullState.MODERATE;
            } else if (distance <= config.maxPullDistance.value()) {
                pullState = PullState.STRONG;
            } else {
                pullState = PullState.MAX;
            }
        }
    }

    private enum PullState {
        WEAK(Color.WHITE, false),
        MODERATE(Color.YELLOW, false),
        STRONG(Color.ORANGE, true),
        MAX(Color.RED, true);

        final Color color;
        final boolean shouldSneaking;

        PullState(Color color, boolean shouldSneaking) {
            this.color = color;
            this.shouldSneaking = shouldSneaking;
        }
    }
}
