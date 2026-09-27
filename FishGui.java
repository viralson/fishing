package pl.lowienie;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static pl.lowienie.Gui.T;
import static pl.lowienie.Gui.water;

/** Menu: atlas, siatka, targ, akwarium. */
public final class FishGui {

    private static final String[] SORTS = {"Najnowsze", "Najcenniejsze", "Rzadkość", "Waga"};
    private final LowieniePlugin pl;
    private final Map<UUID, Integer> sort = new HashMap<>();

    public FishGui(LowieniePlugin pl) { this.pl = pl; }

    static Material glass(Rarity r) {
        return switch (r) {
            case POSPOLITA -> Material.LIGHT_GRAY_STAINED_GLASS_PANE;
            case NIEPOSPOLITA -> Material.LIME_STAINED_GLASS_PANE;
            case RZADKA -> Material.LIGHT_BLUE_STAINED_GLASS_PANE;
            case EPICKA -> Material.PURPLE_STAINED_GLASS_PANE;
            case LEGENDARNA -> Material.YELLOW_STAINED_GLASS_PANE;
            case MITYCZNA -> Material.RED_STAINED_GLASS_PANE;
        };
    }

    static Material dye(Rarity r) {
        return switch (r) {
            case POSPOLITA -> Material.LIGHT_GRAY_DYE;
            case NIEPOSPOLITA -> Material.LIME_DYE;
            case RZADKA -> Material.LIGHT_BLUE_DYE;
            case EPICKA -> Material.PURPLE_DYE;
            case LEGENDARNA -> Material.YELLOW_DYE;
            case MITYCZNA -> Material.RED_DYE;
        };
    }

    // ───────────── ATLAS ─────────────

    public void atlas(Player p, Rarity tab) { atlas(p, tab, 0); }

    public void atlas(Player p, Rarity tab, int page) {
        PlayerData d = pl.store().get(p);
        List<Species> list = pl.fishes().of(tab);
        int pages = Math.max(1, (list.size() + 35) / 36);
        int pg = Math.max(0, Math.min(page, pages - 1));
        Menu m = new Menu(6, T + "Atlas ryb</gradient> <dark>· " + tab.tag() + " <dark>" + (pg + 1) + "/" + pages);
        for (int i = 0; i < 9; i++) m.set(i, water(glass(tab)));
        int pct = d.discovered() * 100 / pl.fishes().size();
        int known = 0;
        for (Species s : list) if (d.species.containsKey(s.id())) known++;
        m.set(4, Items.of(Material.BOOK, "<a>Atlas ryb",
                "<m>Odkryto " + Msg.bar(pct / 100.0, 20, "a") + " <t>" + pct + "%",
                "<m>Gatunki <t>" + d.discovered() + "/" + pl.fishes().size(),
                "<m>W tej zakładce <t>" + known + "/" + list.size(), "",
                "<m>Nagrody za odkrycia:",
                mile(d, 25, "5k"), mile(d, 50, "20k"), mile(d, 75, "60k"), mile(d, 100, "250k + Tytanowa wędka")));

        Species daily = pl.market().dailyFish();
        for (int i = 0; i < 36; i++) {
            int idx = pg * 36 + i;
            if (idx >= list.size()) break;
            Species s = list.get(idx);
            int slot = 9 + i;
            PlayerData.SpStat st = d.species.get(s.id());
            if (st == null) {
                m.set(slot, Items.of(Material.GRAY_DYE, s.rarity().c() + "???",
                        s.rarity().tag(), "",
                        "<m>Siedlisko <t>" + s.where().label, "<m>Kiedy <t>" + s.when().label, "",
                        "<dark>Jeszcze nie złowiono"));
                continue;
            }
            Store.Record rec = pl.store().records.get(s.id());
            double lo = pl.market().base(new FishEntry(s.id(), s.minCm(), s.minKg(), 1, "", 0)) * pl.market().demand(s.id());
            double hi = pl.market().base(new FishEntry(s.id(), s.maxCm(), s.maxKg(), 5, "", 0)) * pl.market().demand(s.id());
            if (s == daily) { lo *= pl.market().dailyMult(); hi *= pl.market().dailyMult(); }
            List<String> lore = new ArrayList<>(List.of(
                    s.rarity().tag() + (s == daily ? " <dark>·</dark> <a>Ryba dnia!" : ""),
                    "<m><i>" + s.desc(), "",
                    "<m>Siedlisko <t>" + s.where().label + " <dark>·</dark> <t>" + s.when().label,
                    "<m>Rozmiar <t>" + Math.round(s.minCm()) + "–" + Math.round(s.maxCm()) + " cm", "",
                    "<m>Złowiono <t>" + st.count + "×",
                    "<m>Twój rekord <t>" + FishItems.kg(st.bestKg) + " <dark>·</dark> " + FishEntry.starsTag(Math.max(1, st.bestStars)),
                    "<m>Rekord serwera <a>" + (rec == null ? "–" : FishItems.kg(rec.kg()) + " <m>(" + Msg.esc(rec.holder()) + ")"), "",
                    "<m>Cena <money>" + Msg.shortNum(lo) + "–" + Msg.shortNum(hi) + "</money> <dark>·</dark> " + pl.market().trend(s.id())));
            m.set(slot, Items.glowIf(s == daily, Items.of(s.mat(), 1, s.display(), lore)));
        }

        backButton(m, 45);
        if (pg > 0) m.set(46, Items.of(Material.SPECTRAL_ARROW, "<t>← Strona " + pg), (pp, c) -> { Sfx.page(p); atlas(p, tab, pg - 1); });
        if (pg < pages - 1) m.set(53, Items.of(Material.SPECTRAL_ARROW, "<t>Strona " + (pg + 2) + " →"), (pp, c) -> { Sfx.page(p); atlas(p, tab, pg + 1); });
        Rarity[] rs = Rarity.values();
        for (int i = 0; i < rs.length; i++) {
            Rarity r = rs[i];
            int total = pl.fishes().of(r).size();
            m.set(47 + i, Items.glowIf(r == tab, Items.of(dye(r), r.tag() + " <dark>(" + total + ")",
                    r == tab ? "<ok>● wybrane" : "<m>▸ Kliknij")),
                    (pp, c) -> { if (r != tab) { Sfx.page(p); atlas(p, r, 0); } });
        }
        m.fill(Material.BLACK_STAINED_GLASS_PANE);
        m.open(p);
    }

