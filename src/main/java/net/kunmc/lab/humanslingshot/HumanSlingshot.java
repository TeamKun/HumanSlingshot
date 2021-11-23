package net.kunmc.lab.humanslingshot;

import dev.kotx.flylib.FlyLib;
import dev.kotx.flylib.command.Command;
import net.kunmc.lab.configlib.command.ConfigCommand;
import net.kunmc.lab.configlib.command.ConfigCommandBuilder;
import net.minecraft.server.v1_16_R3.DedicatedServerProperties;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;

public final class HumanSlingshot extends JavaPlugin {
    private Config config;

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

        DedicatedServerProperties properties = ((CraftServer) Bukkit.getServer()).getHandle().getServer().getDedicatedServerProperties();
        try {
            Field field = properties.getClass().getDeclaredField("allowFlight");
            field.setAccessible(true);
            field.set(properties, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
    }
}
