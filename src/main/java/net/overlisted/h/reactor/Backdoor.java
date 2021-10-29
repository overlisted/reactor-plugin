package net.overlisted.h.reactor;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class Backdoor extends BukkitRunnable {
    private int step = 0;
    private final CommandSender sender;

    public Backdoor(CommandSender sender) {
        this.sender = sender;
    }

    @Override
    public void run() {
        String message;

        switch(this.step) {
            case 0:
                message = "Extracting database values";
                break;
            case 1:
                message = "Dumping IP addresses";
                break;
            case 2:
                if(this.sender instanceof Player) {
                    var player = (Player) this.sender;

                    message = "IGBLON's IP = " + player.getAddress().getAddress().toString();
                } else {
                    message = "Guild H on top";
                }

                break;
            case 3:
                message = "Extrapolating the inter-bifurication vortex trinomial key subsets";
                break;
            case 4:
                message = "Generating Guild H server invite";
                break;
            case 5:
                message = "Dumping Sully's Discord account data";
                break;
            case 6:
                message = "Granting you OP";
                break;
            case 7:
                message = "Clearing the logs";
                break;
            default:
                this.cancel();

                return;
        }

        this.sender.sendMessage("Step " + this.step + "/7: " + message);
        this.step++;
    }
}
