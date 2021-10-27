package net.overlisted.h.reactor;

import org.bukkit.Material;
import org.bukkit.block.Hopper;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Score;

public class ReactorMaterial {
    private final ConfigurationSection config;
    private int value;
    private final Material material;
    private Hopper hopper;
    private Score scoreboardScore;

    public ReactorMaterial(String displayName, Material material, ConfigurationSection config) {
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
    }

    public void run() {
        var inv = this.hopper.getInventory();

        for(ItemStack stack: inv.getContents()) {
            if(stack == null) {
                continue;
            }

            if (stack.getType() == this.material) {
                this.value += stack.getAmount();
            }
        }

        inv.removeItem(new ItemStack(this.material, 64));

        this.value -= this.config.getInt("consumption");

        this.scoreboardScore.setScore(this.value);
    }
}
