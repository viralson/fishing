package pl.lowienie;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Menu: przystań (hub), wędka, wędkarnia, umiejętności, zadania, statystyki, prognoza, ranking, zawody, ustawienia. */
public final class Gui {

    static final String T = "<gradient:#36d1dc:#5b86e5>";
    private static final Material[] WAVES = {Material.LIGHT_BLUE_STAINED_GLASS_PANE, Material.CYAN_STAINED_GLASS_PANE,
            Material.BLUE_STAINED_GLASS_PANE, Material.CYAN_STAINED_GLASS_PANE};

    private final LowieniePlugin pl;

    public Gui(LowieniePlugin pl) { this.pl = pl; }

    // ───────────── pomocnicze ─────────────

    static ItemStack water(Material m) { return Items.of(m, " "); }

    boolean pay(Player p, double cost) {
        if (pl.eco().withdraw(p, cost)) return true;
        Msg.err(p, "Brakuje ci <money>" + pl.eco().format(cost - pl.eco().balance(p)) + "</money>.");
        Sfx.deny(p);
        return false;
    }

    void backButton(Menu m, int slot, java.util.function.Consumer<Player> to) {
        m.set(slot, Items.of(Material.ARROW, "<t>← Powrót", "<m>Wróć do przystani"), (p, t) -> {
            Sfx.back(p);
            to.accept(p);
        });
    }

    /** Dolny rząd – spokojna woda. */
    void seabed(Menu m) {
        int start = m.size() - 9;
        for (int i = start; i < m.size(); i++) if (m.get(i) == null) m.set(i, water(Material.BLUE_STAINED_GLASS_PANE));
    }

    // ───────────── PRZYSTAŃ ─────────────

