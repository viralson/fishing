package pl.lowienie;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Szał ryb – krótkie wydarzenie: szybsze branie i więcej szczęścia. */
public final class Frenzy {

    private final LowieniePlugin pl;
    private long until, total;
    private final BossBar bar = BossBar.bossBar(Msg.mm(""), 1f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);

    public Frenzy(LowieniePlugin pl) { this.pl = pl; }

    public boolean active() { return System.currentTimeMillis() < until; }

    public long left() { return Math.max(0, until - System.currentTimeMillis()); }

    public void start(int minutes) {
        total = minutes * 60_000L;
        until = System.currentTimeMillis() + total;
        Msg.broadcast("<a><b>SZAŁ RYB!</b></a> <t>Przez <a>" + minutes + " min</a> ryby biorą <a>2× szybciej</a> i częściej trafiają się rzadkie!");
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 0.6f, 1.4f);
            p.showBossBar(bar);
        }
        tick();
    }

    public void stop() {
        if (until == 0) return;
        until = 0;
        for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(bar);
        Msg.broadcast("<m>Szał ryb dobiegł końca.");
    }

    /** Co sekundę. */
    public void tick() {
        if (until == 0) return;
        if (!active()) { stop(); return; }
        bar.name(Msg.mm("<a><b>Szał ryb</b></a> <dark>·</dark> <t>2× szybsze branie, +3 szczęścia <dark>·</dark> <a>" + Msg.left(left())));
        bar.progress(Math.max(0f, Math.min(1f, (float) left() / total)));
    }

    public void show(Player p) { if (active()) p.showBossBar(bar); }

    /** Losowy start (co minutę). */
    public void maybeAuto() {
        double chance = pl.getConfig().getDouble("szal.szansa-na-minute", 0.004);
        if (active() || chance <= 0 || Bukkit.getOnlinePlayers().isEmpty()) return;
        if (Math.random() < chance) start(pl.getConfig().getInt("szal.minut", 5));
    }
}
