package pl.lowienie;

/** Klasy wędek – bazowe statystyki i limit ulepszeń. */
public enum RodTier {

    ZWYKLA("Zwykła wędka", "#b8c4cc", 0, 0, 0, 0, 2, 3),
    BAMBUSOWA("Bambusowa wędka", "#9fd36b", 5_000, 1, 1, 1, 3, 5),
    STALOWA("Stalowa wędka", "#9fb4c7", 25_000, 2, 2, 2, 3, 7),
    KARBONOWA("Karbonowa wędka", "#58a6ff", 100_000, 3, 3, 3, 4, 9),
    TYTANOWA("Tytanowa wędka", "#c77dff", 400_000, 4, 4, 4, 4, 11),
    MITYCZNA("Mityczna wędka", "#ff5e7e", 1_500_000, 6, 6, 6, 5, 13);

    public final String name, hex;
    public final double price;
    public final int luck, speed, control, line, maxLevel;

    RodTier(String name, String hex, double price, int luck, int speed, int control, int line, int maxLevel) {
        this.name = name;
        this.hex = hex;
        this.price = price;
        this.luck = luck;
        this.speed = speed;
        this.control = control;
        this.line = line;
        this.maxLevel = maxLevel;
    }

    public String display() { return "<" + hex + ">" + name + "</" + hex + ">"; }

    public static RodTier byName(String s) {
        if (s == null) return null;
        for (RodTier t : values()) if (t.name().equalsIgnoreCase(s)) return t;
        return null;
    }
}