    public void main(Player p) {
        PlayerData d = pl.store().get(p);
        Menu m = new Menu(6, T + "≋ Przystań wędkarska ≋</gradient>");
        // niebo
        for (int i = 0; i < 9; i++) m.set(i, water(i % 2 == 0 ? Material.LIGHT_BLUE_STAINED_GLASS_PANE : Material.WHITE_STAINED_GLASS_PANE));
        // pomost
        for (int i = 9; i < 36; i++) m.set(i, water(Material.BLACK_STAINED_GLASS_PANE));

        long need = pl.fishing().need(d.level);
        boolean max = d.level >= pl.fishing().maxLevel();
        m.set(4, Items.head(p, "<a>" + Msg.esc(p.getName()),
                "<m>Poziom wędkarza <a>" + d.level + (max ? " <dark>(max)" : ""),
                max ? "<ok>Maksymalny poziom!" : Msg.bar((double) d.xp / need, 20, "a") + " <m>" + Msg.shortNum(d.xp) + "/" + Msg.shortNum(need),
                "",
                "<m>Portfel <money>" + pl.eco().format(pl.eco().balance(p)),
                "<m>Złowione <t>" + Msg.fmt(d.caught) + " <dark>·</dark> <m>seria <t>" + d.streak,
                "<m>Atlas <t>" + d.discovered() + "/" + pl.fishes().size(),
                d.freePoints() > 0 ? "" : null,
                d.freePoints() > 0 ? "<ok>● " + d.freePoints() + " wolnych punktów umiejętności" : null));

        m.set(12, Items.glowIf(d.freePoints() > 0, Items.of(Material.EXPERIENCE_BOTTLE, "<a>Umiejętności",
                "<m>Rozwijaj talenty wędkarza.", "", d.freePoints() > 0 ? "<ok>Masz " + d.freePoints() + " wolne punkty!" : "<m>Punkty zdobywasz co poziom.",
                "", "<a>▸ Kliknij")), (pl2, t) -> { Sfx.enter(p); perks(p); });
        m.set(13, Items.glowIf(pl.tournament().active(), Items.of(Material.BELL, "<#ffd84d>Zawody wędkarskie",
                pl.tournament().active() ? "<ok>● Trwają teraz!" : "<m>Obecnie brak zawodów.",
                pl.tournament().active() ? "<m>Tryb <t>" + pl.tournament().mode().label + " <dark>·</dark> <a>" + Msg.left(pl.tournament().left())
                        : (pl.tournament().untilNext() >= 0 ? "<m>Następne za <t>" + Msg.left(pl.tournament().untilNext()) : "<m>Start tylko przez admina."),
                "", "<a>▸ Kliknij")), (pl2, t) -> { Sfx.enter(p); tournament(p); });
        m.set(14, Items.of(Material.GOLDEN_HELMET, "<a>Ranking", "<m>Najlepsi wędkarze serwera.", "", "<a>▸ Kliknij"),
                (pl2, t) -> { Sfx.enter(p); ranking(p, 0); });

        ItemStack hand = p.getInventory().getItemInMainHand();
        FishItems.Rod rod = pl.items().rod(hand);
        m.set(19, Items.of(Material.FISHING_ROD, "<a>Wędka", rod == null ? "<m>Weź wędkę do ręki," : "<m>W ręce: " + rod.tier().display(),
                rod == null ? "<m>aby ją ulepszyć." : "<m>Ulepszaj szczęście, szybkość,", rod == null ? "" : "<m>kontrolę i żyłkę.", "", "<a>▸ Kliknij"),
                (pl2, t) -> { Sfx.enter(p); rod(p); });
        m.set(20, Items.of(Material.COD_BUCKET, "<a>Siatka", "<m>Twoje złowione ryby.", "",
                "<m>Zajęte <t>" + d.bag.size() + "/" + d.bagCap(), "", "<a>▸ Kliknij"), (pl2, t) -> { Sfx.enter(p); pl.fishGui().bag(p, 0); });
        m.set(21, Items.of(Material.EMERALD, "<a>Targ rybny", "<m>Sprzedawaj ryby po cenach rynkowych.", "",
                "<m>Ryba dnia: " + pl.market().dailyFish().display() + " <a>×" + Msg.dec(pl.market().dailyMult()), "", "<a>▸ Kliknij"),
                (pl2, t) -> { Sfx.enter(p); pl.fishGui().market(p); });
        m.set(22, Items.of(Material.BOOK, "<a>Atlas ryb", "<m>Wszystkie gatunki, rekordy i siedliska.", "",
                "<m>Odkryto " + Msg.bar((double) d.discovered() / pl.fishes().size(), 10, "a") + " <t>" + d.discovered() + "/" + pl.fishes().size(), "", "<a>▸ Kliknij"),
                (pl2, t) -> { Sfx.enter(p); pl.fishGui().atlas(p, Rarity.POSPOLITA); });
        m.set(23, Items.of(Material.TROPICAL_FISH_BUCKET, "<a>Akwarium", "<m>Wystaw ryby – zarabiają same.", "",
                "<m>Ryby <t>" + d.aquarium.size() + "/" + d.aqCap(), "<m>Do odebrania <money>" + pl.eco().format(pl.fishGui().aquariumPending(d)), "", "<a>▸ Kliknij"),
                (pl2, t) -> { Sfx.enter(p); pl.fishGui().aquarium(p); });
        List<Quests.Quest> qs = pl.quests().list(d);
        int done = 0;
        for (int i = 0; i < 3; i++) if (d.qProg[i] >= qs.get(i).target()) done++;
        m.set(24, Items.glowIf(done > 0 && !(d.qClaimed[0] && d.qClaimed[1] && d.qClaimed[2]), Items.of(Material.WRITABLE_BOOK, "<a>Zadania dnia",
                "<m>Trzy nowe zadania każdego dnia.", "", "<m>Ukończone <t>" + done + "/3", "", "<a>▸ Kliknij")), (pl2, t) -> { Sfx.enter(p); quests(p); });
        m.set(25, Items.of(Material.BARREL, "<a>Wędkarnia", "<m>Wędki, przynęty i ulepszenia", "<m>siatki oraz akwarium.", "", "<a>▸ Kliknij"),
                (pl2, t) -> { Sfx.enter(p); shop(p); });

        m.set(30, Items.of(Material.SPYGLASS, "<a>Statystyki", "<m>Twój dziennik wędkarza.", "", "<a>▸ Kliknij"),
                (pl2, t) -> { Sfx.enter(p); stats(p, p); });
        m.set(31, Items.of(Material.COMPASS, "<a>Prognoza", "<m>Co bierze tu i teraz?", "<m>Pora, pogoda, biom i szanse.", "", "<a>▸ Kliknij"),
                (pl2, t) -> { Sfx.enter(p); forecast(p); });
        m.set(32, Items.of(Material.COMPARATOR, "<a>Ustawienia", "<m>Siatka, dźwięki, śmieci, podpowiedzi.", "", "<a>▸ Kliknij"),
                (pl2, t) -> { Sfx.enter(p); settings(p); });

        m.set(34, Items.glowIf(d.enabled, Items.of(d.enabled ? Material.LIME_DYE : Material.GRAY_DYE,
                d.enabled ? "<ok>Wędkarstwo: włączone" : "<err>Wędkarstwo: wyłączone",
                d.enabled ? "<m>Hol, gatunki, skarby i potwory." : "<m>Łowisz jak w zwykłym Minecrafcie.",
                "", "<a>▸ Kliknij, aby przełączyć")), (pl2, t) -> {
            d.enabled = !d.enabled;
            d.dirty();
            Sfx.star(p, d.enabled);
            Msg.send(p, d.enabled ? "<ok>Customowe wędkarstwo włączone." : "<m>Customowe wędkarstwo wyłączone – łowisz po staremu.");
            main(p);
        });
        m.set(28, Items.glowIf(d.toBag, Items.of(d.toBag ? Material.COD_BUCKET : Material.CHEST,
                d.toBag ? "<a>Ryby: do siatki" : "<a>Ryby: do ekwipunku",
                "<m>Gdzie trafiają złowione ryby.", "", "<a>▸ Kliknij, aby przełączyć")), (pl2, t) -> {
            d.toBag = !d.toBag;
            d.dirty();
            Sfx.toggle(p);
            main(p);
        });

        // animowana woda z pływającą rybką
        int[] tick = {0};
        Runnable anim = () -> {
            int k = tick[0]++;
            for (int r = 4; r <= 5; r++) for (int c = 0; c < 9; c++) {
                int slot = r * 9 + c;
                m.set(slot, water(WAVES[Math.floorMod(c - k + r * 2, WAVES.length)]));
            }
            int pass = (k / 9) % 2;
            int col = pass == 0 ? k % 9 : 8 - (k % 9);
            int slot = (pass == 0 ? 45 : 36) + col;
            m.set(slot, Items.of(Material.TROPICAL_FISH, "<a>" + (pass == 0 ? "><(((°>" : "<°)))><"), "<m>Kliknij, jeśli zdążysz…"),
                    (pl2, t) -> {
                        Sfx.splash(p);
                        p.sendActionBar(Msg.mm("<a>Plusk!</a> <m>Rybka czmychnęła pod pomost."));
                    });
        };
        anim.run();
        m.ticker(anim);
        m.open(p);
    }