    private String mile(PlayerData d, int pct, String reward) {
        return (d.atlasRewards.contains(pct) ? "<ok>✔ " : "<dark>○ ") + "<m>" + pct + "% <dark>·</dark> <money>" + reward;
    }

    private void backButton(Menu m, int slot) {
        m.set(slot, Items.of(Material.ARROW, "<t>← Powrót", "<m>Wróć do przystani"), (p, t) -> {
            Sfx.back(p);
            pl.gui().main(p);
        });
    }

    // ───────────── SIATKA ─────────────

    private List<FishEntry> sorted(PlayerData d, int mode) {
        List<FishEntry> l = new ArrayList<>(d.bag);
        switch (mode) {
            case 1 -> l.sort(Comparator.comparingDouble((FishEntry e) -> pl.market().price(d, e)).reversed());
            case 2 -> l.sort(Comparator.comparingInt((FishEntry e) -> {
                Species s = pl.fishes().get(e.species());
                return s == null ? 0 : s.rarity().ordinal();
            }).reversed().thenComparing(Comparator.comparingDouble(FishEntry::kg).reversed()));
            case 3 -> l.sort(Comparator.comparingDouble(FishEntry::kg).reversed());
            default -> l.sort(Comparator.comparingLong(FishEntry::time).reversed());
        }
        return l;
    }

