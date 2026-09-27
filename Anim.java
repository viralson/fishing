package pl.powitanie;

import java.util.ArrayList;
import java.util.List;

/**
 * Tekst z kodami &: rozbija na litery z kolorem i stylem,
 * żeby dało się go wpisywać, kasować i przelewać falą kolorów.
 */
public final class Anim {

    /** Jedna litera z jej kolorem (np. "&e" albo "&#ffaa00") i stylami (np. "&l"). */
    public record Glyph(char c, String color, String style) {}

    private Anim() {}

    private static final java.util.regex.Pattern GRADIENT = java.util.regex.Pattern.compile(
            "\\{gradient((?::#[0-9a-fA-F]{6})+)}(.*?)\\{/gradient}");

    /**
     * Rozwija gradienty {gradient:#ff0000:#00ff00}tekst{/gradient} na kolory hex litera po literze
     * (dowolna liczba kolorów; kody stylu &l &o itd. w środku są zachowane).
     */
    public static String gradients(String s) {
        var m = GRADIENT.matcher(s);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String[] hex = m.group(1).substring(1).split(":");
            int[] stops = new int[hex.length];
            for (int i = 0; i < hex.length; i++) stops[i] = Integer.parseInt(hex[i].substring(1), 16);
            String inner = m.group(2);
            int visible = 0;
            for (int i = 0; i < inner.length(); i++) {
                if (inner.charAt(i) == '&' && i + 1 < inner.length() && "klmnor".indexOf(Character.toLowerCase(inner.charAt(i + 1))) >= 0) { i++; continue; }
                visible++;
            }
            StringBuilder g = new StringBuilder();
            String style = "";
            int n = 0;
            for (int i = 0; i < inner.length(); i++) {
                char ch = inner.charAt(i);
                if (ch == '&' && i + 1 < inner.length() && "klmnor".indexOf(Character.toLowerCase(inner.charAt(i + 1))) >= 0) {
                    char code = Character.toLowerCase(inner.charAt(i + 1));
                    style = code == 'r' ? "" : style + "&" + code;
                    i++;
                    continue;
                }
                double t = visible <= 1 ? 0 : (double) n / (visible - 1);
                g.append(String.format("&#%06x", lerp(stops, t))).append(style).append(ch);
                n++;
            }
            m.appendReplacement(out, java.util.regex.Matcher.quoteReplacement(g.toString()));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static int lerp(int[] stops, double t) {
        if (stops.length == 1) return stops[0];
        double pos = t * (stops.length - 1);
        int i = Math.min(stops.length - 2, (int) Math.floor(pos));
        double f = pos - i;
        int a = stops[i], b = stops[i + 1];
        int r = (int) Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * f);
        int gr = (int) Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * f);
        int bl = (int) Math.round((a & 255) + ((b & 255) - (a & 255)) * f);
        return (r << 16) | (gr << 8) | bl;
    }

    public static List<Glyph> parse(String s) {
        s = gradients(s);
        List<Glyph> out = new ArrayList<>();
        String color = "&f", style = "";
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '&' && i + 1 < s.length()) {
                char n = Character.toLowerCase(s.charAt(i + 1));
                if (n == '#' && i + 7 < s.length() && s.substring(i + 2, i + 8).matches("[0-9a-fA-F]{6}")) {
                    color = s.substring(i, i + 8);
                    style = "";
                    i += 7;
                    continue;
                }
                if ("0123456789abcdef".indexOf(n) >= 0) {
                    color = "&" + n;
                    style = "";
                    i++;
                    continue;
                }
                if ("klmno".indexOf(n) >= 0) {
                    style += "&" + n;
                    i++;
                    continue;
                }
                if (n == 'r') {
                    color = "&f";
                    style = "";
                    i++;
                    continue;
                }
            }
            out.add(new Glyph(ch, color, style));
        }
        return out;
    }

    /** Pierwsze {@code count} liter z oryginalnymi kolorami. */
    public static String prefix(List<Glyph> g, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(count, g.size()); i++) {
            Glyph x = g.get(i);
            sb.append(x.color()).append(x.style()).append(x.c());
        }
        return sb.toString();
    }

    /**
     * Fala kolorów: głowa na pozycji {@code head}.
     * Litery head i head-1 dostają kolor[0], head-2 i head-3 kolor[1] itd., reszta kolor bazowy.
     */
    public static String wave(List<Glyph> g, int head, List<String> colors, String base) {
        return wave(g, head, colors, base, false);
    }

    /** Jak wyżej; {@code reverse} = fala płynie od prawej do lewej (ogon po prawej). */
    public static String wave(List<Glyph> g, int head, List<String> colors, String base, boolean reverse) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < g.size(); i++) {
            Glyph x = g.get(i);
            int dist = reverse ? i - head : head - i;
            String col = base;
            if (dist >= 0 && dist / 2 < colors.size()) col = colors.get(dist / 2);
            sb.append(col).append(x.style()).append(x.c());
        }
        return sb.toString();
    }
}
