package net.overlisted.h.reactor;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class Reactor implements Listener {
    private final ConfigurationSection config;
    private final ReactorResource[] resources;

    public boolean consumeResources = false;

    public Reactor() {
        this.config = ReactorPlugin.INSTANCE.getConfig().getConfigurationSection("reactor");
        this.resources = new ReactorResource[] {
                new ReactorResource(this, "Fuel", Material.QUARTZ, config.getConfigurationSection("fuel")),
                new ReactorResource(this, "Coolant", Material.REDSTONE, config.getConfigurationSection("coolant"))
        };
    }

    public void explode() {
        this.consumeResources = false;

        var server = ReactorPlugin.INSTANCE.getServer();

        server
                .getOnlinePlayers()
                .forEach(it -> it.sendTitle(
                        "\u00A74NUCLEAR MELTDOWN",
                        "You have \u00A7b3 minutes\u00A7r to evacuate",
                        10,
                        200,
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

        task.runTaskLater(ReactorPlugin.INSTANCE, 3600);
    }

    public void cancelRunnables() {
        for(ReactorResource resource: this.resources) {
            resource.cancel();
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        for(ReactorResource resource: this.resources) {
            resource.onBlockPlace(event);
        }
    }
}
