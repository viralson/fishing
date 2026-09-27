package pl.lowienie;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Items {

    private Items() {}

    public static ItemStack of(Material m, String name, String... lore) {
        return of(m, 1, name, Arrays.asList(lore));
    }

    public static ItemStack of(Material m, int amount, String name, List<String> lore) {
        ItemStack it = new ItemStack(m, Math.max(1, Math.min(64, amount)));
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.displayName(Msg.item(name));
            List<net.kyori.adventure.text.Component> l = new ArrayList<>();
            for (String s : lore) if (s != null) l.add(Msg.item(s));
            meta.lore(l);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            it.setItemMeta(meta);
        }
        return it;
    }

    public static ItemStack glow(ItemStack it) {
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setEnchantmentGlintOverride(true);
            it.setItemMeta(meta);
        }
        return it;
    }

    public static ItemStack glowIf(boolean cond, ItemStack it) { return cond ? glow(it) : it; }

    public static ItemStack head(OfflinePlayer owner, String name, String... lore) {
        ItemStack it = of(Material.PLAYER_HEAD, name, lore);
        if (it.getItemMeta() instanceof SkullMeta sm) {
            sm.setOwningPlayer(owner);
            it.setItemMeta(sm);
        }
        return it;
    }

    public static ItemStack pane(Material m) { return of(m, " "); }
}
