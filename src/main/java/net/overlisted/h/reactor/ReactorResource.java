package net.overlisted.h.reactor;

import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.block.Hopper;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Score;

public class ReactorResource extends BukkitRunnable {
    private static final long WARNING_DELAY = 20000;
    private final ConfigurationSection config;
    private int value;
    private final Material material;
    private Hopper hopper;
    private final Score scoreboardScore;
    private final String name;
    private long lastWarningTime = 0;
    private Reactor reactor;

    public ReactorResource(Reactor reactor, String displayName, Material material, ConfigurationSection config) {
        this.config = config;
        this.value = config.getInt("initial");
        this.material = material;

        var hopper_pos = config.getConfigurationSection("hopper");
        var block = ReactorPlugin.INSTANCE.overworld.getBlockAt(
                hopper_pos.getInt("x"),
                hopper_pos.getInt("y"),
                hopper_pos.getInt("z")
        );

        if(block.getType() != Material.HOPPER) {
            block.setType(Material.HOPPER);

            var coords = "(" + hopper_pos.getInt("x") + ", " + hopper_pos.getInt("y") + ", " + hopper_pos.getInt("z") + ")";
            ReactorPlugin.INSTANCE.getLogger().warning("Changed the block at " + coords + " to a hopper");
        }

        this.hopper = (Hopper) block.getState();

        this.scoreboardScore = ReactorPlugin.INSTANCE.scoreboard.getScore(displayName);
        this.scoreboardScore.setScore(this.value);

        this.name = displayName.toLowerCase();
        this.reactor = reactor;

        this.runTaskTimer(ReactorPlugin.INSTANCE, 0, this.config.getInt("interval"));
    }

    private void updateValue(int delta) {
        var warn_min = this.config.getInt("warning.min");
        var warn_max = this.config.getInt("warning.max");

        var newValue = this.value + delta;

        String warning = null;

        if(newValue < warn_min) {
            warning = "\u00A7e\u00A7lWarning: You are adding too little ";
        } else if(newValue > warn_max) {
            warning = "\u00A7e\u00A7lWarning: You are adding too much ";
        }

        var now = System.currentTimeMillis();

        if(warning != null && this.lastWarningTime + WARNING_DELAY < now) {
            this.lastWarningTime = now;
            ReactorPlugin.INSTANCE.getServer().spigot().broadcast(TextComponent.fromLegacyText(warning + this.name));
        }

        this.value = newValue;
    }

    @Override
    public void run() {
        var inv = this.hopper.getInventory();

        for(ItemStack stack: inv.getContents()) {
            if(stack == null) {
                continue;
            }

            if (stack.getType() == this.material) {
                this.updateValue(stack.getAmount());
            }
        }

        inv.removeItem(new ItemStack(this.material, 64));

        if(this.reactor.consumeResources) this.updateValue(-this.config.getInt("consumption"));

        this.scoreboardScore.setScore(this.value);

        if(this.value < this.config.getInt("meltdown.min") || this.value > this.config.getInt("meltdown.max")) {
            this.reactor.explode();
        }
    }

    public void onBlockPlace(BlockPlaceEvent event) {
        var loc = event.getBlock().getLocation();

        if(event.getBlock().getType() == Material.HOPPER) {
            if(loc.getBlockX() == this.config.getInt("hopper.x")
                    && loc.getBlockY() == this.config.getInt("hopper.y")
                    && loc.getBlockZ() == this.config.getInt("hopper.z")
            ) {
                this.hopper = (Hopper) event.getBlock().getState();
            }
        }
    }
}
