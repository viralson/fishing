package pl.lowienie;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToDoubleFunction;

/** Zapis graczy (gracze/uuid.yml) i danych globalnych (global.yml). */
public final class Store {

    /** Rekord serwera dla gatunku. */
    public record Record(String holder, double kg, double cm) {}

    /** Wpis rankingu – żeby pokazywać też graczy offline. */
    public static final class Rank {
        public String name;
        public int level;
        public long caught;
        public double earned, heaviest;
    }

    private final LowieniePlugin pl;
    private final File dir, globalFile;
    private final Map<UUID, PlayerData> online = new ConcurrentHashMap<>();
    public final Map<String, Record> records = new HashMap<>();
    public final Map<String, Double> supply = new HashMap<>();
    public final Map<UUID, Rank> ranks = new HashMap<>();
    private boolean globalDirty;

    public Store(LowieniePlugin pl) {
        this.pl = pl;
        this.dir = new File(pl.getDataFolder(), "gracze");
        this.globalFile = new File(pl.getDataFolder(), "global.yml");
        dir.mkdirs();
    }

    public void dirty() { globalDirty = true; }

    // ───────────── gracze ─────────────

    public PlayerData get(Player p) {
        return online.computeIfAbsent(p.getUniqueId(), u -> load(u, p.getName()));
    }

    public PlayerData peek(UUID u) { return online.get(u); }

    public java.util.Collection<PlayerData> loaded() { return online.values(); }

    private PlayerData load(UUID u, String name) {
        PlayerData d = new PlayerData(u, name);
        File f = new File(dir, u + ".yml");
        if (!f.exists()) return d;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        d.level = Math.max(1, y.getInt("poziom", 1));
        d.xp = y.getLong("xp");
        ConfigurationSection ps = y.getConfigurationSection("umiejetnosci");
        if (ps != null) for (String k : ps.getKeys(false)) {
            try { d.perks.put(Perk.valueOf(k), ps.getInt(k)); } catch (IllegalArgumentException ignored) {}
        }
        d.caught = y.getLong("staty.zlowione");
        d.escaped = y.getLong("staty.ucieczki");
        d.perfect = y.getLong("staty.perfekcyjne");
        d.junk = y.getLong("staty.smieci");
        d.treasures = y.getLong("staty.skarby");
        d.creatures = y.getLong("staty.potwory");
        d.earned = y.getDouble("staty.zarobione");
        d.totalKg = y.getDouble("staty.kg");
        d.streak = y.getInt("staty.seria");
        d.bestStreak = y.getInt("staty.najlepsza-seria");
        d.heaviestSp = y.getString("staty.najciezsza.gatunek");
        d.heaviestKg = y.getDouble("staty.najciezsza.kg");
        ConfigurationSection ss = y.getConfigurationSection("gatunki");
        if (ss != null) for (String k : ss.getKeys(false)) {
            PlayerData.SpStat s = d.sp(k);
            s.count = ss.getInt(k + ".ilosc");
            s.bestKg = ss.getDouble(k + ".kg");
            s.bestCm = ss.getDouble(k + ".cm");
            s.bestStars = ss.getInt(k + ".gwiazdki");
        }
        for (String s : y.getStringList("siatka")) { FishEntry e = FishEntry.parse(s); if (e != null) d.bag.add(e); }
        d.bagLevel = y.getInt("siatka-poziom");
        for (String s : y.getStringList("akwarium")) { FishEntry e = FishEntry.parse(s); if (e != null) d.aquarium.add(e); }
        d.aqLevel = y.getInt("akwarium-poziom");
        d.aqLast = y.getLong("akwarium-odbior", System.currentTimeMillis());
        d.qDay = y.getString("zadania.dzien", "");
        List<Integer> prog = y.getIntegerList("zadania.postep");
        for (int i = 0; i < 3 && i < prog.size(); i++) d.qProg[i] = prog.get(i);
        List<Boolean> cl = y.getBooleanList("zadania.odebrane");
        for (int i = 0; i < 3 && i < cl.size(); i++) d.qClaimed[i] = cl.get(i);
        d.qBonus = y.getBoolean("zadania.bonus");
        d.toBag = y.getBoolean("ustawienia.do-siatki", true);
        d.sounds = y.getBoolean("ustawienia.dzwieki", true);
        d.dropJunk = y.getBoolean("ustawienia.wyrzucaj-smieci", false);
        d.hints = y.getBoolean("ustawienia.podpowiedzi", true);
        d.selected = Bait.byName(y.getString("ustawienia.przyneta"));
        d.atlasRewards.addAll(y.getIntegerList("atlas-nagrody"));
        return d;
    }

