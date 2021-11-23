package net.kunmc.lab.humanslingshot;

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
