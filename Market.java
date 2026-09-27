package pl.lowienie;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/** Targ rybny – dynamiczne ceny, ryba dnia, sprzedaż. */
public final class Market {

    private static final double[] STAR_MULT = {1.0, 1.0, 1.2, 1.45, 1.8, 2.5};

    private final LowieniePlugin pl;

    public Market(LowieniePlugin pl) { this.pl = pl; }

    /** Ryba dnia – losowana z rzadkości niepospolita/rzadka, jedna dla całego serwera. */
    public Species dailyFish() {
        LocalDate d = LocalDate.now();
        Random r = new Random(d.toEpochDay() * 7919L);
        List<Species> pool = new ArrayList<>(pl.fishes().of(Rarity.NIEPOSPOLITA));
        pool.addAll(pl.fishes().of(Rarity.RZADKA));
        return pool.get(r.nextInt(pool.size()));
    }

    public double dailyMult() { return pl.getConfig().getDouble("rynek.ryba-dnia-mnoznik", 2.5); }

    /** Popyt 50%–100% – spada, gdy dużo się sprzedaje. */
    public double demand(String species) {
        double s = pl.store().supply.getOrDefault(species, 0.0);
        double drop = pl.getConfig().getDouble("rynek.spadek-za-sztuke", 0.012);
        return Math.max(pl.getConfig().getDouble("rynek.minimalny-popyt", 0.5), 1 - s * drop);
    }

    /** Odbudowa popytu (co minutę). */
    public void recover() {
        double rate = pl.getConfig().getDouble("rynek.odbudowa-na-minute", 0.06);
        Iterator<java.util.Map.Entry<String, Double>> it = pl.store().supply.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            double v = e.getValue() * (1 - rate) - 0.05;
            if (v <= 0.01) it.remove(); else e.setValue(v);
        }
        pl.store().dirty();
    }

    /** Cena bazowa (bez popytu i bonusów gracza). */
    public double base(FishEntry e) {
        Species s = pl.fishes().get(e.species());
        if (s == null) return 0;
        double t = s.maxKg() <= s.minKg() ? 0.5 : (e.kg() - s.minKg()) / (s.maxKg() - s.minKg());
        t = Math.max(0, Math.min(1, t));
        return s.price() * (0.7 + 0.8 * t) * STAR_MULT[Math.max(0, Math.min(5, e.stars()))];
    }

    /** Ostateczna cena dla gracza. */
    public double price(PlayerData d, FishEntry e) {
        double v = base(e) * demand(e.species());
        if (e.species().equals(dailyFish().id())) v *= dailyMult();
        v *= 1 + d.perk(Perk.HANDLARZ) * 0.05;
        v *= pl.getConfig().getDouble("rynek.mnoznik", 1.0);
        return Math.floor(v * 100) / 100;
    }

    private void markSold(FishEntry e) {
        pl.store().supply.merge(e.species(), 1.0, Double::sum);
        pl.store().dirty();
    }

    /** Sprzedaje z siatki wszystkie ryby spełniające warunek. Zwraca [sztuki, kwota]. */
    public double[] sellBag(Player p, Predicate<FishEntry> filter) {
        PlayerData d = pl.store().get(p);
        double total = 0;
        int n = 0;
        Iterator<FishEntry> it = d.bag.iterator();
        while (it.hasNext()) {
            FishEntry e = it.next();
            if (!filter.test(e)) continue;
            total += price(d, e);
            markSold(e);
            it.remove();
            n++;
        }
        return payout(p, d, n, total);
    }

    /** Sprzedaje ryby z ekwipunku. */
    public double[] sellInventory(Player p, Predicate<FishEntry> filter) {
        PlayerData d = pl.store().get(p);
        double total = 0;
        int n = 0;
        ItemStack[] c = p.getInventory().getStorageContents();
        for (int i = 0; i < c.length; i++) {
            FishEntry e = pl.items().entry(c[i]);
            if (e == null || !filter.test(e)) continue;
            int amt = c[i].getAmount();
            for (int k = 0; k < amt; k++) { total += price(d, e); markSold(e); }
            n += amt;
            p.getInventory().setItem(i, null);
        }
        return payout(p, d, n, total);
    }

    /** Sprzedaż jednej ryby z siatki. */
    public double sellOne(Player p, FishEntry e) {
        PlayerData d = pl.store().get(p);
        if (!d.bag.remove(e)) return -1;
        double v = price(d, e);
        markSold(e);
        payout(p, d, 1, v);
        return v;
    }

    private double[] payout(Player p, PlayerData d, int n, double total) {
        if (n > 0) {
            total = Math.floor(total * 100) / 100;
            pl.eco().deposit(p, total);
            d.earned += total;
            d.dirty();
            pl.quests().progress(p, Quests.Type.SELL, null, (int) Math.round(total));
        }
        return new double[]{n, total};
    }

    /** Trend do wyświetlenia. */
    public String trend(String species) {
        double dm = demand(species);
        if (dm >= 0.97) return "<ok>▲ " + Math.round(dm * 100) + "%</ok>";
        if (dm >= 0.75) return "<#ffd84d>▶ " + Math.round(dm * 100) + "%</#ffd84d>";
        return "<err>▼ " + Math.round(dm * 100) + "%</err>";
    }
}
