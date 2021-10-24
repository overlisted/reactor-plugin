package net.overlisted.h.reactor;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;

public final class ReactorPlugin extends JavaPlugin {
    public static ReactorPlugin INSTANCE;

    private LapisSpawner lapisSpawner;
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
                    .registerNewObjective("dfgdfg", "dummy", Component.text("Reactor"));
        }

        this.scoreboard.setDisplaySlot(DisplaySlot.SIDEBAR);

        this.overworld = server.getWorlds().get(0);

        this.lapisSpawner = new LapisSpawner();
        this.radiationShield = new RadiationShield();
        this.reactor = new Reactor();

        server.getPluginManager().registerEvents(this.radiationShield, this);

        var config = this.getConfig();

        this.lapisSpawner.runTaskTimer(this, 0, config.getInt("lapis-spawning.interval"));
        this.radiationShield.runTaskTimer(this, 0, config.getInt("shield.decay-interval"));
        this.reactor.runTaskTimer(this, 0, 20);
    }

    @Override
    public void onDisable() {
        this.lapisSpawner.cancel();
        this.radiationShield.cancel();
        this.reactor.cancel();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch(command.getName()) {
            case "reactor":
                this.reactor.consumeResources = true;
                sender.sendMessage("Turned on the reactor!");

                break;
            case "shield":
                this.radiationShield.decay = true;
                sender.sendMessage("Turned on the decay of the radiation shield!");

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
                sender.sendMessage("ok");

                break;
            default:
                return false;
        }

        return true;
    }
}
