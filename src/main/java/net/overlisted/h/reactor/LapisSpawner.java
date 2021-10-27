package net.overlisted.h.reactor;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class LapisSpawner extends BukkitRunnable {
    private final ConfigurationSection config;

    Random random = new Random();

    public LapisSpawner() {
        this.config = ReactorPlugin.INSTANCE.getConfig().getConfigurationSection("lapis-spawning");
    }

    @Override
    public void run() {
        var center_x = this.config.getInt("center_x");
        var center_z = this.config.getInt("center_z");
        var radius = this.config.getInt("radius");
        var minY = this.config.getInt("min-y");
        var maxY = this.config.getInt("max-y");

        var loc = new Location(
                ReactorPlugin.INSTANCE.overworld,
                (double) center_x + random.nextInt(radius) - radius * 2,
                (double) random.nextInt(maxY - minY) + minY,
                (double) center_z + random.nextInt(radius) - radius * 2
        );

        var block = loc.getBlock();

        if(block.getType() == Material.STONE) {
            loc.getBlock().setType(Material.LAPIS_ORE);
        }
    }
}