    public void save(PlayerData d) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("nick", d.name);
        y.set("poziom", d.level);
        y.set("xp", d.xp);
        for (var e : d.perks.entrySet()) y.set("umiejetnosci." + e.getKey().name(), e.getValue());
        y.set("staty.zlowione", d.caught);
        y.set("staty.ucieczki", d.escaped);
        y.set("staty.perfekcyjne", d.perfect);
        y.set("staty.smieci", d.junk);
        y.set("staty.skarby", d.treasures);
        y.set("staty.potwory", d.creatures);
        y.set("staty.zarobione", d.earned);
        y.set("staty.kg", d.totalKg);
        y.set("staty.seria", d.streak);
        y.set("staty.najlepsza-seria", d.bestStreak);
        y.set("staty.najciezsza.gatunek", d.heaviestSp);
        y.set("staty.najciezsza.kg", d.heaviestKg);
        for (var e : d.species.entrySet()) {
            String k = "gatunki." + e.getKey();
            y.set(k + ".ilosc", e.getValue().count);
            y.set(k + ".kg", e.getValue().bestKg);
            y.set(k + ".cm", e.getValue().bestCm);
            y.set(k + ".gwiazdki", e.getValue().bestStars);
        }
        y.set("siatka", d.bag.stream().map(FishEntry::serialize).toList());
        y.set("siatka-poziom", d.bagLevel);
        y.set("akwarium", d.aquarium.stream().map(FishEntry::serialize).toList());
        y.set("akwarium-poziom", d.aqLevel);
        y.set("akwarium-odbior", d.aqLast);
        y.set("zadania.dzien", d.qDay);
        y.set("zadania.postep", List.of(d.qProg[0], d.qProg[1], d.qProg[2]));
        y.set("zadania.odebrane", List.of(d.qClaimed[0], d.qClaimed[1], d.qClaimed[2]));
        y.set("zadania.bonus", d.qBonus);
        y.set("ustawienia.do-siatki", d.toBag);
        y.set("ustawienia.dzwieki", d.sounds);
        y.set("ustawienia.wyrzucaj-smieci", d.dropJunk);
        y.set("ustawienia.podpowiedzi", d.hints);
        y.set("ustawienia.przyneta", d.selected == null ? null : d.selected.name());
        y.set("atlas-nagrody", new ArrayList<>(d.atlasRewards));
        try {
            y.save(new File(dir, d.id + ".yml"));
            d.dirty = false;
        } catch (IOException ex) {
            pl.getLogger().warning("Nie udało się zapisać gracza " + d.name + ": " + ex.getMessage());
        }
        updateRank(d);
    }

    public void updateRank(PlayerData d) {
        Rank r = ranks.computeIfAbsent(d.id, u -> new Rank());
        r.name = d.name;
        r.level = d.level;
        r.caught = d.caught;
        r.earned = d.earned;
        r.heaviest = d.heaviestKg;
        globalDirty = true;
    }

    public void unload(UUID u) {
        PlayerData d = online.remove(u);
        if (d != null) save(d);
    }

    /** Top 10 wg podanej wartości (odświeża online). */
    public List<Map.Entry<UUID, Rank>> top(ToDoubleFunction<Rank> by) {
        for (PlayerData d : online.values()) updateRank(d);
        List<Map.Entry<UUID, Rank>> l = new ArrayList<>(ranks.entrySet());
        l.sort(Comparator.comparingDouble((Map.Entry<UUID, Rank> e) -> by.applyAsDouble(e.getValue())).reversed());
        return l.subList(0, Math.min(10, l.size()));
    }

    // ───────────── globalne ─────────────

    public void loadGlobal() {
        if (!globalFile.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(globalFile);
        ConfigurationSection rs = y.getConfigurationSection("rekordy");
        if (rs != null) for (String k : rs.getKeys(false))
            records.put(k, new Record(rs.getString(k + ".gracz", "?"), rs.getDouble(k + ".kg"), rs.getDouble(k + ".cm")));
        ConfigurationSection sp = y.getConfigurationSection("rynek");
        if (sp != null) for (String k : sp.getKeys(false)) supply.put(k, sp.getDouble(k));
        ConfigurationSection rk = y.getConfigurationSection("ranking");
        if (rk != null) for (String k : rk.getKeys(false)) {
            try {
                Rank r = new Rank();
                r.name = rk.getString(k + ".nick", "?");
                r.level = rk.getInt(k + ".poziom");
                r.caught = rk.getLong(k + ".zlowione");
                r.earned = rk.getDouble(k + ".zarobione");
                r.heaviest = rk.getDouble(k + ".najciezsza");
                ranks.put(UUID.fromString(k), r);
            } catch (IllegalArgumentException ignored) {}
        }
        if (pl.internalEco() != null) {
            ConfigurationSection w = y.getConfigurationSection("portfel");
            if (w != null) for (String k : w.getKeys(false)) {
                try { pl.internalEco().balances.put(UUID.fromString(k), w.getDouble(k)); } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void saveGlobal() {
        YamlConfiguration y = new YamlConfiguration();
        for (var e : records.entrySet()) {
            y.set("rekordy." + e.getKey() + ".gracz", e.getValue().holder());
            y.set("rekordy." + e.getKey() + ".kg", e.getValue().kg());
            y.set("rekordy." + e.getKey() + ".cm", e.getValue().cm());
        }
        for (var e : supply.entrySet()) if (e.getValue() > 0.01) y.set("rynek." + e.getKey(), e.getValue());
        for (var e : ranks.entrySet()) {
            String k = "ranking." + e.getKey();
            y.set(k + ".nick", e.getValue().name);
            y.set(k + ".poziom", e.getValue().level);
            y.set(k + ".zlowione", e.getValue().caught);
            y.set(k + ".zarobione", e.getValue().earned);
            y.set(k + ".najciezsza", e.getValue().heaviest);
        }
        if (pl.internalEco() != null)
            for (var e : pl.internalEco().balances.entrySet()) y.set("portfel." + e.getKey(), e.getValue());
        try {
            y.save(globalFile);
            globalDirty = false;
        } catch (IOException ex) {
            pl.getLogger().warning("Nie udało się zapisać global.yml: " + ex.getMessage());
        }
    }

    /** Zapis okresowy. */
    public void autosave() {
        for (PlayerData d : online.values()) if (d.dirty) save(d);
        if (globalDirty) saveGlobal();
    }

    public void saveAll() {
        for (PlayerData d : online.values()) save(d);
        saveGlobal();
    }

    public void loadOnline() {
        for (Player p : Bukkit.getOnlinePlayers()) get(p);
    }
}
