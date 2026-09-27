package pl.powitanie;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Animowane powitanie na titlach – raz na gracza na czas działania serwera
 * (lista czyści się przy restarcie).
 */
public final class PowitaniePlugin extends JavaPlugin implements Listener, TabExecutor {

    private final Set<UUID> seen = new HashSet<>();
    private final Map<UUID, Intro> running = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        PluginCommand pc = getCommand("powitanie");
        if (pc != null) {
            pc.setExecutor(this);
            pc.setTabCompleter(this);
        }
        // gracze online w chwili (re)loadu już widzieli powitanie
        for (Player p : Bukkit.getOnlinePlayers()) seen.add(p.getUniqueId());
    }

    @Override
    public void onDisable() {
        for (Intro in : new ArrayList<>(running.values())) in.skip();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (getConfig().getBoolean("raz-na-restart", true) && !seen.add(p.getUniqueId())) return;
        long delay = getConfig().getLong("opoznienie-tickow", 30);
        Bukkit.getScheduler().runTaskLater(this, () -> { if (p.isOnline()) play(p); }, delay);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Intro in = running.remove(e.getPlayer().getUniqueId());
        if (in != null && !in.isCancelled()) in.cancel();
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        if (!e.isSneaking() || !getConfig().getBoolean("pomijanie-kucnieciem", false)) return;
        Intro in = running.get(e.getPlayer().getUniqueId());
        if (in != null) in.skip();
    }

    public void play(Player p) {
        Intro old = running.remove(p.getUniqueId());
        if (old != null && !old.isCancelled()) old.cancel();
        Intro in = new Intro(this, p);
        running.put(p.getUniqueId(), in);
        in.start();
    }

    void done(Player p) { running.remove(p.getUniqueId()); }

    // ───────────── komenda ─────────────

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private void msg(CommandSender s, String text) { s.sendMessage(LEGACY.deserialize("&6&lPowitanie &8│ &7" + text)); }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (!s.hasPermission("powitanie.admin")) { msg(s, "&cBrak uprawnień."); return true; }
        String sub = a.length == 0 ? "" : a[0].toLowerCase();
        switch (sub) {
            case "reload" -> { reloadConfig(); msg(s, "&aPrzeładowano config."); }
            case "reset" -> { seen.clear(); msg(s, "&aWyczyszczono listę – każdy zobaczy powitanie przy następnym wejściu."); }
            case "pokaz", "test" -> {
                Player t = a.length > 1 ? Bukkit.getPlayerExact(a[1]) : s instanceof Player p ? p : null;
                if (t == null) { msg(s, "&cPodaj gracza online."); return true; }
                play(t);
                msg(s, "&aOdtwarzam powitanie dla &f" + t.getName());
            }
            default -> {
                msg(s, "&e/powitanie pokaz [gracz] &7– odtwórz animację");
                msg(s, "&e/powitanie reset &7– wszyscy zobaczą ją ponownie");
                msg(s, "&e/powitanie reload &7– przeładuj config");
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (!s.hasPermission("powitanie.admin")) return out;
        if (a.length == 1) out.addAll(List.of("pokaz", "reset", "reload"));
        else if (a.length == 2 && (a[0].equalsIgnoreCase("pokaz") || a[0].equalsIgnoreCase("test")))
            Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        String last = a[a.length - 1].toLowerCase();
        out.removeIf(x -> !x.toLowerCase().startsWith(last));
        return out;
    }
}