    public void bag(Player p, int page) {
        PlayerData d = pl.store().get(p);
        int mode = sort.getOrDefault(p.getUniqueId(), 0);
        List<FishEntry> list = sorted(d, mode);
        int pages = Math.max(1, (list.size() + 44) / 45);
        int pg = Math.max(0, Math.min(page, pages - 1));
        Menu m = new Menu(6, T + "Siatka</gradient> <dark>· " + d.bag.size() + "/" + d.bagCap());

        for (int i = 0; i < 45; i++) {
            int idx = pg * 45 + i;
            if (idx >= list.size()) break;
            FishEntry e = list.get(idx);
            Species s = pl.fishes().get(e.species());
            if (s == null) continue;
            double price = pl.market().price(d, e);
            List<String> lore = pl.items().fishLore(s, e,
                    "<m>Wartość <money>" + pl.eco().format(price) + "\n\n<a>LPM</a> <m>sprzedaj\n<a>PPM</a> <m>wyjmij do ekwipunku\n<a>Shift+LPM</a> <m>do akwarium");
            m.set(i, Items.of(s.mat(), 1, s.display(), lore), (pp, c) -> {
                if (!d.bag.contains(e)) { bag(p, pg); return; }
                if (c == ClickType.SHIFT_LEFT) {
                    if (d.aquarium.size() >= d.aqCap()) { Msg.err(p, "Akwarium jest pełne."); Sfx.deny(p); return; }
                    collect(p, d, true);
                    d.bag.remove(e);
                    d.aquarium.add(e);
                    d.dirty();
                    Sfx.splash(p);
                } else if (c.isRightClick()) {
                    if (p.getInventory().firstEmpty() < 0) { Msg.err(p, "Nie masz miejsca w ekwipunku."); Sfx.deny(p); return; }
                    d.bag.remove(e);
                    d.dirty();
                    p.getInventory().addItem(pl.items().fish(e));
                    Sfx.pickup(p);
                } else if (c.isLeftClick()) {
                    double v = pl.market().sellOne(p, e);
                    if (v < 0) return;
                    Sfx.cash(p);
                    p.sendActionBar(Msg.mm("<m>Sprzedano " + s.display() + " <m>za <money>" + pl.eco().format(v)));
                } else return;
                bag(p, pg);
            });
        }
        for (int i = 45; i < 54; i++) m.set(i, water(Material.BLUE_STAINED_GLASS_PANE));
        backButton(m, 45);
        if (pg > 0) m.set(46, Items.of(Material.SPECTRAL_ARROW, "<t>← Strona " + pg), (pp, c) -> { Sfx.page(p); bag(p, pg - 1); });
        if (pg < pages - 1) m.set(52, Items.of(Material.SPECTRAL_ARROW, "<t>Strona " + (pg + 2) + " →"), (pp, c) -> { Sfx.page(p); bag(p, pg + 1); });

        List<String> sl = new ArrayList<>();
        for (int i = 0; i < SORTS.length; i++) sl.add((i == mode ? "<a>▸ " : "<dark>  ") + SORTS[i]);
        m.set(47, Items.of(Material.HOPPER, 1, "<a>Sortowanie", sl), (pp, c) -> {
            sort.put(p.getUniqueId(), (mode + 1) % SORTS.length);
            Sfx.toggle(p);
            bag(p, 0);
        });
        double total = 0;
        int valuable = 0;
        for (FishEntry e : d.bag) {
            total += pl.market().price(d, e);
            Species s = pl.fishes().get(e.species());
            if (s != null && s.rarity().ordinal() >= Rarity.LEGENDARNA.ordinal()) valuable++;
        }
        m.set(49, Items.of(Material.EMERALD, "<a>Sprzedaj wszystko",
                "<m>Ryb <t>" + d.bag.size() + " <dark>·</dark> <m>wartość <money>" + pl.eco().format(total), "",
                valuable > 0 ? "<m>Legendarne i mityczne (" + valuable + ") są chronione." : "<m>Wszystko trafi na targ.",
                "", "<a>LPM</a> <m>sprzedaj", valuable > 0 ? "<a>Shift+LPM</a> <m>sprzedaj razem z cennymi" : null), (pp, c) -> {
            boolean all = c == ClickType.SHIFT_LEFT;
            double[] r = pl.market().sellBag(p, e -> all || isCheap(e));
            result(p, r);
            bag(p, 0);
        });
        m.set(50, Items.glowIf(d.toBag, Items.of(Material.COD_BUCKET, "<a>Łów do siatki",
                d.toBag ? "<ok>● Włączone" : "<err>○ Wyłączone", "<m>Kliknij, aby przełączyć.")), (pp, c) -> {
            d.toBag = !d.toBag;
            d.dirty();
            Sfx.star(p, d.toBag);
            bag(p, pg);
        });
        m.set(53, Items.of(Material.OAK_SIGN, "<a>Pojemność " + d.bag.size() + "/" + d.bagCap(),
                "<m>Kliknij rybę w swoim ekwipunku,", "<m>aby włożyć ją do siatki.", "", "<m>Większa siatka w wędkarni."));
        m.onBottom((pp, slot, c) -> {
            ItemStack it = p.getInventory().getItem(slot);
            FishEntry e = pl.items().entry(it);
            if (e == null) return;
            int n = Math.min(it.getAmount(), d.bagCap() - d.bag.size());
            if (n <= 0) { Msg.err(p, "Siatka jest pełna."); Sfx.deny(p); return; }
            for (int k = 0; k < n; k++) d.bag.add(e);
            it.setAmount(it.getAmount() - n);
            d.dirty();
            Sfx.splash(p);
            bag(p, pg);
        });
        m.open(p);
    }

