package pl.lowienie;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Potwory z głębin wyciągane na wędkę. */
public final class Creatures {

    public enum Kind {
        UTOPIEC(EntityType.DROWNED, "<#6fcf97>Utopiec", 30, 5, 400, 40, 40, false),
        STRAZNIK(EntityType.GUARDIAN, "<#58a6ff>Strażnik głębin", 40, 6, 900, 80, 25, false),
        WIEDZMA(EntityType.WITCH, "<#c77dff>Bagienna wiedźma", 45, 0, 1400, 110, 15, false),
        KROL(EntityType.DROWNED, "<#ffc857><b>Król Topielców</b>", 160, 10, 7000, 450, 8, true),
        KRAKEN(EntityType.ELDER_GUARDIAN, "<#ff5e7e><b>Kraken</b>", 350, 12, 35000, 1600, 2, true);

        public final EntityType type;
        public final String name;
        public final double hp, dmg, money;
        public final int xp, weight;
        public final boolean boss;

        Kind(EntityType type, String name, double hp, double dmg, double money, int xp, int weight, boolean boss) {
            this.type = type;
            this.name = name;
            this.hp = hp;
            this.dmg = dmg;
            this.money = money;
            this.xp = xp;
            this.weight = weight;
            this.boss = boss;
        }
    }

    private record Active(Kind kind, UUID owner, long born, BossBar bar) {}

    private final LowieniePlugin pl;
    private final NamespacedKey key, ownerKey;
    private final Map<UUID, Active> alive = new HashMap<>();

    public Creatures(LowieniePlugin pl) {
        this.pl = pl;
        this.key = new NamespacedKey(pl, "creature");
        this.ownerKey = new NamespacedKey(pl, "creature_owner");
    }

    private Kind roll() {
        int sum = 0;
        for (Kind k : Kind.values()) sum += k.weight;
        int x = ThreadLocalRandom.current().nextInt(sum);
        for (Kind k : Kind.values()) {
            x -= k.weight;
            if (x < 0) return k;
        }
        return Kind.UTOPIEC;
    }

    public void spawn(Player p, Location at) {
        Kind k = roll();
        Location loc = at.clone().add(0, 0.5, 0);
        Entity ent = loc.getWorld().spawnEntity(loc, k.type);
        if (!(ent instanceof LivingEntity le)) { ent.remove(); return; }
        le.customName(Msg.mm(k.name));
        le.setCustomNameVisible(true);
        le.setRemoveWhenFarAway(false);
        AttributeInstance hp = le.getAttribute(Attribute.MAX_HEALTH);
        if (hp != null) {
            hp.setBaseValue(k.hp);
            le.setHealth(k.hp);
        }
        AttributeInstance dmg = le.getAttribute(Attribute.ATTACK_DAMAGE);
        if (dmg != null && k.dmg > 0) dmg.setBaseValue(k.dmg);
        le.getPersistentDataContainer().set(key, PersistentDataType.STRING, k.name());
        le.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, p.getUniqueId().toString());
        if (k.type == EntityType.DROWNED) {
            EntityEquipment eq = le.getEquipment();
            if (eq != null) {
                eq.setItemInMainHand(new ItemStack(k == Kind.KROL ? Material.TRIDENT : Material.FISHING_ROD));
                if (k == Kind.KROL) eq.setHelmet(new ItemStack(Material.GOLDEN_HELMET));
                eq.setItemInMainHandDropChance(0f);
                eq.setHelmetDropChance(0f);
            }
        }
        Vector v = p.getLocation().toVector().subtract(loc.toVector());
        if (v.lengthSquared() > 0.01) le.setVelocity(v.normalize().multiply(0.9).setY(0.6));
        if (le instanceof Mob mob) mob.setTarget(p);

