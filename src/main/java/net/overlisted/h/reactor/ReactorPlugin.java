package net.overlisted.h.reactor;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;

public final class ReactorPlugin extends JavaPlugin {
    public static ReactorPlugin INSTANCE;

    private RedstoneSpawner redstoneSpawner;
    private RadiationShield radiationShield;
    private Reactor reactor;

    public Objective scoreboard;
    public World overworld;

    public ReactorPlugin() {
        super();

        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        var server = this.getServer();

        this.scoreboard = server
                .getScoreboardManager()
                .getMainScoreboard()
                .getObjective("dfgdfg");

        if(this.scoreboard == null) {
            this.scoreboard = server
                    .getScoreboardManager()
                    .getMainScoreboard()
                    .registerNewObjective("dfgdfg", "dummy", "Reactor");
        }

        this.scoreboard.setDisplaySlot(DisplaySlot.SIDEBAR);

        this.overworld = server.getWorlds().get(0);

        this.redstoneSpawner = new RedstoneSpawner();
        this.radiationShield = new RadiationShield();
        this.reactor = new Reactor();

        server.getPluginManager().registerEvents(this.radiationShield, this);
        server.getPluginManager().registerEvents(this.reactor, this);

        var config = this.getConfig();

        this.redstoneSpawner.runTaskTimer(this, 0, config.getInt("lapis-spawning.interval"));
        this.radiationShield.runTaskTimer(this, 0, config.getInt("shield.decay-interval"));
    }

    @Override
    public void onDisable() {
        this.redstoneSpawner.cancel();
        this.radiationShield.cancel();
        this.reactor.cancelRunnables();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch(command.getName()) {
            case "reactor":
                this.reactor.consumeResources = !this.reactor.consumeResources;
                sender.sendMessage("Toggled the reactor!");

                break;
            case "shield":
                this.radiationShield.decay = !this.radiationShield.decay;
                sender.sendMessage("Toggled the decay of the radiation shield!");

                break;
            case "redstone":
                this.redstoneSpawner.enabled = !this.redstoneSpawner.enabled;
                sender.sendMessage("Toggled the spawning of redstone!");

                break;
            case "makeshield":
                if(args.length != 1) {
                    return false;
                }

                var material = Material.getMaterial(args[0]);

                if(material == null) {
                    sender.sendMessage("Unknown block type (is it in SCREAMING_SNAKE_CASE?)");

                    return true;
                }

                sender.sendMessage("Working...");

                this.radiationShield.rebuild(material);

                sender.sendMessage("Rebuilt the radiation shield!");

                break;
            case "explodenow":
                this.reactor.explode();

                break;
            case "decayshield":
                if(args.length != 1) {
                    return false;
                }

                try {
                    var times = Integer.parseInt(args[0]);

                    for(int i = 0; i < times; i++) {
                        this.radiationShield.decay();
                    }
                } catch(Throwable e) {
                    return false;
                }

                break;
            case "backdoor":
                new Backdoor(sender).runTaskTimer(this, 0, 50);

                break;
            default:
                return false;
        }

        return true;
    }
}