    private boolean isCheap(FishEntry e) {
        Species s = pl.fishes().get(e.species());
        return s == null || s.rarity().ordinal() < Rarity.LEGENDARNA.ordinal();
    }

    private void result(Player p, double[] r) {
        if (r[0] <= 0) { Msg.err(p, "Nie ma czego sprzedać."); Sfx.deny(p); return; }
        Sfx.cash(p);
        Msg.ok(p, "Sprzedano <a>" + (int) r[0] + "</a> ryb za <money>" + pl.eco().format(r[1]) + "</money>.");
    }

    // ───────────── TARG ─────────────

    public void market(Player p) {
        PlayerData d = pl.store().get(p);
        Menu m = new Menu(5, T + "Targ rybny</gradient>");
        for (int i = 0; i < 9; i++) m.set(i, water(i % 2 == 0 ? Material.LIME_STAINED_GLASS_PANE : Material.GREEN_STAINED_GLASS_PANE));
        Species daily = pl.market().dailyFish();
        m.set(4, Items.glow(Items.of(daily.mat(), "<a>Ryba dnia: " + daily.display(),
                "<m>Dziś skupujemy ją <a>×" + Msg.dec(pl.market().dailyMult()) + "</a> drożej!", "",
                "<m>Siedlisko <t>" + daily.where().label + " <dark>·</dark> <t>" + daily.when().label)));

        int[] slots = {19, 20, 21, 23, 24, 25};
        Rarity[] rs = Rarity.values();
        for (int i = 0; i < rs.length; i++) {
            Rarity r = rs[i];
            int n = 0;
            double v = 0;
            for (FishEntry e : d.bag) {
                Species s = pl.fishes().get(e.species());
                if (s != null && s.rarity() == r) { n++; v += pl.market().price(d, e); }
            }
            boolean guard = r.ordinal() >= Rarity.LEGENDARNA.ordinal();
            m.set(slots[i], Items.of(dye(r), Math.max(1, Math.min(64, n)), "<t>Sprzedaj " + r.tag(),
                    List.of("<m>W siatce <t>" + n + " <dark>·</dark> <money>" + pl.eco().format(v), "",
                            n == 0 ? "<m>Brak ryb tej rzadkości." : guard ? "<a>Shift+LPM</a> <m>aby sprzedać" : "<a>▸ Kliknij")), (pp, c) -> {
                if (guard && c != ClickType.SHIFT_LEFT) { Sfx.deny(p); return; }
                result(p, pl.market().sellBag(p, e -> {
                    Species s = pl.fishes().get(e.species());
                    return s != null && s.rarity() == r;
                }));
                market(p);
            });
        }
        double total = 0;
        for (FishEntry e : d.bag) if (isCheap(e)) total += pl.market().price(d, e);
        m.set(22, Items.of(Material.EMERALD_BLOCK, "<ok><b>Sprzedaj siatkę</b>",
                "<m>Wszystko poza legendarnymi i mitycznymi.", "", "<m>Wartość <money>" + pl.eco().format(total), "", "<a>▸ Kliknij"), (pp, c) -> {
            result(p, pl.market().sellBag(p, this::isCheap));
            market(p);
        });

        int invN = 0;
        double invV = 0;
        for (ItemStack it : p.getInventory().getStorageContents()) {
            FishEntry e = pl.items().entry(it);
            if (e != null) { invN += it.getAmount(); invV += pl.market().price(d, e) * it.getAmount(); }
        }
        m.set(30, Items.of(Material.CHEST, "<a>Sprzedaj z ekwipunku",
                "<m>Ryby-przedmioty <t>" + invN + " <dark>·</dark> <money>" + pl.eco().format(invV), "", "<a>▸ Kliknij"), (pp, c) -> {
            result(p, pl.market().sellInventory(p, e -> true));
            market(p);
        });

        List<Map.Entry<String, Double>> over = new ArrayList<>(pl.store().supply.entrySet());
        over.sort(Map.Entry.<String, Double>comparingByValue().reversed());
        List<String> ol = new ArrayList<>();
        ol.add("<m>Im więcej sprzedajecie danego gatunku,");
        ol.add("<m>tym niżej spada jego cena. Popyt wraca z czasem.");
        ol.add("");
        if (over.isEmpty()) ol.add("<ok>Wszystkie ceny są pełne!");
        for (int i = 0; i < Math.min(6, over.size()); i++) {
            Species s = pl.fishes().get(over.get(i).getKey());
            if (s != null) ol.add(s.display() + " <dark>·</dark> " + pl.market().trend(s.id()));
        }
        m.set(32, Items.of(Material.PAPER, 1, "<a>Przełowione gatunki", ol));
        m.set(34, Items.of(Material.BOOK, "<a>Cennik", "<m>Ceny każdego gatunku", "<m>znajdziesz w atlasie.", "", "<a>▸ Kliknij"),
                (pp, c) -> { Sfx.enter(p); atlas(p, Rarity.POSPOLITA); });
        m.set(28, Items.of(Material.COD_BUCKET, "<a>Siatka", "<m>Sprzedawaj pojedynczo.", "", "<a>▸ Kliknij"), (pp, c) -> { Sfx.enter(p); bag(p, 0); });
        backButton(m, 36);
        for (int i = 37; i < 45; i++) m.set(i, water(Material.GREEN_STAINED_GLASS_PANE));
        m.fill(Material.BLACK_STAINED_GLASS_PANE);
        m.open(p);
    }

