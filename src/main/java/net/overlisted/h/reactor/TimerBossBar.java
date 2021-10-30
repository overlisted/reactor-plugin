package net.overlisted.h.reactor;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

public class TimerBossBar extends BukkitRunnable {
    private final BossBar bar;
    private int passed;
    private final int ticks;

    public TimerBossBar(Collection<? extends Player> players, int ticks, String title, BarColor color) {
        this.passed = 0;
        this.ticks = ticks;

        this.bar = ReactorPlugin.INSTANCE.getServer().createBossBar(title, color, BarStyle.SOLID);
        this.bar.setProgress(1.0);
        players.forEach(this.bar::addPlayer);

        this.runTaskTimer(ReactorPlugin.INSTANCE, 0, 1);
    }

    @Override
    public void run() {
        this.passed++;

        var frac = (double) this.passed / (double) this.ticks;

        if(frac >= 1.0) {
            this.cancel();
            this.bar.setVisible(false);
        }

        this.bar.setProgress(1.0 - frac);
    }
}
