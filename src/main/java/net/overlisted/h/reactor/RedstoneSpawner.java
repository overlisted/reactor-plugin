package net.overlisted.h.reactor;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class RedstoneSpawner extends BukkitRunnable {
    private final ConfigurationSection config;

    Random random = new Random();

    public RedstoneSpawner() {
        this.config = ReactorPlugin.INSTANCE.getConfig().getConfigurationSection("redstone-spawning");
    }

    @Override
    public void run() {
        var center_x = this.config.getInt("center.x");
        var center_z = this.config.getInt("center.z");
        var radius = this.config.getInt("radius");
        var minY = this.config.getInt("min-y");
        var maxY = this.config.getInt("max-y");

        var loc = new Location(
                ReactorPlugin.INSTANCE.overworld,
                (double) center_x + (random.nextInt(radius * 2) - radius),
                (double) random.nextInt(maxY - minY) + minY,
                (double) center_z + (random.nextInt(radius * 2) - radius)
        );

        var block = loc.getBlock();

        if(block.getType() == Material.STONE) {
            loc.getBlock().setType(Material.REDSTONE_ORE);
        }
    }
}
