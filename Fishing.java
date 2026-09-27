package pl.lowienie;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Firework;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkMeta;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Branie, minigra holu i nagrody. */
public final class Fishing {

    public enum Kind { FISH, TREASURE, CREATURE }

    /** Co siedzi na haczyku (znane przed holem – gracz widzi tylko kolor). */
    public record Plan(Kind kind, Species species, Rarity diff, double luck, boolean hotspot, Bait bait, Location hook) {}

    /** Trwający hol. */
    final class Hol {
        final Player p;
        final Plan plan;
        final int len = 30;
        final int need, maxMisses;
        final BossBar bar;
        double cursor, dir = 1, speed;
        int zoneStart, zoneW, hits, misses, perfectHits, idle, cooldown, age;
        boolean anyMiss;

        Hol(Player p, Plan plan, FishItems.Rod rod, PlayerData d) {
            this.p = p;
            this.plan = plan;
            int r = plan.diff().ordinal();
            this.need = plan.diff().hits();
            this.maxMisses = rod.line();
            this.zoneW = (int) Math.max(2, Math.min(14, Math.round(4 + rod.control() * 0.6 - r * 0.5)));
            this.speed = (0.45 + r * 0.13) * (1 - d.perk(Perk.CIERPLIWOSC) * 0.07)
                    * pl.getConfig().getDouble("minigra.predkosc", 1.0);
            this.cursor = ThreadLocalRandom.current().nextInt(len);
            moveZone();
            String title = switch (plan.kind()) {
                case FISH -> plan.diff().c() + "Coś bierze… <b>" + plan.diff().label.toLowerCase() + "</b>!";
                case TREASURE -> "<#ffd84d>Coś ciężkiego… może skarb?";
                case CREATURE -> "<err><b>Coś ogromnego szarpie żyłką!</b>";
            };
            this.bar = BossBar.bossBar(Msg.mm(title), 0f,
                    plan.kind() == Kind.CREATURE ? BossBar.Color.RED : plan.kind() == Kind.TREASURE ? BossBar.Color.YELLOW : plan.diff().bar,
                    BossBar.Overlay.NOTCHED_10);
        }

        void moveZone() {
            zoneStart = ThreadLocalRandom.current().nextInt(0, len - zoneW + 1);
        }

        int center() { return zoneStart + zoneW / 2; }

        String render() {
            int pos = (int) Math.round(cursor);
            StringBuilder sb = new StringBuilder("<m>⟨</m>");
            for (int i = 0; i < len; i++) {
                boolean inZone = i >= zoneStart && i < zoneStart + zoneW;
                String col;
                if (i == pos) col = inZone ? "#ffffff" : "#ffd84d";
                else if (inZone) col = i == center() ? "#2ea043" : "#7ee787";
                else col = "#30363d";
                sb.append('<').append(col).append(">▌</").append(col).append('>');
            }
            sb.append("<m>⟩</m>  ");
            for (int i = 0; i < need; i++) sb.append(i < hits ? "<a>◆</a>" : "<dark>◇</dark>");
            sb.append("  ");
            int lives = maxMisses - misses + 1;
            for (int i = 0; i < maxMisses + 1; i++) sb.append(i < lives ? "<err>❤</err>" : "<dark>❤</dark>");
            return sb.toString();
        }
    }

    private final LowieniePlugin pl;
    private final Map<UUID, Hol> active = new HashMap<>();
    private final Map<UUID, Long> lastCast = new HashMap<>();

    public Fishing(LowieniePlugin pl) { this.pl = pl; }

    public boolean inHol(Player p) { return active.containsKey(p.getUniqueId()); }

    public Map<UUID, Long> lastCasts() { return lastCast; }

    // ───────────── zarzucenie ─────────────

