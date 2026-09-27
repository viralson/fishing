package pl.lowienie;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Zawody wędkarskie z rankingiem na żywo. */
public final class Tournament {

    public enum Mode {
        ILOSC("Najwięcej ryb", "ryb"),
        WAGA("Najcięższa ryba", "kg"),
        PUNKTY("Punkty za rzadkość", "pkt");

        public final String label, unit;
        Mode(String label, String unit) { this.label = label; this.unit = unit; }
    }

    private final LowieniePlugin pl;
    private Mode mode;
    private long until, total;
    private final Map<UUID, Double> scores = new HashMap<>();
    private final Map<UUID, String> names = new HashMap<>();
    private final BossBar bar = BossBar.bossBar(Msg.mm(""), 1f, BossBar.Color.YELLOW, BossBar.Overlay.NOTCHED_20);
    private long nextAuto;

    public Tournament(LowieniePlugin pl) {
        this.pl = pl;
        scheduleNext();
    }

    private void scheduleNext() {
        long every = pl.getConfig().getLong("zawody.co-ile-minut", 90);
        nextAuto = every <= 0 ? Long.MAX_VALUE : System.currentTimeMillis() + every * 60_000L;
    }

    public boolean active() { return mode != null; }

    public Mode mode() { return mode; }

    public long left() { return Math.max(0, until - System.currentTimeMillis()); }

    public long untilNext() { return nextAuto == Long.MAX_VALUE ? -1 : Math.max(0, nextAuto - System.currentTimeMillis()); }

    public void start(Mode m, int minutes) {
        if (active()) return;
        mode = m;
        total = minutes * 60_000L;
        until = System.currentTimeMillis() + total;
        scores.clear();
        names.clear();
        Msg.broadcast("<#ffd84d><b>ZAWODY WĘDKARSKIE!</b></#ffd84d> <t>Tryb: <a>" + m.label + "</a> <dark>·</dark> <t>czas: <a>" + minutes + " min</a> "
                + Msg.button("Ranking", "/ryby zawody", "Pokaż wyniki na żywo"));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 0.7f, 1.1f);
            p.showBossBar(bar);
        }
        tick();
    }

    public void stop(boolean reward) {
        if (!active()) return;
        List<Map.Entry<UUID, Double>> top = standings();
        for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(bar);
        Msg.broadcast("<#ffd84d><b>Koniec zawodów!</b></#ffd84d> <m>(" + mode.label + ")");
        if (top.isEmpty()) Msg.broadcast("<m>Nikt niczego nie złowił…");
        List<Double> prizes = pl.getConfig().getDoubleList("zawody.nagrody");
        if (prizes.isEmpty()) prizes = List.of(15000.0, 7500.0, 3500.0);
        String[] medal = {"<#ffd84d>①", "<#c0c0c0>②", "<#cd7f32>③"};
        for (int i = 0; i < Math.min(3, top.size()); i++) {
            var e = top.get(i);
            double prize = i < prizes.size() ? prizes.get(i) : 0;
            Msg.broadcast(medal[i] + " <t>" + Msg.esc(names.get(e.getKey())) + " <dark>·</dark> <a>" + value(e.getValue())
                    + (reward && prize > 0 ? " <dark>·</dark> <money>+" + pl.eco().format(prize) : ""));
            if (reward && prize > 0) {
                pl.eco().deposit(Bukkit.getOfflinePlayer(e.getKey()), prize);
                Player p = Bukkit.getPlayer(e.getKey());
                if (p != null) {
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
                    if (i == 0) Quests.giveOrDrop(p, pl.items().bait(Bait.ZLOTA, 4));
                }
            }
        }
        mode = null;
        scheduleNext();
    }

    /** Co sekundę. */
    public void tick() {
        if (!active()) {
            if (System.currentTimeMillis() >= nextAuto) {
                if (Bukkit.getOnlinePlayers().size() >= pl.getConfig().getInt("zawody.min-graczy", 2)) {
                    Mode[] ms = Mode.values();
                    start(ms[(int) (Math.random() * ms.length)], pl.getConfig().getInt("zawody.minut", 10));
                } else scheduleNext();
            }
            return;
        }
        if (left() <= 0) { stop(true); return; }
        var top = standings();
        String lead = top.isEmpty() ? "<m>brak wyników" : "<t>Prowadzi <a>" + Msg.esc(names.get(top.get(0).getKey())) + "</a> <m>(" + value(top.get(0).getValue()) + ")";
        bar.name(Msg.mm("<#ffd84d><b>Zawody</b></#ffd84d> <dark>·</dark> <t>" + mode.label + " <dark>·</dark> " + lead + " <dark>·</dark> <a>" + Msg.left(left())));
        bar.progress(Math.max(0f, Math.min(1f, (float) left() / total)));
    }

    public void show(Player p) { if (active()) p.showBossBar(bar); }

    public void onCatch(Player p, FishEntry e, Species s) {
        if (!active()) return;
        names.put(p.getUniqueId(), p.getName());
        switch (mode) {
            case ILOSC -> scores.merge(p.getUniqueId(), 1.0, Double::sum);
            case WAGA -> scores.merge(p.getUniqueId(), e.kg(), Math::max);
            case PUNKTY -> scores.merge(p.getUniqueId(), (double) s.rarity().points() * e.stars(), Double::sum);
        }
    }

    public List<Map.Entry<UUID, Double>> standings() {
        List<Map.Entry<UUID, Double>> l = new ArrayList<>(scores.entrySet());
        l.sort(Map.Entry.<UUID, Double>comparingByValue().reversed());
        return l;
    }

    public String name(UUID u) { return names.getOrDefault(u, "?"); }

    public String value(double v) {
        return mode == Mode.WAGA ? FishItems.kg(v) : Msg.fmt(Math.round(v)) + " " + mode.unit;
    }
}