    // ───────────── WĘDKA ─────────────

    public double upgradeCost(FishItems.Rod r, FishItems.Stat s) {
        double base = switch (s) { case LUCK -> 2000; case SPEED -> 1500; case CONTROL -> 1200; case LINE -> 4000; };
        return Math.round(base * (r.tier().ordinal() + 1) * Math.pow(1.55, r.up(s)) * pl.getConfig().getDouble("ulepszenia.mnoznik-kosztu", 1.0));
    }

    public void rod(Player p) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        FishItems.Rod r = pl.items().rod(hand);
        Menu m = new Menu(5, T + "Warsztat wędki</gradient>");
        m.border(Material.BROWN_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE);
        if (r == null) {
            m.set(22, Items.of(Material.BARRIER, "<err>Brak wędki w ręce", "<m>Weź wędkę do głównej ręki", "<m>i otwórz warsztat ponownie."));
            backButton(m, 36, this::main);
            m.fill(Material.BLACK_STAINED_GLASS_PANE);
            m.open(p);
            return;
        }
        ItemStack show = hand.clone();
        pl.items().refreshRod(show);
        m.set(13, show);
        int[] slots = {29, 30, 32, 33};
        FishItems.Stat[] stats = FishItems.Stat.values();
        for (int i = 0; i < stats.length; i++) {
            FishItems.Stat s = stats[i];
            int lvl = r.up(s), max = r.max(s);
            boolean full = lvl >= max;
            double cost = upgradeCost(r, s);
            m.set(slots[i], Items.of(s.icon, "<a>" + s.name + " <dark>" + lvl + "/" + max,
                    "<m>" + s.desc,
                    "",
                    Msg.bar((double) lvl / max, max, "a"),
                    "",
                    full ? "<ok>Maksymalnie ulepszone" : "<m>Koszt <money>" + pl.eco().format(cost),
                    full ? "<m>Kup lepszą wędkę w wędkarni." : "<a>▸ Kliknij, aby ulepszyć"), (pp, t) -> {
                ItemStack now = p.getInventory().getItemInMainHand();
                FishItems.Rod cur = pl.items().rod(now);
                if (cur == null) { Sfx.deny(p); p.closeInventory(); return; }
                if (cur.up(s) >= cur.max(s)) { Sfx.deny(p); return; }
                double c = upgradeCost(cur, s);
                if (!pay(p, c)) return;
                pl.items().setUpgrade(now, s, cur.up(s) + 1);
                Sfx.upgrade(p);
                Msg.ok(p, s.name + " ulepszone do poziomu <a>" + (cur.up(s) + 1) + "</a>.");
                rod(p);
            });
        }
        m.set(31, Items.of(Material.NAME_TAG, r.tier().display(),
                "<m>Klasa wędki", "",
                "<m>Bazowe: szczęście <t>" + r.tier().luck + "<m>, szybkość <t>" + r.tier().speed,
                "<m>kontrola <t>" + r.tier().control + "<m>, żyłka <t>" + r.tier().line,
                "<m>Limit ulepszeń <t>" + r.tier().maxLevel,
                "",
                "<m>Zaklęcia Szczęście morza i Przynęta",
                "<m>też dodają szczęście i szybkość."));
        backButton(m, 36, this::main);
        m.set(44, Items.of(Material.BARREL, "<a>Wędkarnia", "<m>Kup lepszą wędkę.", "", "<a>▸ Kliknij"), (pp, t) -> { Sfx.enter(p); shop(p); });
        m.fill(Material.GRAY_STAINED_GLASS_PANE);
        m.open(p);
    }

    // ───────────── WĘDKARNIA ─────────────

    public void shop(Player p) {
        PlayerData d = pl.store().get(p);
        Menu m = new Menu(6, T + "Wędkarnia</gradient>");
        for (int i = 0; i < 9; i++) m.set(i, water(Material.BROWN_STAINED_GLASS_PANE));
        m.set(4, Items.of(Material.BARREL, "<a>Wędkarnia", "<m>Portfel <money>" + pl.eco().format(pl.eco().balance(p))));

        // wędki
        int[] rodSlots = {10, 11, 12, 14, 15, 16};
        RodTier[] tiers = RodTier.values();
        for (int i = 0; i < tiers.length; i++) {
            RodTier t = tiers[i];
            ItemStack icon = pl.items().rod(t);
            var meta = icon.getItemMeta();
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
            lore.add(Msg.item(""));
            lore.add(Msg.item(t.price <= 0 ? "<m>Cena <ok>za darmo" : "<m>Cena <money>" + pl.eco().format(t.price)));
            lore.add(Msg.item("<a>▸ Kliknij, aby kupić"));
            meta.lore(lore);
            icon.setItemMeta(meta);
            m.set(rodSlots[i], icon, (pp, c) -> {
                if (t.price > 0 && !pay(p, t.price)) return;
                Quests.giveOrDrop(p, pl.items().rod(t));
                Sfx.cash(p);
                Msg.ok(p, "Kupiono: " + t.display());
            });
        }

        // przynęty
        Bait[] baits = Bait.values();
        for (int i = 0; i < baits.length; i++) {
            Bait b = baits[i];
            boolean sel = d.selected == b;
            int have = pl.items().countBait(p, b);
            m.set(28 + i, Items.glowIf(sel, Items.of(b.mat, Math.max(1, Math.min(64, have)), "<#f0a868>" + b.name + (sel ? " <ok>✔" : ""),
                    List.of("<t>" + b.desc, "", "<m>Paczka <t>" + b.pack + " szt. <dark>·</dark> <money>" + pl.eco().format(b.price),
                            "<m>Masz <t>" + have + " szt.", "",
                            "<a>LPM</a> <m>kup paczkę", "<a>Shift+LPM</a> <m>kup 4 paczki",
                            "<a>PPM</a> <m>" + (sel ? "odznacz jako ulubioną" : "ustaw jako używaną")))), (pp, c) -> {
                if (c.isRightClick()) {
                    d.selected = sel ? null : b;
                    d.dirty();
                    Sfx.star(p, !sel);
                    shop(p);
                    return;
                }
                int packs = c.isShiftClick() ? 4 : 1;
                if (!pay(p, b.price * packs)) return;
                for (int k = 0; k < packs; k++) Quests.giveOrDrop(p, pl.items().bait(b, b.pack));
                Sfx.cash(p);
                shop(p);
            });
        }

        // siatka i akwarium
        boolean bagMax = d.bagLevel >= PlayerData.BAG_MAX_LEVEL;
        double bagCost = 10_000 * Math.pow(3, d.bagLevel);
        m.set(39, Items.of(Material.COD_BUCKET, "<a>Większa siatka",
                "<m>Pojemność <t>" + d.bagCap() + (bagMax ? "" : " <dark>→</dark> <a>" + (d.bagCap() + 18)), "",
                bagMax ? "<ok>Maksymalna wielkość" : "<m>Koszt <money>" + pl.eco().format(bagCost), bagMax ? "" : "<a>▸ Kliknij"), (pp, c) -> {
            if (d.bagLevel >= PlayerData.BAG_MAX_LEVEL) { Sfx.deny(p); return; }
            if (!pay(p, bagCost)) return;
            d.bagLevel++;
            d.dirty();
            Sfx.upgrade(p);
            shop(p);
        });
        boolean aqMax = d.aqLevel >= PlayerData.AQ_MAX_LEVEL;
        double aqCost = 50_000 * Math.pow(4, d.aqLevel);
        m.set(41, Items.of(Material.TROPICAL_FISH_BUCKET, "<a>Większe akwarium",
                "<m>Miejsca <t>" + d.aqCap() + (aqMax ? "" : " <dark>→</dark> <a>" + (d.aqCap() + 9)), "",
                aqMax ? "<ok>Maksymalna wielkość" : "<m>Koszt <money>" + pl.eco().format(aqCost), aqMax ? "" : "<a>▸ Kliknij"), (pp, c) -> {
            if (d.aqLevel >= PlayerData.AQ_MAX_LEVEL) { Sfx.deny(p); return; }
            if (!pay(p, aqCost)) return;
            d.aqLevel++;
            d.dirty();
            Sfx.upgrade(p);
            shop(p);
        });
        backButton(m, 45, this::main);
        seabed(m);
        m.fill(Material.BLACK_STAINED_GLASS_PANE);
        m.open(p);
    }

    // ───────────── UMIEJĘTNOŚCI ─────────────

    public void perks(Player p) {
        PlayerData d = pl.store().get(p);
        Menu m = new Menu(5, T + "Umiejętności</gradient>");
        m.border(Material.PURPLE_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE);
        m.set(4, Items.of(Material.NETHER_STAR, "<a>Punkty: " + d.freePoints(),
                "<m>Dostajesz 1 punkt za każdy poziom.", "<m>Poziom <t>" + d.level + " <dark>·</dark> <m>wydane <t>" + d.spentPoints()));
        int[] slots = {19, 20, 21, 22, 23, 24, 25};
        Perk[] ps = Perk.values();
        for (int i = 0; i < ps.length; i++) {
            Perk k = ps[i];
            int lvl = d.perk(k);
            boolean full = lvl >= Perk.MAX;
            m.set(slots[i], Items.glowIf(lvl > 0, Items.of(k.icon, Math.max(1, lvl), "<a>" + k.name + " <dark>" + lvl + "/" + Perk.MAX,
                    List.of("<t>" + k.desc, "", Msg.bar((double) lvl / Perk.MAX, Perk.MAX * 2, "a"), "",
                            full ? "<ok>Maksymalny poziom" : d.freePoints() > 0 ? "<a>▸ Kliknij, aby rozwinąć" : "<m>Brak wolnych punktów"))), (pp, c) -> {
                if (d.perk(k) >= Perk.MAX || d.freePoints() <= 0) { Sfx.deny(p); return; }
                d.perks.merge(k, 1, Integer::sum);
                d.dirty();
                Sfx.upgrade(p);
                perks(p);
            });
        }
        double resetCost = d.level * pl.getConfig().getDouble("umiejetnosci.koszt-resetu-za-poziom", 1000);
        m.set(40, Items.of(Material.TNT, "<err>Reset umiejętności", "<m>Zwraca wszystkie punkty.", "",
                "<m>Koszt <money>" + pl.eco().format(resetCost), "<a>Shift+PPM</a> <m>aby potwierdzić"), (pp, c) -> {
            if (c != ClickType.SHIFT_RIGHT || d.spentPoints() == 0) { Sfx.deny(p); return; }
            if (!pay(p, resetCost)) return;
            d.perks.clear();
            d.dirty();
            Sfx.toggle(p);
            perks(p);
        });
        backButton(m, 36, this::main);
        m.fill(Material.BLACK_STAINED_GLASS_PANE);
        m.open(p);
    }

    // ───────────── ZADANIA ─────────────

    public void quests(Player p) {
        PlayerData d = pl.store().get(p);
        List<Quests.Quest> qs = pl.quests().list(d);
        Menu m = new Menu(4, T + "Zadania dnia</gradient>");
        m.border(Material.LIME_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE);
        long msLeft = java.time.Duration.between(java.time.LocalDateTime.now(),
                java.time.LocalDate.now().plusDays(1).atStartOfDay()).toMillis();
        m.set(4, Items.of(Material.CLOCK, "<a>Nowe zadania za " + Msg.left(msLeft), "<m>Zadania są losowane codziennie o północy."));
        int[] slots = {11, 13, 15};
        for (int i = 0; i < 3; i++) {
            Quests.Quest q = qs.get(i);
            int prog = d.qProg[i];
            boolean done = prog >= q.target(), claimed = d.qClaimed[i];
            final int idx = i;
            Material icon = claimed ? Material.MAP : done ? Material.FILLED_MAP : Material.PAPER;
            m.set(slots[i], Items.glowIf(done && !claimed, Items.of(icon, (claimed ? "<m><st>" : "<a>") + "Zadanie " + (i + 1),
                    "<t>" + q.text(), "",
                    Msg.bar((double) prog / q.target(), 16, done ? "ok" : "a") + " <m>" + Msg.fmt(prog) + "/" + Msg.fmt(q.target()), "",
                    "<m>Nagroda <money>" + pl.eco().format(q.reward()) + "</money> <dark>+</dark> <a>" + q.xp() + " XP", "",
                    claimed ? "<ok>✔ Odebrano" : done ? "<a>▸ Kliknij, aby odebrać" : "<m>W trakcie…")), (pp, c) -> {
                if (pl.quests().claim(p, idx)) { Sfx.cash(p); quests(p); } else Sfx.deny(p);
            });
        }
        boolean all = d.qClaimed[0] && d.qClaimed[1] && d.qClaimed[2];
        m.set(22, Items.glowIf(all && !d.qBonus, Items.of(Material.CHEST, "<#ffd84d>Bonus za komplet",
                "<m>Ukończ wszystkie trzy zadania.", "",
                "<m>Nagroda <money>" + pl.eco().format(pl.getConfig().getDouble("zadania.bonus-kasa", 5000)) + "</money> <dark>+</dark> <#f0a868>3× Magiczna przynęta", "",
                d.qBonus ? "<ok>✔ Odebrano" : all ? "<a>▸ Kliknij, aby odebrać" : "<m>Jeszcze nie…")), (pp, c) -> {
            if (pl.quests().claimBonus(p)) { Sfx.cash(p); quests(p); } else Sfx.deny(p);
        });
        backButton(m, 27, this::main);
        m.fill(Material.BLACK_STAINED_GLASS_PANE);
        m.open(p);
    }

    // ───────────── STATYSTYKI ─────────────

    public void stats(Player viewer, Player target) {
        PlayerData d = pl.store().get(target);
        Menu m = new Menu(4, T + "Dziennik · " + Msg.esc(target.getName()) + "</gradient>");
        m.border(Material.CYAN_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE);
        m.set(4, Items.head(target, "<a>" + Msg.esc(target.getName()), "<m>Poziom <a>" + d.level));
        Species hv = pl.fishes().get(d.heaviestSp);
        double tries = d.caught + d.escaped;
        m.set(10, Items.of(Material.COD, "<a>Złowione ryby", "<t>" + Msg.fmt(d.caught), "<m>Łącznie <t>" + FishItems.kg(d.totalKg)));
        m.set(11, Items.of(Material.STRING, "<a>Ucieczki", "<t>" + Msg.fmt(d.escaped),
                "<m>Skuteczność <t>" + (tries == 0 ? "–" : Math.round(d.caught * 100 / tries) + "%")));
        m.set(12, Items.of(Material.AMETHYST_SHARD, "<a>Perfekcyjne hole", "<t>" + Msg.fmt(d.perfect)));
        m.set(13, Items.of(Material.BLAZE_POWDER, "<a>Serie", "<m>Obecna <t>" + d.streak, "<m>Najlepsza <t>" + d.bestStreak));
        m.set(14, Items.of(Material.CHEST, "<a>Skarby", "<t>" + Msg.fmt(d.treasures)));
        m.set(15, Items.of(Material.TRIDENT, "<a>Pokonane potwory", "<t>" + Msg.fmt(d.creatures)));
        m.set(16, Items.of(Material.LEATHER_BOOTS, "<a>Śmieci", "<t>" + Msg.fmt(d.junk)));
        m.set(21, Items.of(Material.GOLD_INGOT, "<a>Zarobione", "<money>" + pl.eco().format(d.earned)));
        m.set(22, Items.of(hv == null ? Material.BARRIER : hv.mat(), "<a>Najcięższa ryba",
                hv == null ? "<m>jeszcze brak" : hv.display(), hv == null ? "" : "<t>" + FishItems.kg(d.heaviestKg)));
        m.set(23, Items.of(Material.BOOK, "<a>Atlas", "<t>" + d.discovered() + "/" + pl.fishes().size() + " gatunków"));
        if (viewer == target) backButton(m, 27, this::main);
        m.fill(Material.BLACK_STAINED_GLASS_PANE);
        m.open(viewer);
    }

    // ───────────── PROGNOZA ─────────────

    public void forecast(Player p) { forecast(p, 0); }

    public void forecast(Player p, int page) {
        PlayerData d = pl.store().get(p);
        Set<Species.Where> where = Fishing.where(p.getLocation());
        var w = p.getWorld();
        boolean day = Fishing.isDay(w);
        double luck = pl.fishing().baseLuck(d, pl.items().rod(p.getInventory().getItemInMainHand()));
        double[] wt = Fishing.rarityWeights(luck);
        double sum = 0;
        for (double v : wt) sum += v;

        Menu m = new Menu(6, T + "Prognoza wędkarska</gradient>");
        List<String> lore = new ArrayList<>();
        StringBuilder wh = new StringBuilder();
        for (Species.Where x : where) if (x != Species.Where.ANY) wh.append(wh.isEmpty() ? "" : ", ").append(x.label);
        lore.add("<m>Wody <t>" + (wh.isEmpty() ? "zwykłe" : wh));
        lore.add("<m>Pora <t>" + (day ? "dzień ☀" : "noc ☾") + " <dark>·</dark> <m>pogoda <t>" + (w.isThundering() ? "burza ⚡" : w.hasStorm() ? "deszcz ☂" : "pogodnie"));
        lore.add("<m>Szczęście <a>" + Msg.dec(luck) + " <dark>(bez przynęty)");
        if (pl.frenzy().active()) lore.add("<a>Trwa szał ryb! <m>" + Msg.left(pl.frenzy().left()));
        lore.add("");
        for (Rarity r : Rarity.values())
            lore.add(r.tag() + " <dark>·</dark> <t>" + String.format(java.util.Locale.ROOT, "%.2f%%", wt[r.ordinal()] / sum * 100));
        for (int i = 0; i < 9; i++) if (i != 4) m.set(i, water(day ? Material.LIGHT_BLUE_STAINED_GLASS_PANE : Material.BLUE_STAINED_GLASS_PANE));

        List<Species> ok = new ArrayList<>();
        for (Species s : pl.fishes().all())
            if (where.contains(s.where()) && Fishing.whenOk(s.when(), w)) ok.add(s);
        ok.sort(java.util.Comparator.comparingInt((Species s) -> s.rarity().ordinal()).reversed());
        int unknown = 0;
        for (Species s : ok) if (!d.species.containsKey(s.id())) unknown++;
        lore.add("");
        lore.add("<m>Bierze tu <a>" + ok.size() + "</a> <m>gatunków, nieodkrytych <a>" + unknown);
        m.set(4, Items.of(Material.COMPASS, 1, "<a>Warunki tutaj", lore));
        int pages = Math.max(1, (ok.size() + 35) / 36);
        int pg = Math.max(0, Math.min(page, pages - 1));
        for (int i = 0; i < 36; i++) {
            int idx = pg * 36 + i;
            if (idx >= ok.size()) break;
            Species s = ok.get(idx);
            boolean known = d.species.containsKey(s.id());
            m.set(9 + i, known
                    ? Items.of(s.mat(), s.display(), s.rarity().tag(), "<m>" + s.where().label + " <dark>·</dark> <m>" + s.when().label)
                    : Items.of(Material.GRAY_DYE, s.rarity().c() + "???", s.rarity().tag(), "<m>Nieodkryty – bierze tutaj!"));
        }
        if (ok.isEmpty()) m.set(22, Items.of(Material.BARRIER, "<err>Nic tu nie bierze", "<m>Podejdź bliżej wody."));
        if (pg > 0) m.set(46, Items.of(Material.SPECTRAL_ARROW, "<t>← Strona " + pg), (pp, c) -> { Sfx.page(p); forecast(p, pg - 1); });
        if (pg < pages - 1) m.set(52, Items.of(Material.SPECTRAL_ARROW, "<t>Strona " + (pg + 2) + " →"), (pp, c) -> { Sfx.page(p); forecast(p, pg + 1); });
        backButton(m, 45, this::main);
        seabed(m);
        m.fill(Material.BLACK_STAINED_GLASS_PANE);
        m.open(p);
    }

    // ───────────── RANKING ─────────────

    private static final String[] RANK_TABS = {"Poziom", "Złowione", "Zarobek", "Najcięższa"};
    private static final Material[] RANK_ICONS = {Material.EXPERIENCE_BOTTLE, Material.COD, Material.GOLD_INGOT, Material.ANVIL};

    public void ranking(Player p, int tab) {
        Menu m = new Menu(6, T + "Ranking · " + RANK_TABS[tab] + "</gradient>");
        List<Map.Entry<UUID, Store.Rank>> top = pl.store().top(switch (tab) {
            case 0 -> r -> r.level;
            case 1 -> r -> r.caught;
            case 2 -> r -> r.earned;
            default -> r -> r.heaviest;
        });
        int[] slots = {13, 21, 23, 28, 29, 30, 31, 32, 33, 34};
        String[] medal = {"<#ffd84d>①", "<#c0c0c0>②", "<#cd7f32>③"};
        for (int i = 0; i < slots.length; i++) {
            if (i >= top.size()) {
                m.set(slots[i], Items.of(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "<m>#" + (i + 1) + " – wolne miejsce"));
                continue;
            }
            var e = top.get(i);
            Store.Rank r = e.getValue();
            String val = switch (tab) {
                case 0 -> "Poziom " + r.level;
                case 1 -> Msg.fmt(r.caught) + " ryb";
                case 2 -> pl.eco().format(r.earned);
                default -> FishItems.kg(r.heaviest);
            };
            OfflinePlayer op = Bukkit.getOfflinePlayer(e.getKey());
            m.set(slots[i], Items.head(op, (i < 3 ? medal[i] + " " : "<m>#" + (i + 1) + " ") + "<t>" + Msg.esc(r.name), "<a>" + val));
        }
        for (int i = 0; i < RANK_TABS.length; i++) {
            final int t = i;
            m.set(47 + i + (i >= 2 ? 1 : 0), Items.glowIf(i == tab, Items.of(RANK_ICONS[i], (i == tab ? "<a>" : "<t>") + RANK_TABS[i],
                    i == tab ? "<ok>● wybrane" : "<m>▸ Kliknij")), (pp, c) -> { if (t != tab) { Sfx.page(p); ranking(p, t); } });
        }
        backButton(m, 45, this::main);
        for (int i = 0; i < 9; i++) if (m.get(i) == null) m.set(i, water(Material.YELLOW_STAINED_GLASS_PANE));
        m.fill(Material.BLACK_STAINED_GLASS_PANE);
        m.open(p);
    }

    // ───────────── ZAWODY ─────────────

    public void tournament(Player p) {
        Tournament t = pl.tournament();
        Menu m = new Menu(5, T + "Zawody wędkarskie</gradient>");
        m.border(Material.YELLOW_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE);
        if (t.active()) {
            var st = t.standings();
            int my = -1;
            for (int i = 0; i < st.size(); i++) if (st.get(i).getKey().equals(p.getUniqueId())) my = i;
            m.set(4, Items.of(Material.BELL, "<#ffd84d>" + t.mode().label, "<m>Do końca <a>" + Msg.left(t.left()),
                    "<m>Uczestników <t>" + st.size(),
                    my < 0 ? "<m>Złów coś, by dołączyć!" : "<m>Twoje miejsce <a>#" + (my + 1) + " <dark>·</dark> <t>" + t.value(st.get(my).getValue())));
            int[] slots = {13, 21, 23, 29, 30, 31, 32, 33};
            for (int i = 0; i < slots.length && i < st.size(); i++) {
                var e = st.get(i);
                m.set(slots[i], Items.head(Bukkit.getOfflinePlayer(e.getKey()), "<m>#" + (i + 1) + " <t>" + Msg.esc(t.name(e.getKey())), "<a>" + t.value(e.getValue())));
            }
            if (p.hasPermission("lowienie.admin"))
                m.set(40, Items.of(Material.BARRIER, "<err>Zakończ zawody", "<m>Z nagrodami dla top 3.", "<a>Shift+PPM"), (pp, c) -> {
                    if (c != ClickType.SHIFT_RIGHT) return;
                    t.stop(true);
                    Sfx.toggle(p);
                    tournament(p);
                });
        } else {
            m.set(13, Items.of(Material.CLOCK, "<m>Obecnie brak zawodów",
                    t.untilNext() >= 0 ? "<m>Następne za <a>" + Msg.left(t.untilNext()) : "<m>Automatyczne zawody są wyłączone.",
                    "<m>Nagrody dla najlepszej trójki!"));
            if (p.hasPermission("lowienie.admin")) {
                int[] slots = {29, 31, 33};
                Tournament.Mode[] ms = Tournament.Mode.values();
                for (int i = 0; i < ms.length; i++) {
                    Tournament.Mode md = ms[i];
                    m.set(slots[i], Items.of(Material.LIME_DYE, "<ok>Start: " + md.label, "<m>Admin – " + pl.getConfig().getInt("zawody.minut", 10) + " minut"), (pp, c) -> {
                        t.start(md, pl.getConfig().getInt("zawody.minut", 10));
                        tournament(p);
                    });
                }
            }
        }
        backButton(m, 36, this::main);
        m.fill(Material.BLACK_STAINED_GLASS_PANE);
        m.open(p);
    }

    // ───────────── USTAWIENIA ─────────────

    public void settings(Player p) {
        PlayerData d = pl.store().get(p);
        Menu m = new Menu(3, T + "Ustawienia</gradient>");
        toggle(m, 11, p, Material.FISHING_ROD, "Customowe wędkarstwo", "Wyłączone = zwykłe łowienie jak w vanilli.", d.enabled, () -> d.enabled = !d.enabled);
        toggle(m, 12, p, Material.COD_BUCKET, "Łów do siatki", "Włączone: ryby do siatki. Wyłączone: do ekwipunku.", d.toBag, () -> d.toBag = !d.toBag);
        toggle(m, 13, p, Material.NOTE_BLOCK, "Dźwięki holu", "Tykanie znacznika podczas holu.", d.sounds, () -> d.sounds = !d.sounds);
        toggle(m, 14, p, Material.LEATHER_BOOTS, "Wyrzucaj śmieci", "Śmieci wracają od razu do wody.", d.dropJunk, () -> d.dropJunk = !d.dropJunk);
        toggle(m, 15, p, Material.OAK_SIGN, "Podpowiedzi", "Wskazówki na ekranie i ławice.", d.hints, () -> d.hints = !d.hints);
        backButton(m, 18, this::main);
        m.fill(Material.BLACK_STAINED_GLASS_PANE);
        m.open(p);
    }

    private void toggle(Menu m, int slot, Player p, Material icon, String name, String desc, boolean on, Runnable flip) {
        m.set(slot, Items.glowIf(on, Items.of(icon, "<a>" + name, "<m>" + desc, "", on ? "<ok>● Włączone" : "<err>○ Wyłączone", "<a>▸ Kliknij")), (pp, c) -> {
            flip.run();
            pl.store().get(p).dirty();
            Sfx.star(p, !on);
            settings(p);
        });
        m.set(slot + 9, water(on ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE));
    }
}
