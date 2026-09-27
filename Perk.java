package pl.lowienie;

import org.bukkit.Material;

/** Umiejętności wędkarza – 1 punkt za każdy poziom. */
public enum Perk {

    SZCZESCIARZ("Szczęściarz", Material.RABBIT_FOOT, "<a>+1</a> szczęścia za poziom"),
    PODWOJNY("Podwójny połów", Material.BUNDLE, "<a>+3%</a> szansy na drugą rybę"),
    SKARBNIK("Poszukiwacz skarbów", Material.CHEST, "<a>+1.5%</a> szansy na skarb"),
    HANDLARZ("Handlarz", Material.EMERALD, "<a>+5%</a> do cen sprzedaży"),
    CIERPLIWOSC("Cierpliwość", Material.CLOCK, "Hol jest <a>7%</a> wolniejszy"),
    LOWCA("Łowca bestii", Material.TRIDENT, "<a>+25%</a> nagród z potworów"),
    OSZCZEDNY("Oszczędny", Material.PINK_DYE, "<a>12%</a> szansy, że przynęta się nie zużyje");

    public static final int MAX = 5;

    public final String name;
    public final Material icon;
    public final String desc;

    Perk(String name, Material icon, String desc) {
        this.name = name;
        this.icon = icon;
        this.desc = desc;
    }
}
