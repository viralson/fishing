package pl.lowienie;

import java.util.Locale;

/** Konkretna złowiona ryba (w siatce, akwarium lub jako przedmiot). */
public record FishEntry(String species, double cm, double kg, int stars, String catcher, long time) {

    public String serialize() {
        return String.format(Locale.ROOT, "%s;%.1f;%.3f;%d;%s;%d", species, cm, kg, stars, catcher.replace(";", ""), time);
    }

    public static FishEntry parse(String s) {
        try {
            String[] a = s.split(";");
            return new FishEntry(a[0], Double.parseDouble(a[1]), Double.parseDouble(a[2]),
                    Integer.parseInt(a[3]), a[4], Long.parseLong(a[5]));
        } catch (Exception e) {
            return null;
        }
    }

    /** Gwiazdki jako tekst. */
    public String starsTag() { return starsTag(stars); }

    public static String starsTag(int stars) {
        return "<#ffd84d>" + "★".repeat(stars) + "</#ffd84d><dark>" + "★".repeat(5 - stars) + "</dark>";
    }
}
