package net.kunmc.lab.humanslingshot;

import net.kunmc.lab.configlib.config.BaseConfig;
import net.kunmc.lab.configlib.value.DoubleValue;
import net.kunmc.lab.configlib.value.FloatValue;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class Config extends BaseConfig {
    public DoubleValue maxPullDistance = new DoubleValue(12.0, 9.1, 100.0);
    public FloatValue explosionMagnification = new FloatValue(1.0F);
    public DoubleValue launchPowerMagnification = new DoubleValue(1.0);

    public Config(@NotNull Plugin plugin) {
        super(plugin, "");
    }
}