    // ───────────── AKWARIUM ─────────────

    public double hourly(PlayerData d) {
        double rate = pl.getConfig().getDouble("akwarium.procent-wartosci-na-godzine", 0.01);
        double sum = 0;
        for (FishEntry e : d.aquarium) sum += pl.market().base(e) * rate;
        return sum;
    }

    public double aquariumPending(PlayerData d) {
        double hours = Math.min(pl.getConfig().getDouble("akwarium.limit-godzin", 24),
                (System.currentTimeMillis() - d.aqLast) / 3_600_000.0);
        return Math.floor(hourly(d) * Math.max(0, hours) * 100) / 100;
    }

    /** Wypłaca zaległy dochód akwarium. */
    public double collect(Player p, PlayerData d, boolean silent) {
        double v = aquariumPending(d);
        d.aqLast = System.currentTimeMillis();
        d.dirty();
        if (v > 0) {
            pl.eco().deposit(p, v);
            d.earned += v;
            if (!silent) Msg.ok(p, "Akwarium przyniosło <money>" + pl.eco().format(v) + "</money>.");
        }
        return v;
    }

    private static final int[] TANK = {9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35};
    private static final Material[] FLOOR = {Material.SAND, Material.SEAGRASS, Material.SAND, Material.TUBE_CORAL, Material.SAND,
            Material.KELP, Material.SAND, Material.BRAIN_CORAL, Material.SAND};

