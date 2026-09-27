package pl.lowienie;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Wszystko, co wiemy o wędkarzu. */
public final class PlayerData {

    public static final class SpStat {
        public int count;
        public double bestKg, bestCm;
        public int bestStars;
    }

    public final UUID id;
    public String name;

    public int level = 1;
    public long xp;
    public final Map<Perk, Integer> perks = new EnumMap<>(Perk.class);

    // statystyki
    public long caught, escaped, perfect, junk, treasures, creatures;
    public double earned, totalKg;
    public int streak, bestStreak;
    public String heaviestSp;
    public double heaviestKg;
    public final Map<String, SpStat> species = new HashMap<>();

    // siatka i akwarium
    public final List<FishEntry> bag = new ArrayList<>();
    public int bagLevel;
    public final List<FishEntry> aquarium = new ArrayList<>();
    public int aqLevel;
    public long aqLast = System.currentTimeMillis();

    // zadania dnia
    public String qDay = "";
    public final int[] qProg = new int[3];
    public final boolean[] qClaimed = new boolean[3];
    public boolean qBonus;

    // ustawienia
    public boolean toBag = true, sounds = true, dropJunk = false, hints = true;
    public Bait selected;

    public final Set<Integer> atlasRewards = new HashSet<>();

    public boolean dirty;

    public PlayerData(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public int perk(Perk p) { return perks.getOrDefault(p, 0); }

    public int spentPoints() {
        int s = 0;
        for (int v : perks.values()) s += v;
        return s;
    }

    public int freePoints() { return Math.max(0, (level - 1) - spentPoints()); }

    public static final int BAG_MAX_LEVEL = 4, AQ_MAX_LEVEL = 2;

    public int bagCap() { return 36 + bagLevel * 18; }

    public int aqCap() { return 9 + aqLevel * 9; }

    public int discovered() { return species.size(); }

    public SpStat sp(String id) { return species.computeIfAbsent(id, k -> new SpStat()); }

    public void dirty() { dirty = true; }
}
