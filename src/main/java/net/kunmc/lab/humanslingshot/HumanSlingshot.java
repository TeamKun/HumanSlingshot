package net.kunmc.lab.humanslingshot;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedParticle;
import dev.kotx.flylib.FlyLib;
import dev.kotx.flylib.command.Command;
import net.kunmc.lab.configlib.command.ConfigCommand;
import net.kunmc.lab.configlib.command.ConfigCommandBuilder;
import net.minecraft.server.v1_16_R3.DedicatedServer;
import net.minecraft.server.v1_16_R3.MinecraftServer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Wall;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HumanSlingshot extends JavaPlugin implements Listener {
    private Config config;
    private final Map<UUID, Pole> uuidPoleMap = new HashMap<>();

    @Override
    public void onEnable() {
        config = new Config(this);

        ConfigCommand configCommand = new ConfigCommandBuilder(config).disableReloadCommand().build();
        FlyLib.create(this, builder -> {
            builder.command(new Command("slingshot") {
                {
                    children(configCommand);
                }
            });
        });

        Bukkit.getPluginManager().registerEvents(this, this);

        new BukkitRunnable() {
            final ProtocolManager manager = ProtocolLibrary.getProtocolManager();

            @Override
            public void run() {
                for (Map.Entry<UUID, Pole> entry : uuidPoleMap.entrySet()) {
                    Pole pole = entry.getValue();
                    if (pole == null) {
                        continue;
                    }
                    Location loc = pole.particleLocation();

                    PacketContainer packet = manager.createPacket(PacketType.Play.Server.WORLD_PARTICLES);
                    packet.getDoubles()
                            .write(0, loc.getX())
                            .write(1, loc.getY() + 0.5)
                            .write(2, loc.getZ());
                    packet.getNewParticles().write(0, WrappedParticle.create(Particle.SOUL_FIRE_FLAME, null));

                    sendPacket(entry.getKey(), packet);
                }
            }

            private void sendPacket(UUID uuid, PacketContainer packet) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    try {
                        manager.sendServerPacket(p, packet);
                    } catch (InvocationTargetException ignored) {
                        ignored.printStackTrace();
                    }
                }
            }
        }.runTaskTimer(this, 0, 4);

        DedicatedServer server = ((CraftServer) Bukkit.getServer()).getHandle().getServer();
        try {
            Field field = MinecraftServer.class.getDeclaredField("allowFlight");
            field.setAccessible(true);
            field.set(server, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
    }

    @EventHandler
    private void onPlayerInteractWall(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = e.getItem();
        if (item == null || item.getType() != Material.LEAD) {
            return;
        }

        Block b = e.getClickedBlock();
        if (!(b.getBlockData() instanceof Wall)) {
            return;
        }

        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();

        Pole pole1 = uuidPoleMap.get(uuid);
        if (pole1 == null) {
            if (uuidPoleMap.compute(uuid, (k, v) -> Pole.of(b, this)) != null) {
                p.sendMessage(ChatColor.LIGHT_PURPLE + "1つ目の支柱を設定しました.2つ目を選択してください.");
            }

            return;
        }

        Pole pole2 = Pole.of(b, this);
        if (pole2 != null) {
            if (Slingshot.of(pole1, pole2, config, this) != null) {
                p.sendMessage(ChatColor.GREEN + "パチンコを生成しました.");
                uuidPoleMap.remove(uuid);
            } else {
                pole2.unassign();
                p.sendMessage(ChatColor.RED + "支柱の間隔が広すぎます.6マス以内のものを選択してください.");
            }
        }
    }

    @EventHandler
    private void onItemChange(PlayerItemHeldEvent e) {
        Player p = e.getPlayer();

        Pole pole = uuidPoleMap.remove(p.getUniqueId());
        if (pole != null) {
            pole.unassign();
            p.sendMessage(ChatColor.LIGHT_PURPLE + "支柱選択を中断しました.");
        }
    }
}