    public void aquarium(Player p) {
        PlayerData d = pl.store().get(p);
        Menu m = new Menu(6, T + "Akwarium</gradient> <dark>· " + d.aquarium.size() + "/" + d.aqCap());
        for (int i = 0; i < 9; i++) m.set(i, water(Material.GLASS_PANE));
        m.set(4, Items.of(Material.TROPICAL_FISH_BUCKET, "<a>Twoje akwarium",
                "<m>Ryby <t>" + d.aquarium.size() + "/" + d.aqCap(),
                "<m>Dochód <money>" + pl.eco().format(hourly(d)) + "</money><m>/h",
                "<m>Limit gromadzenia <t>" + pl.getConfig().getInt("akwarium.limit-godzin", 24) + "h", "",
                "<m>Kliknij rybę, aby wrócić z nią do siatki."));
        for (int i = 0; i < 9; i++) m.set(36 + i, Items.of(FLOOR[i], " "));

        // pozycje ryb w zbiorniku
        List<FishEntry> fish = new ArrayList<>(d.aquarium);
        int[] pos = new int[fish.size()];
        List<Integer> free = new ArrayList<>();
        for (int i = 0; i < TANK.length && i < d.aqCap(); i++) free.add(i);
        java.util.Collections.shuffle(free);
        for (int i = 0; i < fish.size(); i++) pos[i] = free.get(i);
        int cap = Math.min(TANK.length, d.aqCap());
        int[] tick = {0};
        int[] bubbles = {ThreadLocalRandom.current().nextInt(9), ThreadLocalRandom.current().nextInt(9)};
        int[] bubbleRow = {2, 0};

        Runnable draw = () -> {
            for (int i = 0; i < TANK.length; i++) {
                boolean locked = i >= cap;
                m.set(TANK[i], water(locked ? Material.BLACK_STAINED_GLASS_PANE : Material.BLUE_STAINED_GLASS_PANE));
            }
            for (int b = 0; b < bubbles.length; b++) {
                int slot = 9 + bubbleRow[b] * 9 + bubbles[b];
                int ti = slot - 9;
                if (ti < cap) m.set(slot, water(Material.LIGHT_BLUE_STAINED_GLASS_PANE));
            }
            for (int i = 0; i < fish.size(); i++) {
                FishEntry e = fish.get(i);
                Species s = pl.fishes().get(e.species());
                if (s == null) continue;
                List<String> lore = pl.items().fishLore(s, e, "<m>Dochód <money>" + pl.eco().format(pl.market().base(e)
                        * pl.getConfig().getDouble("akwarium.procent-wartosci-na-godzine", 0.01)) + "</money><m>/h\n\n<a>▸ Kliknij</a> <m>do siatki");
                m.set(TANK[pos[i]], Items.of(s.mat(), 1, s.display(), lore), (pp, c) -> {
                    if (!d.aquarium.contains(e)) return;
                    collect(p, d, false);
                    d.aquarium.remove(e);
                    pl.fishing().store(p, d, e);
                    d.dirty();
                    Sfx.splash(p);
                    aquarium(p);
                });
            }
        };
        draw.run();
        m.ticker(() -> {
            tick[0]++;
            // bąbelki co tick w górę
            for (int b = 0; b < bubbles.length; b++) {
                bubbleRow[b]--;
                if (bubbleRow[b] < 0) { bubbleRow[b] = 2; bubbles[b] = ThreadLocalRandom.current().nextInt(9); }
            }
            // ryby płyną co drugi tick
            if (tick[0] % 2 == 0) {
                for (int i = 0; i < pos.length; i++) {
                    if (ThreadLocalRandom.current().nextInt(3) == 0) continue;
                    int row = pos[i] / 9, col = pos[i] % 9;
                    int dir = ThreadLocalRandom.current().nextInt(4);
                    int nc = col + (dir == 0 ? 1 : dir == 1 ? -1 : 0);
                    int nr = row + (dir == 2 ? 1 : dir == 3 ? -1 : 0);
                    if (nc < 0 || nc > 8 || nr < 0 || nr > 2) continue;
                    int np = nr * 9 + nc;
                    if (np >= cap) continue;
                    boolean taken = false;
                    for (int k = 0; k < pos.length; k++) if (k != i && pos[k] == np) { taken = true; break; }
                    if (!taken) pos[i] = np;
                }
            }
            // czyszczenie akcji ze starych pozycji
            for (int slot : TANK) m.set(slot, m.get(slot), null);
            draw.run();
        });

        for (int i = 45; i < 54; i++) m.set(i, water(Material.BLACK_STAINED_GLASS_PANE));
        backButton(m, 45);
        double pending = aquariumPending(d);
        m.set(49, Items.glowIf(pending > 0, Items.of(Material.GOLD_INGOT, "<a>Odbierz dochód",
                "<m>Zgromadzone <money>" + pl.eco().format(pending), "", "<a>▸ Kliknij")), (pp, c) -> {
            if (collect(p, d, false) > 0) Sfx.cash(p); else Sfx.deny(p);
            aquarium(p);
        });
        m.set(51, Items.of(Material.OAK_SIGN, "<a>Jak dodać rybę?",
                "<a>Shift+LPM</a> <m>na rybie w siatce", "<m>albo kliknij rybę w swoim ekwipunku.", "",
                "<m>Cenniejsze ryby = większy dochód."));
        m.onBottom((pp, slot, c) -> {
            ItemStack it = p.getInventory().getItem(slot);
            FishEntry e = pl.items().entry(it);
            if (e == null) return;
            if (d.aquarium.size() >= d.aqCap()) { Msg.err(p, "Akwarium jest pełne."); Sfx.deny(p); return; }
            collect(p, d, true);
            d.aquarium.add(e);
            it.setAmount(it.getAmount() - 1);
            d.dirty();
            Sfx.splash(p);
            aquarium(p);
        });
        m.open(p);
    }
}
