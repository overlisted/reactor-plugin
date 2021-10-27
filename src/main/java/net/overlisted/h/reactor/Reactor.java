package net.overlisted.h.reactor;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class Reactor extends BukkitRunnable implements Listener {
    private final ConfigurationSection config;
    private final ReactorMaterial[] materials;

    public boolean consumeResources = false;

    public Reactor() {
        this.config = ReactorPlugin.INSTANCE.getConfig().getConfigurationSection("reactor");
        this.materials = new ReactorMaterial[] {
                new ReactorMaterial("Fuel", Material.REDSTONE, config.getConfigurationSection("fuel")),
                new ReactorMaterial("Coolant", Material.QUARTZ, config.getConfigurationSection("coolant"))
        };
    }

    public void explode() {
        this.consumeResources = false;

        var server = ReactorPlugin.INSTANCE.getServer();

        server
                .getOnlinePlayers()
                .forEach(it -> it.sendTitle(
                        "Reactor is about to explode!",
                        "You have 1 minute to evacuate",
                        10,
                        1180,
                        10
                ));

        var task = new BukkitRunnable() {
            @Override
            public void run() {
                var config = Reactor.this.config.getConfigurationSection("tnt");
                var world = ReactorPlugin.INSTANCE.overworld;

                for(int x = config.getInt("fill-min.x"); x <= config.getInt("fill-max.x"); x++) {
                    for(int y = config.getInt("fill-min.y"); y <= config.getInt("fill-max.y"); y++) {
                        for(int z = config.getInt("fill-min.z"); z <= config.getInt("fill-max.z"); z++) {
                            world.getBlockAt(x, y, z).setType(Material.TNT);
                        }
                    }
                }

                var primed = new Location(
                        world,
                        config.getDouble("primed.x"),
                        config.getDouble("primed.y"),
                        config.getDouble("primed.z")
                );

                primed.getBlock().setType(Material.AIR);
                world.spawnEntity(primed, EntityType.PRIMED_TNT);
            }
        };

        task.runTaskLater(ReactorPlugin.INSTANCE, 1200);
    }

    @Override
    public void run() {
        if(this.consumeResources) {
            for(ReactorMaterial material: this.materials) {
                material.run();
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        for(ReactorMaterial material: this.materials) {
            material.onBlockPlace(event);
        }
    }
}
