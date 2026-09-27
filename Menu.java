package pl.lowienie;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Menu GUI z przypisanymi akcjami do slotów. */
public final class Menu implements InventoryHolder {

    @FunctionalInterface
    public interface Action {
        void click(Player p, ClickType type);
    }

    private final Inventory inv;
    private final Map<Integer, Action> actions = new HashMap<>();
    /** Klik w ekwipunek gracza (dolny) przy otwartym menu. */
    @FunctionalInterface
    public interface BottomAction {
        void click(Player p, int slot, ClickType type);
    }

    private Consumer<Player> onClose;
    private BottomAction bottom;
    private Runnable ticker;

    public Menu(int rows, String title) {
        this.inv = Bukkit.createInventory(this, rows * 9, Msg.mm(title));
    }

    @Override
    public Inventory getInventory() { return inv; }

    public int size() { return inv.getSize(); }

    public void set(int slot, ItemStack item, Action action) {
        if (slot < 0 || slot >= inv.getSize()) return;
        inv.setItem(slot, item);
        if (action == null) actions.remove(slot); else actions.put(slot, action);
    }

    public void set(int slot, ItemStack item) { set(slot, item, null); }

    public Action action(int slot) { return actions.get(slot); }

    public void clear() {
        inv.clear();
        actions.clear();
    }

    public void onClose(Consumer<Player> c) { onClose = c; }
    public Consumer<Player> onClose() { return onClose; }

    public void onBottom(BottomAction a) { bottom = a; }
    public BottomAction bottom() { return bottom; }

    /** Animacja odświeżana co kilka ticków, póki menu jest otwarte. */
    public void ticker(Runnable r) { ticker = r; }
    public Runnable ticker() { return ticker; }

    public ItemStack get(int slot) { return inv.getItem(slot); }

    /** Podmienia tylko wygląd slotu, bez zmiany akcji. */
    public void look(int slot, ItemStack item) { if (slot >= 0 && slot < inv.getSize()) inv.setItem(slot, item); }

    public void fill(Material m) {
        ItemStack pane = Items.pane(m);
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType().isAir()) inv.setItem(i, pane);
        }
    }

    public void border(Material a, Material b) {
        int rows = inv.getSize() / 9;
        for (int i = 0; i < inv.getSize(); i++) {
            int r = i / 9, c = i % 9;
            if (r == 0 || r == rows - 1 || c == 0 || c == 8) set(i, Items.pane(i % 2 == 0 ? a : b));
        }
    }

    public void open(Player p) { p.openInventory(inv); }

    public boolean isViewing(Player p) {
        return p.isOnline() && p.getOpenInventory().getTopInventory().getHolder(false) == this;
    }
}
