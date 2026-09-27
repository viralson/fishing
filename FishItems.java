package pl.lowienie;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Przedmioty: ryby, przynęty i wędki (dane w PDC). */
public final class FishItems {

    /** Ulepszane cechy wędki. */
    public enum Stat {
        LUCK("Szczęście", "up_luck", Material.RABBIT_FOOT, "Częściej rzadkie ryby i skarby"),
        SPEED("Szybkość", "up_speed", Material.SUGAR, "Ryby biorą szybciej"),
        CONTROL("Kontrola", "up_control", Material.LEAD, "Szersza strefa zacięcia w holu"),
        LINE("Żyłka", "up_line", Material.STRING, "Więcej pomyłek zanim ryba ucieknie");

        public final String name, key, desc;
        public final Material icon;

        Stat(String name, String key, Material icon, String desc) {
            this.name = name;
            this.key = key;
            this.icon = icon;
            this.desc = desc;
        }
    }

    /** Statystyki wędki w ręce. */
    public record Rod(RodTier tier, int upLuck, int upSpeed, int upControl, int upLine, int lotsLevel, int lureLevel) {
        public int luck() { return tier.luck + upLuck + lotsLevel; }
        public int speed() { return tier.speed + upSpeed + lureLevel; }
        public int control() { return tier.control + upControl; }
        public int line() { return tier.line + upLine; }
        public int up(Stat s) {
            return switch (s) { case LUCK -> upLuck; case SPEED -> upSpeed; case CONTROL -> upControl; case LINE -> upLine; };
        }
        public int max(Stat s) { return s == Stat.LINE ? Math.max(1, tier.maxLevel / 3) : tier.maxLevel; }
    }

    private final LowieniePlugin pl;
    private final NamespacedKey fishKey, baitKey, tierKey;

    public FishItems(LowieniePlugin pl) {
        this.pl = pl;
        this.fishKey = new NamespacedKey(pl, "fish");
        this.baitKey = new NamespacedKey(pl, "bait");
        this.tierKey = new NamespacedKey(pl, "rod_tier");
    }

    // ───────────── ryby ─────────────

    public ItemStack fish(FishEntry e) {
        Species s = pl.fishes().get(e.species());
        if (s == null) return new ItemStack(Material.COD);
        ItemStack it = new ItemStack(s.mat());
        ItemMeta m = it.getItemMeta();
        m.displayName(Msg.item(s.display()));
        m.lore(fishLore(s, e, null).stream().map(Msg::item).toList());
        if (s.rarity().ordinal() >= Rarity.LEGENDARNA.ordinal()) m.setEnchantmentGlintOverride(true);
        m.getPersistentDataContainer().set(fishKey, PersistentDataType.STRING, e.serialize());
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        it.setItemMeta(m);
        return it;
    }

    /** Opis ryby – wspólny dla przedmiotu i ikon w menu. */
    public List<String> fishLore(Species s, FishEntry e, String extra) {
        List<String> l = new ArrayList<>();
        l.add(s.rarity().tag() + " <dark>·</dark> " + e.starsTag());
        l.add("");
        l.add("<m>Długość  <t>" + Msg.dec(e.cm()) + " cm");
        l.add("<m>Waga     <t>" + kg(e.kg()));
        l.add("<m>Złowił   <t>" + Msg.esc(e.catcher()) + " <dark>· " + new SimpleDateFormat("dd.MM.yy").format(new Date(e.time())));
        if (extra != null) {
            l.add("");
            l.addAll(List.of(extra.split("\n")));
        }
        return l;
    }

    public static String kg(double kg) {
        return kg < 1 ? Math.round(kg * 1000) + " g" : Msg.dec(kg) + " kg";
    }

    public FishEntry entry(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return null;
        String s = it.getItemMeta().getPersistentDataContainer().get(fishKey, PersistentDataType.STRING);
        return s == null ? null : FishEntry.parse(s);
    }

    // ───────────── przynęty ─────────────

    public ItemStack bait(Bait b, int amount) {
        ItemStack it = new ItemStack(b.mat, Math.max(1, Math.min(64, amount)));
        ItemMeta m = it.getItemMeta();
        m.displayName(Msg.item("<#f0a868>" + b.name));
        m.lore(List.of(Msg.item("<m>Przynęta wędkarska"), Msg.item(""), Msg.item("<t>" + b.desc),
                Msg.item(""), Msg.item("<m>Zużywa się przy każdym braniu.")));
        m.getPersistentDataContainer().set(baitKey, PersistentDataType.STRING, b.name());
        it.setItemMeta(m);
        return it;
    }

