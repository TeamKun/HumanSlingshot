package net.kunmc.lab.humanslingshot;

import org.bukkit.plugin.java.JavaPlugin;

public final class HumanSlingshot extends JavaPlugin {
    private Config config;

    @Override
    public void onEnable() {
        config = new Config(this);
    }

    @Override
    public void onDisable() {
    }
}
