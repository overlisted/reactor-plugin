package net.overlisted.h.reactor;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class RadiationShield extends BukkitRunnable implements Listener {
    private final ConfigurationSection config;

    int fullIntegrity;
    Random random = new Random();
    int integrity;
    BossBar integrityBar;

    public boolean decay = false;

    public RadiationShield() {
        this.config = ReactorPlugin.INSTANCE.getConfig().getConfigurationSection("shield");

        var server = ReactorPlugin.INSTANCE.getServer();

        var sideLength = this.config.getInt("side-length");
        var minY = this.config.getInt("min-y");
        var maxY = this.config.getInt("max-y");
        this.fullIntegrity = (sideLength - 1) * 4 * (maxY - minY) + (sideLength - 2) * (sideLength - 2);
        this.integrity = this.fullIntegrity;
        this.integrityBar = server.createBossBar(
                "Radiation Shield",
                BarColor.WHITE,
                BarStyle.SEGMENTED_10
        );

        this.updateIntegrity();
    }

    public void rebuild(Material material) {
        var world = ReactorPlugin.INSTANCE.overworld;
        var halfSideLen = this.config.getInt("side-length") / 2;
        var minY = this.config.getInt("min-y");
        var maxY = this.config.getInt("max-y");

        for(int side = 0; side < 4; side++) {
            for(int sidePos = -halfSideLen; sidePos <= halfSideLen; sidePos++) {
                for(int y = minY; y < maxY; y++) {
                    var x = switch(side) {
                        case 0, 1 -> sidePos;
                        case 2 -> halfSideLen;
                        case 3 -> -halfSideLen;
                        default -> throw new RuntimeException("how");
                    };
                    var z = switch(side) {
                        case 0 -> halfSideLen;
                        case 1 -> -halfSideLen;
                        case 2, 3 -> sidePos;
                        default -> throw new RuntimeException("how");
                    };

                    world.getBlockAt(x, y, z).setType(material);
                }
            }
        }

        for(int x = -halfSideLen; x <= halfSideLen; x++) {
            for(int z = -halfSideLen; z <= halfSideLen; z++) {
                world.getBlockAt(x, maxY, z).setType(material);
            }
        }

        this.integrity = this.fullIntegrity;
        this.updateIntegrity();
    }

    public void decay() {
        // this is really smart i love being smart

        var side = this.config.getInt("side-length");
        var neg = this.random.nextBoolean();
        var halfSide = side / 2;
        if(neg) halfSide = -halfSide;
        var minY = this.config.getInt("min-y");
        var maxY = this.config.getInt("max-y");

        var sidePos = this.random.nextInt(side + 1) - side / 2;
        var axis = this.random.nextInt(3); // 0 = x, 1 = z, 2 = ceiling

        var x = switch(axis) {
            case 0 -> sidePos;
            case 1 -> halfSide;
            case 2 -> this.random.nextInt(side) - side / 2;
            default -> throw new RuntimeException("how");
        };
        var y = switch(axis) {
            case 0, 1 -> random.nextInt(maxY - minY + 1) + minY;
            case 2 -> maxY;
            default -> throw new RuntimeException("how");
        };
        var z = switch(axis) {
            case 0 -> halfSide;
            case 1 -> sidePos;
            case 2 -> this.random.nextInt(side) - side / 2;
            default -> throw new RuntimeException("how");
        };

        var block = ReactorPlugin.INSTANCE.overworld.getBlockAt(x, y, z);

        if(!block.isEmpty()) {
            block.setType(Material.AIR);
            this.integrity--;

            this.updateIntegrity();
        }
    }

    @Override
    public void run() {
        var frac = (double) this.integrity / (double) this.fullIntegrity;

        if(this.integrity == 0) {
            var poison = new PotionEffect(PotionEffectType.POISON, 2, 1);

            ReactorPlugin.INSTANCE
                    .getServer()
                    .getOnlinePlayers()
                    .forEach(it -> it.addPotionEffect(poison));

            var component = Component.text(
                    "Nothing is protecting you from radiation",
                    Style.style().color(TextColor.color(Color.RED.asRGB())).decorate(TextDecoration.BOLD).build()
            );

            ReactorPlugin.INSTANCE
                    .getServer()
                    .getOnlinePlayers()
                    .forEach(it -> it.sendActionBar(component));
        } else if(frac < 0.5) {
            var poison = new PotionEffect(PotionEffectType.POISON, 2, 2);

            ReactorPlugin.INSTANCE
                    .getServer()
                    .getOnlinePlayers()
                    .forEach(it -> it.addPotionEffect(poison));

            var component = Component.text(
                    "The radiation shield is getting very weak",
                    Style.style().color(TextColor.color(Color.YELLOW.asRGB())).build()
            );

            ReactorPlugin.INSTANCE
                    .getServer()
                    .getOnlinePlayers()
                    .forEach(it -> it.sendActionBar(component));
        }

        if(this.decay) {
            this.decay();
        }
    }

    private void updateIntegrity() {
        var frac = (double) this.integrity / (double) this.fullIntegrity;

        this.integrityBar.setProgress(frac);
    }

    private boolean isBlockIn(Location loc) {
        var halfSide = this.config.getInt("side-length") / 2;
        var minY = this.config.getInt("min-y");
        var maxY = this.config.getInt("max-y");

        var x = loc.getBlockX();
        var y = loc.getBlockY();
        var z = loc.getBlockZ();
        var wall = y >= minY && y < maxY && (Math.abs(x) == halfSide || Math.abs(z) == halfSide);
        var ceiling = y == maxY && Math.abs(x) <= halfSide && Math.abs(z) <= halfSide;

        return wall || ceiling;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if(this.isBlockIn(event.getBlock().getLocation())) {
            this.integrity++;
            this.updateIntegrity();
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if(this.isBlockIn(event.getBlock().getLocation())) {
            this.integrity--;
            this.updateIntegrity();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        this.integrityBar.addPlayer(event.getPlayer());
    }
}
