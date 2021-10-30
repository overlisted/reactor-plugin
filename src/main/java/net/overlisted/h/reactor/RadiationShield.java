package net.overlisted.h.reactor;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
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
        this.integrityBar = server.createBossBar(
                "Radiation Shield",
                BarColor.WHITE,
                BarStyle.SEGMENTED_10
        );
        this.updateIntegrity(this.fullIntegrity);
    }

    public void rebuild(Material material) {
        var world = ReactorPlugin.INSTANCE.overworld;
        var halfSideLen = this.config.getInt("side-length") / 2;
        var minY = this.config.getInt("min-y");
        var maxY = this.config.getInt("max-y");
        var center_x = this.config.getInt("center.x");
        var center_z = this.config.getInt("center.z");

        for(int side = 0; side < 4; side++) {
            for(int sidePos = -halfSideLen; sidePos <= halfSideLen; sidePos++) {
                for(int y = minY; y < maxY; y++) {
                    var x = center_x + switch(side) {
                        case 0, 1 -> sidePos;
                        case 2 -> halfSideLen;
                        case 3 -> -halfSideLen;
                        default -> throw new RuntimeException("how");
                    };
                    var z = center_z + switch(side) {
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
                world.getBlockAt(center_x + x, maxY, center_z + z).setType(material);
            }
        }

        this.updateIntegrity(this.fullIntegrity);
    }

    private void updateIntegrity(int newValue) {
        var newFrac = (double) newValue / (double) this.fullIntegrity;

        this.integrityBar.setProgress(newFrac);
        this.integrity = newValue;
    }

    public void decay() {
        // this is really smart i love being smart

        var center_x = this.config.getInt("center.x");
        var center_z = this.config.getInt("center.z");
        var side = this.config.getInt("side-length");
        var neg = this.random.nextBoolean();
        var halfSide = side / 2;
        if(neg) halfSide = -halfSide;
        var minY = this.config.getInt("min-y");
        var maxY = this.config.getInt("max-y");

        var sidePos = this.random.nextInt(side + 1) - side / 2;
        var axis = this.random.nextInt(3); // 0 = x, 1 = z, 2 = ceiling

        var x = center_x + switch(axis) {
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
        var z = center_z + switch(axis) {
            case 0 -> halfSide;
            case 1 -> sidePos;
            case 2 -> this.random.nextInt(side) - side / 2;
            default -> throw new RuntimeException("how");
        };

        var block = ReactorPlugin.INSTANCE.overworld.getBlockAt(x, y, z);

        if(!block.isEmpty()) {
            block.setType(Material.AIR);
            updateIntegrity(this.integrity - 1);
        }
    }

    @Override
    public void run() {
        var frac = (double) this.integrity / (double) this.fullIntegrity;

        PotionEffect effect = null;
        BaseComponent[] component = null;

        if(this.integrity == 0) {
            effect = new PotionEffect(PotionEffectType.POISON, 200, 2);
            component = TextComponent.fromLegacyText("\u00A74Nothing is protecting you from radiation");
        } else if(frac < 0.5) {
            effect = new PotionEffect(PotionEffectType.POISON, 200, 1);
            component = TextComponent.fromLegacyText("\u00A7eThe radiation shield is getting very weak");
        }

        for(Player player: ReactorPlugin.INSTANCE.getServer().getOnlinePlayers()) {
            if(effect != null) {
                if(player.getActivePotionEffects().stream().noneMatch(it -> it.getType().equals(PotionEffectType.POISON))) {
                    player.addPotionEffect(effect);
                }
            }

            if(component != null) player.spigot().sendMessage(ChatMessageType.ACTION_BAR, component);
        }

        if(this.decay) {
            this.decay();
        }
    }

    private boolean isBlockIn(Location loc) {
        var center_x = this.config.getInt("center.x");
        var center_z = this.config.getInt("center.z");
        var halfSide = this.config.getInt("side-length") / 2;
        var minY = this.config.getInt("min-y");
        var maxY = this.config.getInt("max-y");

        var x = loc.getBlockX() - center_x;
        var y = loc.getBlockY();
        var z = loc.getBlockZ() - center_z;
        var wall = y >= minY && y < maxY && (Math.abs(x) == halfSide || Math.abs(z) == halfSide);
        var ceiling = y == maxY && Math.abs(x) <= halfSide && Math.abs(z) <= halfSide;

        return wall || ceiling;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if(this.isBlockIn(event.getBlock().getLocation())) {
            this.updateIntegrity(this.integrity + 1);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if(this.isBlockIn(event.getBlock().getLocation())) {
            this.updateIntegrity(this.integrity - 1);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        this.integrityBar.addPlayer(event.getPlayer());
    }
}
