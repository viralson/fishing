package pl.powitanie;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Animacja powitalna jednego gracza – każda klatka to jeden tick. */
public final class Intro extends BukkitRunnable {

    /** Jedna klatka: tytuł (gotowy tekst z &) i szablon podpisu (placeholdery podmieniane na żywo). */
    private record Frame(String title, String subtitle, Sound sound, float pitch) {}

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&').hexColors().build();

    private final PowitaniePlugin pl;
    private final Player p;
    private final List<Frame> frames = new ArrayList<>();
    private final int fadeOut;
    private int i;

    public Intro(PowitaniePlugin pl, Player p) {
        this.pl = pl;
        this.p = p;
        this.fadeOut = pl.getConfig().getInt("zanikanie-na-koncu", 20);
        build();
    }

    // ───────────── budowanie klatek ─────────────

    private void build() {
        var c = pl.getConfig();
        int typeSpeed = Math.max(1, c.getInt("tempo.pisanie", 2));
        int eraseSpeed = Math.max(1, c.getInt("tempo.kasowanie", 1));
        int waveSpeed = Math.max(1, c.getInt("tempo.fala", 1));
        boolean sounds = c.getBoolean("dzwieki", true);
        List<String> waveColors = c.getStringList("fala.kolory");
        if (waveColors.isEmpty()) waveColors = List.of("&e", "&6");
        String waveBase = c.getString("fala.bazowy", "&f");

        for (Map<?, ?> raw : c.getMapList("kroki")) {
            String title = str(raw, "title", "");
            String sub = str(raw, "subtitle", "");
            boolean type = bool(raw, "pisanie", true);
            boolean erase = bool(raw, "kasowanie", true);
            boolean wave = bool(raw, "fala", false);
            int waves = num(raw, "fala-powtorzen", 1);
            int hold = num(raw, "trzymaj", 30);
            List<Anim.Glyph> g = Anim.parse(title);

            // wpisywanie litera po literze
            if (type) {
                for (int n = 1; n <= g.size(); n++) {
                    boolean visible = !Character.isWhitespace(g.get(n - 1).c());
                    for (int t = 0; t < typeSpeed; t++)
                        add(Anim.prefix(g, n), sub, sounds && t == 0 && visible ? Sound.BLOCK_NOTE_BLOCK_HAT : null, 1.6f + n * 0.03f);
                }
            }
            // fala kolorów: od lewej do prawej, potem od prawej do lewej
            if (wave) {
                int tail = waveColors.size() * 2;
                boolean pingPong = c.getBoolean("fala.tam-i-z-powrotem", true);
                boolean waveSound = sounds && c.getBoolean("fala.dzwiek", true);
                int step = 0;
                for (int w = 0; w < waves; w++) {
                    for (int dir = 0; dir < (pingPong ? 2 : 1); dir++) {
                        boolean rev = dir == 1;
                        int len = g.size() + tail;
                        for (int k = 0; k < len; k++) {
                            int head = rev ? g.size() - 1 - k : k;
                            // dźwięk co drugi krok, ton rośnie w prawo i opada w lewo
                            float pos = Math.max(0, Math.min(1, (float) (rev ? g.size() - 1 - k : k) / Math.max(1, g.size() - 1)));
                            Sound snd = waveSound && step++ % 2 == 0 && k < g.size() ? Sound.BLOCK_NOTE_BLOCK_CHIME : null;
                            for (int t = 0; t < waveSpeed; t++)
                                add(Anim.wave(g, head, waveColors, waveBase, rev), sub, t == 0 ? snd : null, 0.9f + pos * 0.9f);
                        }
                    }
                }
            }
            // przytrzymanie
            String full = wave ? Anim.wave(g, -1, waveColors, waveBase) : Anim.prefix(g, g.size());
            for (int t = 0; t < hold; t++) add(full, sub, null, 1f);
            // kasowanie od końca
            if (erase) {
                for (int n = g.size() - 1; n >= 0; n--)
                    for (int t = 0; t < eraseSpeed; t++) add(Anim.prefix(g, n), sub, null, 1f);
            }
        }
    }

    private void add(String title, String sub, Sound s, float pitch) { frames.add(new Frame(title, sub, s, pitch)); }

    private static String str(Map<?, ?> m, String k, String def) {
        Object o = m.get(k);
        return o == null ? def : String.valueOf(o);
    }

    private static boolean bool(Map<?, ?> m, String k, boolean def) {
        Object o = m.get(k);
        return o instanceof Boolean b ? b : o == null ? def : Boolean.parseBoolean(String.valueOf(o));
    }

    private static int num(Map<?, ?> m, String k, int def) {
        Object o = m.get(k);
        if (o instanceof Number n) return n.intValue();
        try { return o == null ? def : Integer.parseInt(String.valueOf(o)); } catch (NumberFormatException e) { return def; }
    }

