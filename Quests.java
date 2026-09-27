package pl.lowienie;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Zadania dnia – 3 losowe na gracza, odnawiane o północy. */
public final class Quests {

    public enum Type { CATCH, RARITY, SPECIES, SELL, PERFECT, TREASURE, WEIGHT, CREATURE, STREAK }

    public record Quest(Type type, int target, String arg, double reward, int xp, String text) {}

    private final LowieniePlugin pl;

    public Quests(LowieniePlugin pl) { this.pl = pl; }

    public static String today() { return LocalDate.now().toString(); }

    public void ensureDay(PlayerData d) {
        String t = today();
        if (t.equals(d.qDay)) return;
        d.qDay = t;
        for (int i = 0; i < 3; i++) { d.qProg[i] = 0; d.qClaimed[i] = false; }
        d.qBonus = false;
        d.dirty();
    }

    public List<Quest> list(PlayerData d) {
        ensureDay(d);
        Random r = new Random(LocalDate.now().toEpochDay() * 31 + d.id.hashCode());
        List<Type> types = new ArrayList<>(List.of(Type.values()));
        if (!pl.getConfig().getBoolean("potwory.wlaczone", true)) types.remove(Type.CREATURE);
        List<Quest> out = new ArrayList<>();
        double mult = pl.getConfig().getDouble("zadania.mnoznik-nagrod", 1.0);
        for (int i = 0; i < 3; i++) {
            Type t = types.remove(r.nextInt(types.size()));
            out.add(make(t, r, mult));
        }
        return out;
    }

    private Quest make(Type t, Random r, double mult) {
        return switch (t) {
            case CATCH -> { int n = 10 + r.nextInt(5) * 5; yield new Quest(t, n, null, n * 60 * mult, n * 4, "Złów <a>" + n + "</a> ryb"); }
            case RARITY -> {
                Rarity ra = r.nextBoolean() ? Rarity.RZADKA : Rarity.NIEPOSPOLITA;
                int n = ra == Rarity.RZADKA ? 2 + r.nextInt(3) : 5 + r.nextInt(6);
                yield new Quest(t, n, String.valueOf(ra.ordinal()), n * (ra == Rarity.RZADKA ? 600 : 180) * mult, n * 12,
                        "Złów <a>" + n + "</a> ryb " + ra.tag() + "<t> lub lepszych");
            }
            case SPECIES -> {
                List<Species> pool = new ArrayList<>();
                for (Species s : pl.fishes().base())
                    if (s.where() == Species.Where.ANY && s.when() == Species.When.ANY && s.rarity().ordinal() <= 1) pool.add(s);
                Species s = pool.get(r.nextInt(pool.size()));
                int n = s.rarity() == Rarity.POSPOLITA ? 3 + r.nextInt(4) : 1 + r.nextInt(2);
                yield new Quest(t, n, s.id(), n * (s.rarity() == Rarity.POSPOLITA ? 200 : 600) * mult, n * 10,
                        "Złów <a>" + n + "×</a> " + s.display());
            }
            case SELL -> { int n = (5 + r.nextInt(16)) * 1000; yield new Quest(t, n, null, n * 0.25 * mult, n / 100, "Sprzedaj ryby za <money>" + Msg.shortNum(n) + "</money>"); }
            case PERFECT -> { int n = 3 + r.nextInt(6); yield new Quest(t, n, null, n * 250 * mult, n * 10, "Wykonaj <a>" + n + "</a> perfekcyjnych holi"); }
            case TREASURE -> { int n = 1 + r.nextInt(2); yield new Quest(t, n, null, n * 1500 * mult, n * 40, "Wyłów <a>" + n + "</a> " + (n == 1 ? "skarb" : "skarby")); }
            case WEIGHT -> { int n = (3 + r.nextInt(8)) * 10; yield new Quest(t, n, null, n * 40 * mult, n, "Złów łącznie <a>" + n + " kg</a> ryb"); }
            case CREATURE -> { int n = 1 + r.nextInt(2); yield new Quest(t, n, null, n * 2000 * mult, n * 50, "Pokonaj <a>" + n + "</a> " + (n == 1 ? "potwora" : "potwory") + " z głębin"); }
            case STREAK -> { int n = 5 + r.nextInt(4) * 5; yield new Quest(t, n, null, n * 150 * mult, n * 6, "Zrób serię <a>" + n + "</a> udanych holi"); }
        };
    }

    /** Zwiększa postęp pasujących zadań. */
    public void progress(Player p, Type type, String arg, int amount) {
        PlayerData d = pl.store().get(p);
        List<Quest> qs = list(d);
        for (int i = 0; i < 3; i++) {
            Quest q = qs.get(i);
            if (q.type() != type || d.qProg[i] >= q.target()) continue;
            if (type == Type.RARITY && (arg == null || Integer.parseInt(arg) < Integer.parseInt(q.arg()))) continue;
            if (type == Type.SPECIES && !q.arg().equals(arg)) continue;
            int before = d.qProg[i];
            d.qProg[i] = type == Type.STREAK ? Math.max(before, amount) : before + amount;
            d.qProg[i] = Math.min(q.target(), d.qProg[i]);
            d.dirty();
            if (d.qProg[i] >= q.target() && before < q.target()) {
                Msg.send(p, "<ok>Zadanie ukończone!</ok> <t>" + q.text() + " <dark>· " +
                        Msg.button("Odbierz", "/ryby zadania", "Otwórz zadania dnia"));
                Sfx.sound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5f, 1.4f);
            }
        }
    }

    public boolean claim(Player p, int i) {
        PlayerData d = pl.store().get(p);
        Quest q = list(d).get(i);
        if (d.qClaimed[i] || d.qProg[i] < q.target()) return false;
        d.qClaimed[i] = true;
        pl.eco().deposit(p, q.reward());
        pl.fishing().addXp(p, d, q.xp());
        d.dirty();
        Msg.ok(p, "Nagroda: <money>" + pl.eco().format(q.reward()) + "</money> <dark>·</dark> <a>+" + q.xp() + " XP");
        return true;
    }

    public boolean claimBonus(Player p) {
        PlayerData d = pl.store().get(p);
        list(d);
        if (d.qBonus || !(d.qClaimed[0] && d.qClaimed[1] && d.qClaimed[2])) return false;
        d.qBonus = true;
        d.dirty();
        double money = pl.getConfig().getDouble("zadania.bonus-kasa", 5000);
        pl.eco().deposit(p, money);
        giveOrDrop(p, pl.items().bait(Bait.MAGICZNA, 3));
        Msg.ok(p, "Bonus za komplet: <money>" + pl.eco().format(money) + "</money> <dark>+</dark> <#f0a868>3× Magiczna przynęta");
        return true;
    }

    static void giveOrDrop(Player p, org.bukkit.inventory.ItemStack it) {
        for (var left : p.getInventory().addItem(it).values()) p.getWorld().dropItemNaturally(p.getLocation(), left);
    }
}
