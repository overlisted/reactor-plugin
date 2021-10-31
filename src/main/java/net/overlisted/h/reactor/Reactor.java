package net.overlisted.h.reactor;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.boss.BarColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.UUID;

public class Reactor implements Listener {
    private final ConfigurationSection config;
    private final ReactorResource[] resources;

    private final double safezoneLowX;
    private final double safezoneLowY;
    private final double safezoneLowZ;

    private final double safezoneHighX;
    private final double safezoneHighY;
    private final double safezoneHighZ;

    private final int contaminationRate;
    private final int contaminationAmount;
    private final int explosionRate;
    private final int explosionAmount;

    public boolean consumeResources = false;
    private boolean exploding = false;

    public Reactor() {
        this.config = ReactorPlugin.INSTANCE.getConfig().getConfigurationSection("reactor");
        this.resources = new ReactorResource[] {
                new ReactorResource(this, "Fuel", Material.QUARTZ, config.getConfigurationSection("fuel")),
                new ReactorResource(this, "Coolant", Material.REDSTONE, config.getConfigurationSection("coolant"))
        };

        var safezoneConfig = ReactorPlugin.INSTANCE.getConfig().getConfigurationSection("safezone");
        var meltdownConfig = ReactorPlugin.INSTANCE.getConfig().getConfigurationSection("meltdown");

        this.safezoneLowX = safezoneConfig.getInt("low-pos.x");
        this.safezoneLowY = safezoneConfig.getInt("low-pos.y");
        this.safezoneLowZ = safezoneConfig.getInt("low-pos.z");

        this.safezoneHighX = safezoneConfig.getInt("high-pos.x");
        this.safezoneHighY = safezoneConfig.getInt("high-pos.y");
        this.safezoneHighZ = safezoneConfig.getInt("high-pos.z");

        this.contaminationRate = meltdownConfig.getInt("contamination-rate");
        this.contaminationAmount = meltdownConfig.getInt("contamination-amt");
        this.explosionRate = meltdownConfig.getInt("explosion-rate");
        this.explosionAmount = meltdownConfig.getInt("explosion-amount");
    }

    public void explode() {
        //sully bullshit meltdown logic free 2021 no virus
        this.exploding = true;
        var beenToSafezone = new ArrayList<UUID>();

        ReactorPlugin.INSTANCE.getServer().getScheduler().scheduleSyncRepeatingTask(ReactorPlugin.INSTANCE, () -> {
            for (var player : ReactorPlugin.INSTANCE.getServer().getOnlinePlayers()) {
                double x = player.getLocation().getX();
                double y = player.getLocation().getY();
                double z = player.getLocation().getZ();

                if ((x <= this.safezoneHighX && x >= this.safezoneLowX) &&
                        (y <= this.safezoneHighY && y >= this.safezoneLowY) &&
                            (z <= this.safezoneHighZ && z >= this.safezoneLowZ)) {
                    if (!beenToSafezone.contains(player.getUniqueId())) {
                        beenToSafezone.add(player.getUniqueId());
                        ReactorPlugin.INSTANCE.getServer().broadcastMessage(
                                ChatColor.GREEN + player.getDisplayName() + ChatColor.RESET
                                        + ChatColor.YELLOW + " has made it to safety!"
                        );
                    }
                } else {
                    if (player.getHealth() - 2 >= 0) {
                        player.setHealth(player.getHealth() - 2);
                    } else player.setHealth(0);
                }
            }
        }, 0, 40);

        //Make random blocks turn into netherrack
        //Make random blocks explode
        var borderCenter = ReactorPlugin.INSTANCE.overworld.getWorldBorder().getCenter();
        var borderSize = ReactorPlugin.INSTANCE.overworld.getWorldBorder().getSize();

        ReactorPlugin.INSTANCE.getServer().getScheduler().scheduleSyncRepeatingTask(ReactorPlugin.INSTANCE, () -> {
            for (int i = 0; i < contaminationAmount; i++) {
                int x;
                int z;

                if (Math.random() > 0.5) {
                    x = borderCenter.getBlockX() - (int) (Math.random() * borderSize);
                    z = borderCenter.getBlockZ() - (int) (Math.random() * borderSize);
                } else {
                    x = borderCenter.getBlockX() + (int) (Math.random() * borderSize);
                    z = borderCenter.getBlockZ() + (int) (Math.random() * borderSize);
                }

                int y = ReactorPlugin.INSTANCE.overworld.getHighestBlockYAt(x, z);

                var block = ReactorPlugin.INSTANCE.overworld.getBlockAt(new Location(
                        ReactorPlugin.INSTANCE.overworld,
                        x,
                        y,
                        z
                ));

                block.setType(Material.NETHERRACK);

                //Set every other netherrack block on fire
                if (i % 2 == 0) {
                    block.getRelative(BlockFace.UP).setType(Material.FIRE);
                }
            }
        }, 0, contaminationRate);

        ReactorPlugin.INSTANCE.getServer().getScheduler().scheduleSyncRepeatingTask(ReactorPlugin.INSTANCE, () -> {
            for (int i = 0; i < explosionAmount; i++) {
                int x;
                int z;

                if (Math.random() > 0.5) {
                    x = borderCenter.getBlockX() - (int) (Math.random() * borderSize);
                    z = borderCenter.getBlockZ() - (int) (Math.random() * borderSize);
                } else {
                    x = borderCenter.getBlockX() + (int) (Math.random() * borderSize);
                    z = borderCenter.getBlockZ() + (int) (Math.random() * borderSize);
                }

                int y = ReactorPlugin.INSTANCE.overworld.getHighestBlockYAt(x, z);

                var block = ReactorPlugin.INSTANCE.overworld.getBlockAt(new Location(
                        ReactorPlugin.INSTANCE.overworld,
                        x,
                        y,
                        z
                ));

                if (!block.isEmpty() && !block.getType().equals(Material.FIRE) && !block.getType().equals(Material.BEDROCK)) {
                    ReactorPlugin.INSTANCE.overworld.createExplosion(block.getLocation(), 4.0f);
                }

            }
        }, 0, explosionRate);

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

        new TimerBossBar(
                server.getOnlinePlayers(),
                3600,
                "Evacuation",
                BarColor.YELLOW
        );

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

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (exploding) {
            event.getEntity().setGameMode(GameMode.SPECTATOR);
        }
    }
}