    // ───────────── odtwarzanie ─────────────

    public void start() {
        runTaskTimer(pl, 0L, 1L);
    }

    @Override
    public void run() {
        if (!p.isOnline() || i >= frames.size()) {
            finish();
            return;
        }
        Frame f = frames.get(i);
        boolean last = i == frames.size() - 1;
        Title.Times times = Title.Times.times(Duration.ZERO,
                Duration.ofMillis(last ? 1000 : 2000), Duration.ofMillis(last ? fadeOut * 50L : 0));
        p.showTitle(Title.title(text(f.title()), text(Anim.gradients(placeholders(f.subtitle()))), times));
        if (f.sound() != null) p.playSound(p.getLocation(), f.sound(), 0.35f, f.pitch());
        i++;
    }

    private void finish() {
        cancel();
        pl.done(p);
    }

    /** Przerwanie (np. gracz kucnął, żeby pominąć). */
    public void skip() {
        if (isCancelled()) return;
        cancel();
        p.clearTitle();
        pl.done(p);
    }

    private static Component text(String s) { return LEGACY.deserialize(s); }

    private String placeholders(String s) {
        var c = pl.getConfig();
        ZoneId zone;
        try { zone = ZoneId.of(c.getString("strefa-czasowa", "Europe/Warsaw")); } catch (Exception e) { zone = ZoneId.systemDefault(); }
        ZonedDateTime now = ZonedDateTime.now(zone);
        if (s.contains("{wakacje}")) s = s.replace("{wakacje}", vacation(now.toLocalDate()));
        return s.replace("{player}", p.getName())
                .replace("{greeting}", greeting(now.getHour()))
                .replace("{time}", now.format(DateTimeFormatter.ofPattern(c.getString("format-godziny", "HH:mm:ss"))))
                .replace("{date}", now.format(DateTimeFormatter.ofPattern(c.getString("format-daty", "dd.MM.yyyy"))))
                .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("{max}", String.valueOf(Bukkit.getMaxPlayers()));
    }

    // ───────────── wakacje ─────────────

    /** "Wakacje za: X dni" albo "Wakacje trwają!". */
    private String vacation(java.time.LocalDate today) {
        var c = pl.getConfig();
        int year = today.getYear();
        java.time.LocalDate start = vacStart(year), end = vacEnd(year);
        if (!today.isBefore(start) && !today.isAfter(end)) return c.getString("wakacje.trwaja", "Wakacje trwają!");
        if (today.isAfter(end)) start = vacStart(year + 1);
        long days = java.time.temporal.ChronoUnit.DAYS.between(today, start);
        String word = days == 1 ? c.getString("wakacje.dzien", "dzień") : c.getString("wakacje.dni", "dni");
        return c.getString("wakacje.przed", "Wakacje za: {dni}").replace("{dni}", days + " " + word);
    }

    /** Początek: data z configu (MM-dd) albo "auto" = sobota po pierwszym piątku po 20 czerwca. */
    private java.time.LocalDate vacStart(int year) {
        String v = pl.getConfig().getString("wakacje.poczatek", "auto");
        java.time.LocalDate d = parseMd(v, year);
        if (d != null) return d;
        java.time.LocalDate x = java.time.LocalDate.of(year, 6, 21);
        while (x.getDayOfWeek() != java.time.DayOfWeek.FRIDAY) x = x.plusDays(1);
        return x.plusDays(1);
    }

    private java.time.LocalDate vacEnd(int year) {
        java.time.LocalDate d = parseMd(pl.getConfig().getString("wakacje.koniec", "08-31"), year);
        return d == null ? java.time.LocalDate.of(year, 8, 31) : d;
    }

    private static java.time.LocalDate parseMd(String v, int year) {
        if (v == null) return null;
        String[] a = v.trim().split("-");
        if (a.length != 2) return null;
        try { return java.time.LocalDate.of(year, Integer.parseInt(a[0]), Integer.parseInt(a[1])); } catch (Exception e) { return null; }
    }

    /** Powitanie zależne od godziny, np. "5-18: Dzień dobry". */
    private String greeting(int hour) {
        ConfigurationSection sec = pl.getConfig().getConfigurationSection("powitania");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                String[] a = key.split("-");
                if (a.length != 2) continue;
                try {
                    int from = Integer.parseInt(a[0].trim()), to = Integer.parseInt(a[1].trim());
                    boolean in = from <= to ? hour >= from && hour < to : hour >= from || hour < to;
                    if (in) return sec.getString(key, "Witaj");
                } catch (NumberFormatException ignored) {}
            }
        }
        return hour >= 5 && hour < 18 ? "Dzień dobry" : "Dobry wieczór";
    }
}
