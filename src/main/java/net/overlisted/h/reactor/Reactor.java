package net.overlisted.h.reactor;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Hopper;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Score;

import java.time.Duration;

public class Reactor extends BukkitRunnable {
    private final ConfigurationSection config;

    Hopper hopper;
    int fuel;
    int coolant;
    Score fuelEntry = ReactorPlugin.INSTANCE.scoreboard.getScore("Fuel");
    Score coolantEntry = ReactorPlugin.INSTANCE.scoreboard.getScore("Coolant");

    public boolean consumeResources = false;

    public Reactor() {
        this.config = ReactorPlugin.INSTANCE.getConfig().getConfigurationSection("reactor");

        var hopper_pos = config.getConfigurationSection("hopper");
        var block = ReactorPlugin.INSTANCE.overworld.getBlockAt(
                hopper_pos.getInt("x"),
                hopper_pos.getInt("y"),
                hopper_pos.getInt("z")
        );

        if(!(block.getState() instanceof Hopper)) {
            block.setType(Material.HOPPER);

            var coords = "(" + hopper_pos.getInt("x") + ", " + hopper_pos.getInt("y") + ", " + hopper_pos.getInt("z") + ")";
            ReactorPlugin.INSTANCE.getLogger().warning("Changed the block at " + coords + " to a hopper");
        }

        this.hopper = (Hopper) block.getState();

        this.fuel = config.getInt("initial-fuel");
        this.coolant = config.getInt("initial-coolant");

        this.updateEntries();
    }

    private void updateEntries() {
        this.fuelEntry.setScore(this.fuel);
        this.coolantEntry.setScore(this.coolant);
    }

    public void explode() {
        this.consumeResources = false;

        var server = ReactorPlugin.INSTANCE.getServer();
        var title = Title.title(
                Component.text(
                        "Reactor is about to explode!",
                        Style.style().decorate(TextDecoration.BOLD).build()
                ),
                Component.text(
                        "You have 1 minute to evacuate",
                        Style.style().color(TextColor.color(Color.RED.asRGB())).build()
                ),
                Title.Times.of(Duration.ZERO, Duration.ofMinutes(1), Duration.ZERO)
        );

        server.showTitle(title);

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
        var inv = this.hopper.getInventory();

        for(ItemStack stack: inv.getContents()) {
            if(stack == null) {
                continue;
            }

            if (stack.getType() == Material.QUARTZ) {
                this.fuel += stack.getAmount();
            }

            if (stack.getType() == Material.LAPIS_LAZULI) {
                this.coolant += stack.getAmount();
            }
        }

        inv.removeItem(new ItemStack(Material.LAPIS_LAZULI, 64), new ItemStack(Material.QUARTZ, 64));

        if(this.consumeResources) {
            this.fuel -= this.config.getInt("fuel-consumption");
            this.coolant -= this.config.getInt("coolant-consumption");

            if(this.fuel <= 0 || this.coolant <= 0) {
                this.explode();
            }
        }

        this.updateEntries();
    }
}
