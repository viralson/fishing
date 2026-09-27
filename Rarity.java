package pl.lowienie;

import net.kyori.adventure.bossbar.BossBar;

/** Rzadkości ryb – kolor, waga losowania, XP i trudność holu. */
public enum Rarity {

    POSPOLITA("Pospolita", "#b8c4cc", 600, 6, BossBar.Color.WHITE),
    NIEPOSPOLITA("Niepospolita", "#7ee787", 250, 14, BossBar.Color.GREEN),
    RZADKA("Rzadka", "#58a6ff", 100, 35, BossBar.Color.BLUE),
    EPICKA("Epicka", "#c77dff", 35, 90, BossBar.Color.PURPLE),
    LEGENDARNA("Legendarna", "#ffc857", 12, 240, BossBar.Color.YELLOW),
    MITYCZNA("Mityczna", "#ff5e7e", 3, 700, BossBar.Color.RED);

    public final String label, hex;
    public final double weight;
    public final int xp;
    public final BossBar.Color bar;

    Rarity(String label, String hex, double weight, int xp, BossBar.Color bar) {
        this.label = label;
        this.hex = hex;
        this.weight = weight;
        this.xp = xp;
        this.bar = bar;
    }

    /** Kolorowy tag otwierający. */
    public String c() { return "<" + hex + ">"; }

    /** Nazwa w kolorze. */
    public String tag() { return c() + label + "</" + hex + ">"; }

    /** Ile trafień potrzeba do wyciągnięcia. */
    public int hits() { return 2 + ordinal(); }

    /** Punkty w zawodach. */
    public int points() { return switch (this) {
        case POSPOLITA -> 1; case NIEPOSPOLITA -> 3; case RZADKA -> 8;
        case EPICKA -> 20; case LEGENDARNA -> 60; case MITYCZNA -> 200; };
    }

    public static Rarity byName(String s) {
        if (s == null) return null;
        for (Rarity r : values()) if (r.name().equalsIgnoreCase(s) || r.label.equalsIgnoreCase(s)) return r;
        return null;
    }
}