        BossBar bar = null;
        if (k.boss) {
            bar = BossBar.bossBar(Msg.mm(k.name), 1f, k == Kind.KRAKEN ? BossBar.Color.RED : BossBar.Color.YELLOW, BossBar.Overlay.NOTCHED_12);
            Msg.broadcast("<err>" + Msg.esc(p.getName()) + " wyciągnął z głębin</err> " + k.name + "<err>!</err>");
        }
        alive.put(le.getUniqueId(), new Active(k, p.getUniqueId(), System.currentTimeMillis(), bar));
        Msg.send(p, "<err>Z wody wyłania się</err> " + k.name + "<err>!</err> <m>Pokonaj go, by zdobyć nagrodę.");
        p.playSound(p.getLocation(), k.boss ? Sound.ENTITY_ELDER_GUARDIAN_CURSE : Sound.ENTITY_DROWNED_AMBIENT_WATER, 1f, 0.8f);
    }

    /** Co 10 ticków – paski bossów i sprzątanie. */
    public void tick() {
        Iterator<Map.Entry<UUID, Active>> it = alive.entrySet().iterator();
        long now = System.currentTimeMillis();
        long life = pl.getConfig().getLong("potwory.czas-zycia-sekund", 180) * 1000;
        while (it.hasNext()) {
            var e = it.next();
            Entity ent = Bukkit.getEntity(e.getKey());
            Active a = e.getValue();
            if (!(ent instanceof LivingEntity le) || le.isDead() || !le.isValid()) {
                hideBar(a);
                it.remove();
                continue;
            }
            if (now - a.born() > life) {
                Player owner = Bukkit.getPlayer(a.owner());
                if (owner != null) Msg.send(owner, a.kind().name + " <m>odpłynął w głębiny…");
                le.remove();
                hideBar(a);
                it.remove();
                continue;
            }
            if (a.bar() != null) {
                AttributeInstance hp = le.getAttribute(Attribute.MAX_HEALTH);
                double max = hp == null ? a.kind().hp : hp.getValue();
                a.bar().progress((float) Math.max(0, Math.min(1, le.getHealth() / max)));
                for (Player p : le.getWorld().getPlayers()) {
                    if (p.getLocation().distanceSquared(le.getLocation()) < 48 * 48) p.showBossBar(a.bar());
                    else p.hideBossBar(a.bar());
                }
            }
        }
    }

    private void hideBar(Active a) {
        if (a.bar() == null) return;
        for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(a.bar());
    }

    /** Wywoływane z EntityDeathEvent. Zwraca true, gdy to nasz potwór. */
    public boolean onDeath(LivingEntity le, java.util.List<ItemStack> drops) {
        String k = le.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (k == null) return false;
        Kind kind;
        try { kind = Kind.valueOf(k); } catch (IllegalArgumentException ex) { return false; }
        Active a = alive.remove(le.getUniqueId());
        if (a != null) hideBar(a);
        drops.clear();
        Player killer = le.getKiller();
        if (killer == null) return true;
        PlayerData d = pl.store().get(killer);
        double mult = (1 + d.perk(Perk.LOWCA) * 0.25) * pl.getConfig().getDouble("potwory.mnoznik-nagrod", 1.0);
        double money = Math.round(kind.money * mult);
        pl.eco().deposit(killer, money);
        d.earned += money;
        d.creatures++;
        d.dirty();
        pl.fishing().addXp(killer, d, Math.round(kind.xp * mult));
        pl.quests().progress(killer, Quests.Type.CREATURE, null, 1);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        drops.add(new ItemStack(Material.PRISMARINE_SHARD, 2 + r.nextInt(6)));
        if (r.nextInt(3) == 0) drops.add(pl.items().bait(Bait.KRWISTA, 2));
        if (kind.boss) {
            drops.add(new ItemStack(kind == Kind.KRAKEN ? Material.HEART_OF_THE_SEA : Material.NAUTILUS_SHELL, kind == Kind.KRAKEN ? 1 : 3));
            drops.add(pl.items().bait(Bait.ZLOTA, kind == Kind.KRAKEN ? 4 : 2));
            Msg.broadcast("<a>" + Msg.esc(killer.getName()) + "</a> pokonał " + kind.name + "<t>! <money>+" + pl.eco().format(money));
        } else {
            Msg.send(killer, "Pokonano " + kind.name + " <dark>·</dark> <money>+" + pl.eco().format(money) + "</money> <dark>·</dark> <a>+" + Math.round(kind.xp * mult) + " XP");
        }
        killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.3f);
        return true;
    }

    public void removeAll() {
        for (var e : alive.entrySet()) {
            Entity ent = Bukkit.getEntity(e.getKey());
            if (ent != null) ent.remove();
            hideBar(e.getValue());
        }
        alive.clear();
    }

    public int count() { return alive.size(); }
}