    public Bait bait(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return null;
        return Bait.byName(it.getItemMeta().getPersistentDataContainer().get(baitKey, PersistentDataType.STRING));
    }

    public int countBait(Player p, Bait b) {
        int n = 0;
        for (ItemStack it : p.getInventory().getContents()) if (bait(it) == b) n += it.getAmount();
        return n;
    }

    /** Wybrana przynęta, a gdy jej brak – pierwsza lepsza z ekwipunku. */
    public Bait activeBait(Player p, PlayerData d) {
        if (d.selected != null && countBait(p, d.selected) > 0) return d.selected;
        for (ItemStack it : p.getInventory().getContents()) {
            Bait b = bait(it);
            if (b != null) return b;
        }
        return null;
    }

    public void consumeBait(Player p, Bait b) {
        for (ItemStack it : p.getInventory().getContents()) {
            if (bait(it) == b) {
                it.setAmount(it.getAmount() - 1);
                return;
            }
        }
    }

    // ───────────── wędki ─────────────

    public ItemStack rod(RodTier t) {
        ItemStack it = new ItemStack(Material.FISHING_ROD);
        ItemMeta m = it.getItemMeta();
        PersistentDataContainer pdc = m.getPersistentDataContainer();
        pdc.set(tierKey, PersistentDataType.STRING, t.name());
        if (t.ordinal() >= RodTier.KARBONOWA.ordinal()) m.setUnbreakable(true);
        it.setItemMeta(m);
        refreshRod(it);
        return it;
    }

    public boolean isRod(ItemStack it) { return it != null && it.getType() == Material.FISHING_ROD; }

    public Rod rod(ItemStack it) {
        if (!isRod(it)) return null;
        ItemMeta m = it.getItemMeta();
        PersistentDataContainer pdc = m.getPersistentDataContainer();
        RodTier t = RodTier.byName(pdc.get(tierKey, PersistentDataType.STRING));
        if (t == null) t = RodTier.ZWYKLA;
        return new Rod(t, up(pdc, Stat.LUCK), up(pdc, Stat.SPEED), up(pdc, Stat.CONTROL), up(pdc, Stat.LINE),
                m.getEnchantLevel(Enchantment.LUCK_OF_THE_SEA), m.getEnchantLevel(Enchantment.LURE));
    }

    private int up(PersistentDataContainer pdc, Stat s) {
        Integer v = pdc.get(new NamespacedKey(pl, s.key), PersistentDataType.INTEGER);
        return v == null ? 0 : v;
    }

    public void setUpgrade(ItemStack it, Stat s, int level) {
        ItemMeta m = it.getItemMeta();
        PersistentDataContainer pdc = m.getPersistentDataContainer();
        if (!pdc.has(tierKey)) pdc.set(tierKey, PersistentDataType.STRING, RodTier.ZWYKLA.name());
        pdc.set(new NamespacedKey(pl, s.key), PersistentDataType.INTEGER, level);
        it.setItemMeta(m);
        refreshRod(it);
    }

    /** Odświeża nazwę i opis wędki. */
    public void refreshRod(ItemStack it) {
        Rod r = rod(it);
        if (r == null) return;
        ItemMeta m = it.getItemMeta();
        m.displayName(Msg.item(r.tier().display()));
        List<String> l = new ArrayList<>();
        l.add("<m>Wędka wędkarska");
        l.add("");
        for (Stat s : Stat.values()) {
            int base = switch (s) { case LUCK -> r.luck(); case SPEED -> r.speed(); case CONTROL -> r.control(); case LINE -> r.line(); };
            l.add("<m>" + s.name + " <t>" + base + (r.up(s) > 0 ? " <dark>(</dark><a>+" + r.up(s) + "</a><dark>)</dark>" : ""));
        }
        l.add("");
        l.add("<m>Ulepszenia <t>" + (r.upLuck() + r.upSpeed() + r.upControl() + r.upLine()) + "<dark>/</dark>" +
                (r.tier().maxLevel * 3 + r.max(Stat.LINE)));
        m.lore(l.stream().map(Msg::item).toList());
        if (r.tier().ordinal() >= RodTier.TYTANOWA.ordinal()) m.setEnchantmentGlintOverride(true);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
        it.setItemMeta(m);
    }
}
