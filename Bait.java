package pl.lowienie;

import org.bukkit.Material;

/** Przynęty – zużywane przy każdym braniu. */
public enum Bait {

    ROBAK("Robak", Material.PINK_DYE, 60, 16, 0.15, 0, 0, 0, 0, "Ryby biorą <a>15%</a> szybciej"),
    KULKA("Kulka zanętowa", Material.SLIME_BALL, 150, 16, 0.30, 0, 0, 0, 0, "Ryby biorą <a>30%</a> szybciej"),
    BLYSTKA("Błystka", Material.IRON_NUGGET, 300, 16, 0.10, 2, 0, 0, 0, "<a>+2</a> szczęścia, <a>10%</a> szybciej"),
    SWIETLIK("Świecąca przynęta", Material.GLOW_INK_SAC, 500, 16, 0, 2, 0, 0, 0, "<a>+2</a> szczęścia, w nocy i jaskiniach <a>+5</a>"),
    KRWISTA("Krwista przynęta", Material.RED_DYE, 600, 8, 0.10, 1, 0, 3.0, 0, "Szansa na <err>potwora</err> <a>×3</a>"),
    MAGICZNA("Magiczna przynęta", Material.AMETHYST_SHARD, 1500, 8, 0.20, 5, 0.03, 0, 0, "<a>+5</a> szczęścia, <a>+3%</a> na skarb"),
    ZLOTA("Złota przynęta", Material.GOLD_NUGGET, 4000, 4, 0.25, 4, 0.02, 0, 0.15, "<a>+4</a> szczęścia, <a>+15%</a> na podwójny połów");

    public final String name;
    public final Material mat;
    public final double price;      // cena za paczkę
    public final int pack;          // sztuk w paczce
    public final double speed;      // skrócenie czasu brania
    public final int luck;
    public final double treasure;   // dodatkowa szansa na skarb
    public final double creatureMult;
    public final double doubleCatch;
    public final String desc;

    Bait(String name, Material mat, double price, int pack, double speed, int luck, double treasure,
         double creatureMult, double doubleCatch, String desc) {
        this.name = name;
        this.mat = mat;
        this.price = price;
        this.pack = pack;
        this.speed = speed;
        this.luck = luck;
        this.treasure = treasure;
        this.creatureMult = creatureMult;
        this.doubleCatch = doubleCatch;
        this.desc = desc;
    }

    public static Bait byName(String s) {
        if (s == null) return null;
        for (Bait b : values()) if (b.name().equalsIgnoreCase(s)) return b;
        return null;
    }
}