    public void onCast(Player p, FishHook hook) {
        PlayerData d = pl.store().get(p);
        FishItems.Rod rod = pl.items().rod(p.getInventory().getItemInMainHand());
        if (rod == null) rod = pl.items().rod(p.getInventory().getItemInOffHand());
        if (rod == null) return;
        lastCast.put(p.getUniqueId(), System.currentTimeMillis());
        Bait bait = pl.items().activeBait(p, d);
        double f = 1.0 / (1 + rod.speed() * 0.1);
        if (bait != null) f *= 1 - bait.speed;
        if (pl.frenzy().active()) f *= 0.5;
        f *= pl.getConfig().getDouble("branie.mnoznik-czasu", 1.0);
        int min = (int) Math.max(20, 100 * f);
        int max = (int) Math.max(min + 20, 450 * f);
        hook.setApplyLure(false);
        hook.setWaitTime(min, max);
    }

    // ───────────── branie ─────────────

    public void onBite(Player p, FishHook hook) {
        PlayerData d = pl.store().get(p);
        ItemStack hand = p.getInventory().getItemInMainHand();
        FishItems.Rod rod = pl.items().rod(hand);
        if (rod == null) rod = pl.items().rod(p.getInventory().getItemInOffHand());
        if (rod == null) rod = new FishItems.Rod(RodTier.ZWYKLA, 0, 0, 0, 0, 0, 0);

        Location loc = hook.getLocation();
        Set<Species.Where> where = where(loc);
        boolean day = isDay(loc.getWorld());

        Bait bait = pl.items().activeBait(p, d);
        if (bait != null && ThreadLocalRandom.current().nextDouble() >= d.perk(Perk.OSZCZEDNY) * 0.12)
            pl.items().consumeBait(p, bait);

        Hotspots.Spot spot = pl.hotspots().at(loc);
        double luck = rod.luck() + d.perk(Perk.SZCZESCIARZ) + Math.min(5, d.streak / 10) + d.level / 10.0;
        if (bait != null) {
            luck += bait.luck;
            if (bait == Bait.SWIETLIK && (!day || where.contains(Species.Where.CAVE))) luck += 3;
        }
        if (spot != null) { luck += 4; spot.use(); }
        if (pl.frenzy().active()) luck += 3;

        ThreadLocalRandom r = ThreadLocalRandom.current();
        double junk = Math.max(0.02, pl.getConfig().getDouble("szanse.smieci", 0.10) - luck * 0.006);
        double treasure = pl.getConfig().getDouble("szanse.skarb", 0.035) + d.perk(Perk.SKARBNIK) * 0.015 + luck * 0.002
                + (bait == null ? 0 : bait.treasure);
        double creature = pl.getConfig().getBoolean("potwory.wlaczone", true)
                ? pl.getConfig().getDouble("szanse.potwor", 0.03) * (bait == null || bait.creatureMult <= 0 ? 1 : bait.creatureMult) : 0;

        double roll = r.nextDouble();
        if (roll < junk) { giveJunk(p, d); return; }
        roll -= junk;
        Plan plan;
        if (roll < treasure) plan = new Plan(Kind.TREASURE, null, Rarity.RZADKA, luck, spot != null, bait, loc);
        else if (roll - treasure < creature) plan = new Plan(Kind.CREATURE, null, Rarity.NIEPOSPOLITA, luck, spot != null, bait, loc);
        else {
            Species s = rollSpecies(luck, where, day, loc.getWorld());
            plan = new Plan(Kind.FISH, s, s.rarity(), luck, spot != null, bait, loc);
        }
        if (spot != null) Msg.send(p, "<a>Ławica!</a> <m>Łowisz w ławicy – większa szansa na rzadkie ryby.");

        if (!pl.getConfig().getBoolean("minigra.wlaczona", true)) {
            finish(p, plan, false);
            return;
        }
        Hol h = new Hol(p, plan, rod, d);
        active.put(p.getUniqueId(), h);
        p.showBossBar(h.bar);
        if (d.hints) p.showTitle(Title.title(Component.empty(),
                Msg.mm("<m>Kucnij lub kliknij <a>LPM</a>, gdy znacznik jest na <ok>zielonym</ok>"),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(1500), Duration.ofMillis(300))));
        Sfx.sound(p, Sound.ENTITY_FISHING_BOBBER_SPLASH, 1f, 0.8f);
    }

    /** Pora dnia w świecie. */
    public static boolean isDay(World w) {
        long t = w.getTime();
        return t < 12300 || t > 23850;
    }

    /** Typy wód w miejscu spławika. */
    public static Set<Species.Where> where(Location loc) {
        Set<Species.Where> s = EnumSet.of(Species.Where.ANY);
        Block b = loc.getBlock();
        String biome = b.getBiome().key().value();
        if (biome.contains("ocean")) s.add(Species.Where.OCEAN);
        if (biome.contains("river")) s.add(Species.Where.RIVER);
        if (biome.contains("swamp") || biome.contains("mangrove")) s.add(Species.Where.SWAMP);
        if (biome.contains("frozen") || biome.contains("snowy") || biome.contains("cold") || biome.contains("ice") || biome.contains("grove"))
            s.add(Species.Where.COLD);
        if (biome.contains("warm") || biome.contains("jungle") || biome.contains("desert") || biome.contains("savanna")
                || biome.contains("badlands") || biome.contains("mangrove")) s.add(Species.Where.WARM);
        if (loc.getBlockY() < 45 && b.getLightFromSky() < 8) s.add(Species.Where.CAVE);
        return s;
    }

    public static boolean whenOk(Species.When when, World w) {
        return switch (when) {
            case ANY -> true;
            case DAY -> isDay(w);
            case NIGHT -> !isDay(w);
            case RAIN -> w.hasStorm();
            case STORM -> w.isThundering();
        };
    }

    /** Wagi rzadkości przy danym szczęściu. */
    public static double[] rarityWeights(double luck) {
        double[] weights = new double[Rarity.values().length];
        for (Rarity ra : Rarity.values()) {
            int i = ra.ordinal();
            weights[i] = ra.weight * (i == 0 ? 1 / (1 + luck * 0.03) : 1 + luck * 0.07 * i);
        }
        return weights;
    }

    /** Szczęście bez przynęty i ławicy – do prognozy. */
    public double baseLuck(PlayerData d, FishItems.Rod rod) {
        double l = (rod == null ? 0 : rod.luck()) + d.perk(Perk.SZCZESCIARZ) + Math.min(5, d.streak / 10) + d.level / 10.0;
        if (pl.frenzy().active()) l += 3;
        return l;
    }

    public Species rollSpecies(double luck, Set<Species.Where> where, boolean day, World w) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double[] weights = rarityWeights(luck);
        double sum = 0;
        for (double v : weights) sum += v;
        double x = r.nextDouble() * sum;
        int pick = 0;
        for (int i = 0; i < weights.length; i++) {
            x -= weights[i];
            if (x <= 0) { pick = i; break; }
        }
        for (int i = pick; i >= 0; i--) {
            List<Species> ok = new ArrayList<>();
            for (Species s : pl.fishes().of(Rarity.values()[i]))
                if (where.contains(s.where()) && whenOk(s.when(), w)) ok.add(s);
            if (!ok.isEmpty()) {
                // gatunki lokalne (nie "wszędzie") biorą dwa razy chętniej
                List<Species> weighted = new ArrayList<>(ok);
                for (Species s : ok) if (s.where() != Species.Where.ANY || s.when() != Species.When.ANY) weighted.add(s);
                return weighted.get(r.nextInt(weighted.size()));
            }
        }
        return pl.fishes().get("karp");
    }

    // ───────────── minigra ─────────────

    public void tick() {
        if (active.isEmpty()) return;
        for (Hol h : new ArrayList<>(active.values())) {
            Player p = h.p;
            if (!p.isOnline() || p.isDead()) { stop(h, false, true); continue; }
            h.age++;
            h.idle++;
            if (h.cooldown > 0) h.cooldown--;
            h.cursor += h.dir * h.speed;
            if (h.cursor >= h.len - 1) { h.cursor = h.len - 1; h.dir = -1; tickSound(h); }
            if (h.cursor <= 0) { h.cursor = 0; h.dir = 1; tickSound(h); }
            // epickie i wyżej szarpią – strefa ucieka
            if (h.plan.diff().ordinal() >= Rarity.EPICKA.ordinal() && h.age % (40 - h.plan.diff().ordinal() * 4) == 0) {
                int shift = ThreadLocalRandom.current().nextBoolean() ? 2 : -2;
                h.zoneStart = Math.max(0, Math.min(h.len - h.zoneW, h.zoneStart + shift));
            }
            if (h.idle > pl.getConfig().getInt("minigra.limit-bez-trafienia", 140)) {
                Msg.send(p, "<err>Ryba odpłynęła…</err> <m>Trzeba było zacinać!");
                stop(h, false, false);
                continue;
            }
            p.sendActionBar(Msg.mm(h.render()));
        }
    }

    private void tickSound(Hol h) {
        if (pl.store().get(h.p).sounds) Sfx.sound(h.p, Sound.BLOCK_NOTE_BLOCK_HAT, 0.25f, 1.8f);
    }

    /** Zacięcie – kucnięcie lub LPM. */
    public boolean strike(Player p) {
        Hol h = active.get(p.getUniqueId());
        if (h == null) return false;
        if (h.cooldown > 0) return true;
        h.cooldown = 5;
        int pos = (int) Math.round(h.cursor);
        if (pos >= h.zoneStart && pos < h.zoneStart + h.zoneW) {
            h.hits++;
            h.idle = 0;
            boolean perfect = Math.abs(pos - h.center()) <= 0;
            if (perfect) {
                h.perfectHits++;
                Sfx.sound(p, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.6f);
            }
            Sfx.sound(p, Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 0.8f + h.hits * 0.15f);
            Sfx.sound(p, Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 0.6f, 1.2f);
            h.bar.progress(Math.min(1f, (float) h.hits / h.need));
            if (h.hits >= h.need) { stop(h, true, false); return true; }
            h.moveZone();
            h.speed *= 1.06;
        } else {
            h.misses++;
            h.anyMiss = true;
            Sfx.sound(p, Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
            if (h.misses > h.maxMisses) {
                Sfx.sound(p, Sound.ENTITY_ITEM_BREAK, 0.8f, 1.2f);
                String what = h.plan.kind() == Kind.FISH ? "To była ryba " + h.plan.diff().tag() + "<m>." : "";
                Msg.send(p, "<err>Żyłka pękła!</err> <m>" + what);
                stop(h, false, false);
            }
        }
        return true;
    }

    private void stop(Hol h, boolean success, boolean silent) {
        active.remove(h.p.getUniqueId());
        h.p.hideBossBar(h.bar);
        if (!h.p.isOnline()) return;
        h.p.sendActionBar(Component.empty());
        if (success) {
            finish(h.p, h.plan, !h.anyMiss);
        } else if (!silent) {
            PlayerData d = pl.store().get(h.p);
            d.escaped++;
            d.streak = 0;
            d.dirty();
        }
    }

    public void cancelAll() {
        for (Hol h : new ArrayList<>(active.values())) stop(h, false, true);
    }

    public void cancel(Player p) {
        Hol h = active.get(p.getUniqueId());
        if (h != null) stop(h, false, true);
    }

    // ───────────── wyniki ─────────────

    private void finish(Player p, Plan plan, boolean perfect) {
        PlayerData d = pl.store().get(p);
        d.streak++;
        d.bestStreak = Math.max(d.bestStreak, d.streak);
        if (perfect) {
            d.perfect++;
            pl.quests().progress(p, Quests.Type.PERFECT, null, 1);
            if (d.streak > 1 && d.streak % 10 == 0)
                Msg.send(p, "<a>Seria " + d.streak + "!</a> <m>Szczęście <a>+" + Math.min(5, d.streak / 10) + "</a> do końca serii.");
        }
        pl.quests().progress(p, Quests.Type.STREAK, null, d.streak);
        d.dirty();
        switch (plan.kind()) {
            case FISH -> catchFish(p, d, plan, perfect);
            case TREASURE -> treasure(p, d, plan);
            case CREATURE -> pl.creatures().spawn(p, plan.hook());
        }
    }

    private void catchFish(Player p, PlayerData d, Plan plan, boolean perfect) {
        FishEntry e = roll(plan.species(), plan.luck(), perfect, p.getName());
        int count = 1;
        double dbl = d.perk(Perk.PODWOJNY) * 0.03 + (plan.bait() == null ? 0 : plan.bait().doubleCatch) + (plan.hotspot() ? 0.10 : 0);
        FishEntry second = ThreadLocalRandom.current().nextDouble() < dbl ? roll(plan.species(), plan.luck(), false, p.getName()) : null;

        register(p, d, e, perfect);
        if (second != null) {
            register(p, d, second, false);
            count = 2;
        }

        // wiadomość
        Species s = plan.species();
        Component msg = Msg.mm(Msg.PREFIX + (perfect ? "<a>Perfekcyjnie!</a> " : "") + "Złowiono ")
                .append(Msg.mm(s.display()).hoverEvent(pl.items().fish(e).asHoverEvent()))
                .append(Msg.mm(" " + e.starsTag() + " <dark>·</dark> <m>" + Msg.dec(e.cm()) + " cm · " + FishItems.kg(e.kg())
                        + (count == 2 ? " <dark>·</dark> <a>podwójny połów!</a>" : "")));
        org.bukkit.command.CommandSender cs = p;
        cs.sendMessage(msg);
        celebrate(p, s.rarity(), plan.hook());

        Rarity annMin = Rarity.byName(pl.getConfig().getString("ogloszenia.od-rzadkosci", "LEGENDARNA"));
        if (annMin == null) annMin = Rarity.LEGENDARNA;
        if (s.rarity().ordinal() >= annMin.ordinal()) {
            Component b = Msg.mm(Msg.PREFIX + "<a>" + Msg.esc(p.getName()) + "</a> złowił ")
                    .append(Msg.mm(s.display()).hoverEvent(pl.items().fish(e).asHoverEvent()))
                    .append(Msg.mm(" " + e.starsTag() + " <m>(" + FishItems.kg(e.kg()) + ")"));
            Bukkit.getServer().sendMessage(b);
        }
    }

    /** Losuje rozmiar ryby. */
    public FishEntry roll(Species s, double luck, boolean perfect, String catcher) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double t = Math.pow(r.nextDouble(), 1.6 / (1 + Math.max(0, luck) * 0.03));
        double cm = s.minCm() + (s.maxCm() - s.minCm()) * t;
        double kg = s.minKg() + (s.maxKg() - s.minKg()) * Math.pow(t, 1.3) * (0.92 + r.nextDouble() * 0.16);
        kg = Math.max(s.minKg(), Math.min(s.maxKg(), kg));
        int stars = t < 0.3 ? 1 : t < 0.55 ? 2 : t < 0.75 ? 3 : t < 0.92 ? 4 : 5;
        if (perfect) stars = Math.min(5, stars + 1);
        return new FishEntry(s.id(), Math.round(cm * 10) / 10.0, kg, stars, catcher, System.currentTimeMillis());
    }

    /** Zapis złowionej ryby: statystyki, rekordy, XP, zadania, zawody, siatka. */
    private void register(Player p, PlayerData d, FishEntry e, boolean perfect) {
        Species s = pl.fishes().get(e.species());
        boolean isNew = !d.species.containsKey(s.id());
        PlayerData.SpStat st = d.sp(s.id());
        boolean personal = !isNew && e.kg() > st.bestKg;
        st.count++;
        if (e.kg() > st.bestKg) { st.bestKg = e.kg(); st.bestCm = e.cm(); }
        st.bestStars = Math.max(st.bestStars, e.stars());
        d.caught++;
        d.totalKg += e.kg();
        if (e.kg() > d.heaviestKg) { d.heaviestKg = e.kg(); d.heaviestSp = s.id(); }
        d.dirty();

        if (isNew) {
            Msg.send(p, "<a>Nowy gatunek w atlasie!</a> <t>" + s.display() + " <dark>· <m>" + d.discovered() + "/" + pl.fishes().size());
            Sfx.sound(p, Sound.UI_TOAST_IN, 1f, 1.2f);
            atlasMilestones(p, d);
        } else if (personal) {
            Msg.send(p, "<ok>Rekord osobisty!</ok> <m>" + s.name() + " – <t>" + FishItems.kg(e.kg()));
        }

        Store.Record rec = pl.store().records.get(s.id());
        if (rec == null || e.kg() > rec.kg()) {
            pl.store().records.put(s.id(), new Store.Record(p.getName(), e.kg(), e.cm()));
            pl.store().dirty();
            if (rec != null && s.rarity().ordinal() >= Rarity.RZADKA.ordinal())
                Msg.broadcast("<a>Rekord serwera!</a> <t>" + Msg.esc(p.getName()) + " <m>złowił</m> " + s.display()
                        + " <m>ważącą</m> <a>" + FishItems.kg(e.kg()) + "</a> <dark>(poprzedni: " + Msg.esc(rec.holder()) + ")");
            else if (rec == null) Msg.send(p, "<m>Ustanowiłeś pierwszy rekord serwera dla tego gatunku.");
        }

        double xp = s.rarity().xp * (1 + (e.stars() - 1) * 0.15) * (perfect ? 1.5 : 1)
                * pl.getConfig().getDouble("poziomy.mnoznik-xp", 1.0);
        addXp(p, d, Math.max(1, Math.round(xp)));

        pl.quests().progress(p, Quests.Type.CATCH, null, 1);
        pl.quests().progress(p, Quests.Type.RARITY, String.valueOf(s.rarity().ordinal()), 1);
        pl.quests().progress(p, Quests.Type.SPECIES, s.id(), 1);
        pl.quests().progress(p, Quests.Type.WEIGHT, null, (int) Math.round(e.kg()));
        pl.tournament().onCatch(p, e, s);

        store(p, d, e);
    }

    /** Do siatki albo do ekwipunku. */
    public void store(Player p, PlayerData d, FishEntry e) {
        if (d.toBag && d.bag.size() < d.bagCap()) {
            d.bag.add(e);
            d.dirty();
            if (d.bag.size() == d.bagCap())
                Msg.send(p, "<err>Siatka jest pełna!</err> <m>Kolejne ryby trafią do ekwipunku. " + Msg.button("Sprzedaj", "/ryby sprzedaj", "Sprzedaj wszystkie ryby"));
        } else if (!d.toBag && p.getInventory().firstEmpty() < 0 && d.bag.size() < d.bagCap()) {
            d.bag.add(e);
            d.dirty();
            p.sendActionBar(Msg.mm("<m>Ekwipunek pełny – ryba trafiła do <a>siatki</a>."));
        } else {
            Quests.giveOrDrop(p, pl.items().fish(e));
        }
    }

    private void celebrate(Player p, Rarity r, Location at) {
        World w = p.getWorld();
        Location l = p.getLocation().add(0, 1, 0);
        switch (r) {
            case POSPOLITA, NIEPOSPOLITA -> Sfx.sound(p, Sound.ENTITY_PLAYER_SPLASH, 0.5f, 1.4f);
            case RZADKA -> {
                Sfx.sound(p, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.6f);
                w.spawnParticle(Particle.DUST, l, 25, 0.5, 0.6, 0.5, new Particle.DustOptions(Color.fromRGB(0x58a6ff), 1.2f));
            }
            case EPICKA -> {
                Sfx.sound(p, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
                Sfx.sound(p, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1f);
                w.spawnParticle(Particle.DUST, l, 40, 0.6, 0.8, 0.6, new Particle.DustOptions(Color.fromRGB(0xc77dff), 1.4f));
                w.spawnParticle(Particle.END_ROD, l, 15, 0.4, 0.6, 0.4, 0.02);
            }
            case LEGENDARNA, MITYCZNA -> {
                Sfx.sound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, r == Rarity.MITYCZNA ? 0.8f : 1.1f);
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, l, r == Rarity.MITYCZNA ? 120 : 60, 0.6, 1, 0.6, 0.3);
                p.showTitle(Title.title(Msg.mm(r.c() + "<b>" + r.label.toUpperCase() + "!</b>"),
                        Msg.mm("<m>Ryba życia na haczyku"),
                        Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2200), Duration.ofMillis(600))));
                if (r == Rarity.MITYCZNA && at != null) {
                    Firework fw = w.spawn(at.clone().add(0, 1, 0), Firework.class, f -> {
                        FireworkMeta fm = f.getFireworkMeta();
                        fm.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BALL_LARGE)
                                .withColor(Color.fromRGB(0xff5e7e), Color.fromRGB(0x5ce1e6)).withFade(Color.WHITE).flicker(true).build());
                        f.setFireworkMeta(fm);
                    });
                    Bukkit.getScheduler().runTaskLater(pl, fw::detonate, 2L);
                }
            }
        }
    }

    // ───────────── śmieci i skarby ─────────────

    private static final Material[] JUNK = {Material.LEATHER_BOOTS, Material.STICK, Material.BOWL, Material.ROTTEN_FLESH,
            Material.LILY_PAD, Material.STRING, Material.BONE, Material.GLASS_BOTTLE, Material.KELP, Material.INK_SAC, Material.TRIPWIRE_HOOK};

    private void giveJunk(Player p, PlayerData d) {
        d.junk++;
        d.dirty();
        Material m = JUNK[ThreadLocalRandom.current().nextInt(JUNK.length)];
        if (d.dropJunk) {
            p.sendActionBar(Msg.mm("<m>Wyłowiono śmieć – wyrzucony z powrotem do wody."));
            return;
        }
        Quests.giveOrDrop(p, new ItemStack(m));
        p.sendActionBar(Msg.mm("<m>Eh… tylko śmieć. <dark>(" + m.name().toLowerCase().replace('_', ' ') + ")"));
        Sfx.sound(p, Sound.ENTITY_ITEM_PICKUP, 0.6f, 0.7f);
    }

    private void treasure(Player p, PlayerData d, Plan plan) {
        d.treasures++;
        d.dirty();
        pl.quests().progress(p, Quests.Type.TREASURE, null, 1);
        addXp(p, d, 60);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double bonus = 1 + plan.luck() * 0.05;
        int roll = r.nextInt(100);
        String what;
        if (roll < 22) {
            double v = Math.round((500 + r.nextInt(2500)) * bonus);
            pl.eco().deposit(p, v);
            d.earned += v;
            what = "<money>" + pl.eco().format(v) + "</money> <m>w zardzewiałej szkatułce";
        } else if (roll < 37) {
            int n = 1 + r.nextInt(3);
            Quests.giveOrDrop(p, new ItemStack(Material.DIAMOND, n));
            what = "<#8be9fd>" + n + "× diament";
        } else if (roll < 50) {
            int n = 3 + r.nextInt(8);
            Quests.giveOrDrop(p, new ItemStack(Material.EMERALD, n));
            what = "<ok>" + n + "× szmaragd";
        } else if (roll < 62) {
            Quests.giveOrDrop(p, enchantedBook());
            what = "<myst>zaczarowaną księgę";
        } else if (roll < 72) {
            int n = 3 + r.nextInt(6);
            Quests.giveOrDrop(p, new ItemStack(Material.GOLD_INGOT, n));
            what = "<#ffd84d>" + n + "× sztabka złota";
        } else if (roll < 80) {
            Bait b = r.nextInt(3) == 0 ? Bait.ZLOTA : Bait.MAGICZNA;
            Quests.giveOrDrop(p, pl.items().bait(b, b == Bait.ZLOTA ? 2 : 4));
            what = "<#f0a868>paczkę przynęt (" + b.name + ")";
        } else if (roll < 87) {
            int n = 5 + r.nextInt(11);
            Quests.giveOrDrop(p, new ItemStack(Material.EXPERIENCE_BOTTLE, n));
            what = "<ok>" + n + "× butelka doświadczenia";
        } else if (roll < 93) {
            Quests.giveOrDrop(p, new ItemStack(r.nextBoolean() ? Material.NAME_TAG : Material.SADDLE));
            what = "<t>starą pamiątkę";
        } else if (roll < 98) {
            Quests.giveOrDrop(p, new ItemStack(Material.NAUTILUS_SHELL, 1 + r.nextInt(3)));
            what = "<a>muszle łodzika";
        } else {
            Quests.giveOrDrop(p, new ItemStack(Material.HEART_OF_THE_SEA));
            what = "<a><b>Serce Morza</b>";
            Msg.broadcast("<a>" + Msg.esc(p.getName()) + "</a> wyłowił <a>Serce Morza</a>!");
        }
        Msg.send(p, "<#ffd84d>Skarb!</#ffd84d> <t>Wyłowiono " + what);
        Sfx.sound(p, Sound.BLOCK_CHEST_OPEN, 0.8f, 1.2f);
        Sfx.sound(p, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2f);
        p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5);
    }

    private ItemStack enchantedBook() {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        List<Enchantment> all = new ArrayList<>();
        io.papermc.paper.registry.RegistryAccess.registryAccess()
                .getRegistry(io.papermc.paper.registry.RegistryKey.ENCHANTMENT).forEach(all::add);
        if (all.isEmpty()) return book;
        Enchantment e = all.get(ThreadLocalRandom.current().nextInt(all.size()));
        EnchantmentStorageMeta m = (EnchantmentStorageMeta) book.getItemMeta();
        m.addStoredEnchant(e, 1 + ThreadLocalRandom.current().nextInt(e.getMaxLevel()), true);
        book.setItemMeta(m);
        return book;
    }

    // ───────────── poziomy ─────────────

    public int maxLevel() { return pl.getConfig().getInt("poziomy.max", 50); }

    public long need(int level) { return Math.round(100 + 60 * Math.pow(level, 1.6)); }

    public void addXp(Player p, PlayerData d, long amount) {
        if (d.level >= maxLevel()) return;
        d.xp += amount;
        boolean up = false;
        while (d.level < maxLevel() && d.xp >= need(d.level)) {
            d.xp -= need(d.level);
            d.level++;
            up = true;
            double reward = d.level * pl.getConfig().getDouble("poziomy.nagroda-za-poziom", 250);
            pl.eco().deposit(p, reward);
            Msg.send(p, "<a><b>Awans!</b></a> <t>Poziom wędkarza <a>" + d.level + "</a> <dark>·</dark> <money>+"
                    + pl.eco().format(reward) + "</money> <dark>·</dark> <ok>+1 punkt umiejętności</ok>");
        }
        if (d.level >= maxLevel()) d.xp = 0;
        d.dirty();
        if (up) {
            p.showTitle(Title.title(Msg.mm("<gradient:#36d1dc:#5b86e5><b>POZIOM " + d.level + "</b></gradient>"),
                    Msg.mm("<m>Otwórz <a>/ryby</a> i wydaj punkt umiejętności"),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2000), Duration.ofMillis(500))));
            Sfx.sound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1.5f);
        }
    }

    // ───────────── atlas ─────────────

    private void atlasMilestones(Player p, PlayerData d) {
        int pct = d.discovered() * 100 / pl.fishes().size();
        int[] steps = {25, 50, 75, 100};
        double[] money = {5_000, 20_000, 60_000, 250_000};
        for (int i = 0; i < steps.length; i++) {
            if (pct >= steps[i] && d.atlasRewards.add(steps[i])) {
                pl.eco().deposit(p, money[i]);
                Msg.send(p, "<a>Atlas " + steps[i] + "%!</a> <t>Nagroda: <money>" + pl.eco().format(money[i]) + "</money>");
                if (steps[i] == 100) {
                    Quests.giveOrDrop(p, pl.items().rod(RodTier.TYTANOWA));
                    Msg.broadcast("<a>" + Msg.esc(p.getName()) + "</a> odkrył <a>wszystkie</a> gatunki ryb! Mistrz wędkarstwa!");
                }
                d.dirty();
            }
        }
    }
}
