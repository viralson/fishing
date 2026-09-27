package pl.lowienie;

import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class FishListener implements Listener {

    private final LowieniePlugin pl;

    public FishListener(LowieniePlugin pl) { this.pl = pl; }

    private boolean disabledWorld(Player p) {
        return pl.getConfig().getStringList("wylaczone-swiaty").contains(p.getWorld().getName());
    }

    // ───────────── łowienie ─────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFish(PlayerFishEvent e) {
        Player p = e.getPlayer();
        if (disabledWorld(p)) return;
        switch (e.getState()) {
            case FISHING -> {
                if (pl.fishing().inHol(p)) { e.setCancelled(true); return; }
                pl.fishing().onCast(p, e.getHook());
            }
            case CAUGHT_FISH -> {
                if (e.getCaught() instanceof Item item) item.remove();
                e.setExpToDrop(Math.min(e.getExpToDrop(), 3));
                pl.fishing().onBite(p, e.getHook());
            }
            default -> {}
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        if (e.isSneaking()) pl.fishing().strike(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onClick(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Action a = e.getAction();
        if (a != Action.LEFT_CLICK_AIR && a != Action.LEFT_CLICK_BLOCK) return;
        if (pl.fishing().strike(e.getPlayer())) e.setCancelled(true);
    }

    // ───────────── jedzenie ryb ─────────────

    @EventHandler(ignoreCancelled = true)
    public void onEat(PlayerItemConsumeEvent e) {
        FishEntry f = pl.items().entry(e.getItem());
        if (f == null) return;
        Species s = pl.fishes().get(f.species());
        if (s == null) return;
        Player p = e.getPlayer();
        int bonus = f.stars() >= 5 ? 1 : 0;
        switch (s.rarity()) {
            case POSPOLITA -> add(p, PotionEffectType.SATURATION, 2, 0);
            case NIEPOSPOLITA -> add(p, PotionEffectType.REGENERATION, 5, bonus);
            case RZADKA -> { add(p, PotionEffectType.REGENERATION, 8, bonus); add(p, PotionEffectType.SPEED, 30, 0); }
            case EPICKA -> { add(p, PotionEffectType.REGENERATION, 10, 1); add(p, PotionEffectType.STRENGTH, 60, bonus); }
            case LEGENDARNA -> {
                add(p, PotionEffectType.ABSORPTION, 120, 1 + bonus);
                add(p, PotionEffectType.REGENERATION, 15, 1);
                add(p, PotionEffectType.LUCK, 300, 0);
            }
            case MITYCZNA -> {
                add(p, PotionEffectType.RESISTANCE, 120, 0);
                add(p, PotionEffectType.ABSORPTION, 180, 3);
                add(p, PotionEffectType.REGENERATION, 20, 2);
                add(p, PotionEffectType.WATER_BREATHING, 600, 0);
                add(p, PotionEffectType.DOLPHINS_GRACE, 300, 0);
            }
        }
        if (s.id().equals("zlota_rybka")) add(p, PotionEffectType.LUCK, 600, 1);
        p.sendActionBar(Msg.mm("<m>Smacznego! " + s.display() + " <m>dodaje sił."));
    }

    private static void add(Player p, PotionEffectType t, int seconds, int amp) {
        p.addPotionEffect(new PotionEffect(t, seconds * 20, amp, true, true, true));
    }

    /** Ryby i przynęty nie idą do craftingu (np. złoty karp jako sztabka). */
    @EventHandler
    public void onCraft(PrepareItemCraftEvent e) {
        for (ItemStack it : e.getInventory().getMatrix()) {
            if (pl.items().entry(it) != null || pl.items().bait(it) != null) {
                e.getInventory().setResult(null);
                return;
            }
        }
    }

    // ───────────── potwory ─────────────

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        LivingEntity le = e.getEntity();
        if (pl.creatures().onDeath(le, e.getDrops())) e.setDroppedExp(10);
    }

    // ───────────── menu ─────────────

    @EventHandler
    public void onInvClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder(false) instanceof Menu menu)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() == null) return;
        var type = e.getClick();
        if (e.getClickedInventory() == e.getView().getTopInventory()) {
            Menu.Action a = menu.action(e.getRawSlot());
            if (a == null) return;
            pl.getServer().getScheduler().runTask(pl, () -> { if (menu.isViewing(p)) a.click(p, type); });
        } else if (menu.bottom() != null) {
            int slot = e.getSlot();
            pl.getServer().getScheduler().runTask(pl, () -> { if (menu.isViewing(p)) menu.bottom().click(p, slot, type); });
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Menu) e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder(false) instanceof Menu m && m.onClose() != null && e.getPlayer() instanceof Player p)
            m.onClose().accept(p);
    }

    // ───────────── wejście / wyjście ─────────────

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        pl.store().get(p).name = p.getName();
        pl.frenzy().show(p);
        pl.tournament().show(p);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        pl.fishing().cancel(e.getPlayer());
        pl.fishing().lastCasts().remove(e.getPlayer().getUniqueId());
        pl.store().unload(e.getPlayer().getUniqueId());
    }
}
