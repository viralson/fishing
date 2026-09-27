package pl.lowienie;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FishCommand implements TabExecutor {

    private static final List<String> SUBS = List.of("sprzedaj", "siatka", "targ", "atlas", "akwarium", "zadania", "wedkarnia",
            "wedka", "umiejetnosci", "top", "zawody", "prognoza", "ustawienia", "staty", "pomoc");
    private static final List<String> ADMIN = List.of("daj", "szal", "xp", "reload");

    private final LowieniePlugin pl;

    public FishCommand(LowieniePlugin pl) { this.pl = pl; }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        String sub = a.length == 0 ? "" : a[0].toLowerCase(Locale.ROOT);
        boolean admin = s.hasPermission("lowienie.admin");

        // komendy admina działają też z konsoli
        switch (sub) {
            case "daj" -> { if (admin) give(s, a); else noPerm(s); return true; }
            case "szal" -> {
                if (!admin) { noPerm(s); return true; }
                if (a.length > 1 && a[1].equalsIgnoreCase("stop")) pl.frenzy().stop();
                else pl.frenzy().start(a.length > 1 ? parseInt(a[1], 5) : pl.getConfig().getInt("szal.minut", 5));
                return true;
            }
            case "xp" -> {
                if (!admin) { noPerm(s); return true; }
                Player t = a.length > 1 ? Bukkit.getPlayerExact(a[1]) : null;
                if (t == null || a.length < 3) { Msg.err(s, "Użycie: <a>/ryby xp <gracz> <ilość>"); return true; }
                pl.fishing().addXp(t, pl.store().get(t), parseInt(a[2], 0));
                Msg.ok(s, "Dodano XP graczowi <a>" + t.getName());
                return true;
            }
            case "reload" -> {
                if (!admin) { noPerm(s); return true; }
                pl.reloadConfig();
                Msg.ok(s, "Przeładowano konfigurację.");
                return true;
            }
            case "zawody" -> {
                if (a.length > 1 && admin) {
                    if (a[1].equalsIgnoreCase("stop")) { pl.tournament().stop(true); return true; }
                    if (a[1].equalsIgnoreCase("start")) {
                        Tournament.Mode m = Tournament.Mode.PUNKTY;
                        if (a.length > 2) try { m = Tournament.Mode.valueOf(a[2].toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException ignored) {}
                        if (pl.tournament().active()) { Msg.err(s, "Zawody już trwają."); return true; }
                        pl.tournament().start(m, a.length > 3 ? parseInt(a[3], 10) : pl.getConfig().getInt("zawody.minut", 10));
                        return true;
                    }
                }
            }
            default -> {}
        }

        if (!(s instanceof Player p)) { Msg.err(s, "Tylko dla graczy."); return true; }
        switch (sub) {
            case "" -> { Sfx.open(p); pl.gui().main(p); }
            case "sprzedaj" -> {
                boolean all = a.length > 1 && a[1].equalsIgnoreCase("wszystko");
                double[] b = pl.market().sellBag(p, e -> all || cheap(e));
                double[] i = pl.market().sellInventory(p, e -> all || cheap(e));
                int n = (int) (b[0] + i[0]);
                if (n == 0) { Msg.err(p, "Nie masz ryb do sprzedania."); Sfx.deny(p); return true; }
                Sfx.cash(p);
                Msg.ok(p, "Sprzedano <a>" + n + "</a> ryb za <money>" + pl.eco().format(b[1] + i[1]) + "</money>."
                        + (all ? "" : " <m>(bez legendarnych i mitycznych)"));
            }
            case "siatka" -> { Sfx.open(p); pl.fishGui().bag(p, 0); }
            case "targ" -> { Sfx.open(p); pl.fishGui().market(p); }
            case "atlas" -> { Sfx.open(p); pl.fishGui().atlas(p, Rarity.POSPOLITA); }
            case "akwarium" -> { Sfx.open(p); pl.fishGui().aquarium(p); }
            case "zadania" -> { Sfx.open(p); pl.gui().quests(p); }
            case "wedkarnia", "sklep" -> { Sfx.open(p); pl.gui().shop(p); }
            case "wedka" -> { Sfx.open(p); pl.gui().rod(p); }
            case "umiejetnosci" -> { Sfx.open(p); pl.gui().perks(p); }
            case "top", "ranking" -> { Sfx.open(p); pl.gui().ranking(p, 0); }
            case "zawody" -> { Sfx.open(p); pl.gui().tournament(p); }
            case "prognoza" -> { Sfx.open(p); pl.gui().forecast(p); }
            case "ustawienia" -> { Sfx.open(p); pl.gui().settings(p); }
            case "staty" -> {
                Player t = a.length > 1 ? Bukkit.getPlayerExact(a[1]) : p;
                if (t == null) { Msg.err(p, "Gracz musi być online."); return true; }
                Sfx.open(p);
                pl.gui().stats(p, t);
            }
            default -> help(p, admin);
        }
        return true;
    }

    private boolean cheap(FishEntry e) {
        Species s = pl.fishes().get(e.species());
        return s == null || s.rarity().ordinal() < Rarity.LEGENDARNA.ordinal();
    }

    private void help(CommandSender s, boolean admin) {
        s.sendMessage(Msg.mm("<line>"));
        s.sendMessage(Msg.mm(" " + Msg.BRAND + " <m>– komendy"));
        s.sendMessage(Msg.mm(" <a>/ryby</a> <m>przystań (główne menu)"));
        s.sendMessage(Msg.mm(" <a>/ryby sprzedaj [wszystko]</a> <m>szybka sprzedaż"));
        s.sendMessage(Msg.mm(" <a>/ryby siatka · targ · atlas · akwarium</a>"));
        s.sendMessage(Msg.mm(" <a>/ryby zadania · wedkarnia · wedka · umiejetnosci</a>"));
        s.sendMessage(Msg.mm(" <a>/ryby top · zawody · prognoza · ustawienia · staty [gracz]</a>"));
        if (admin) {
            s.sendMessage(Msg.mm(" <err>/ryby daj wedke|przynete|rybe <gracz> <typ> [ilość]"));
            s.sendMessage(Msg.mm(" <err>/ryby zawody start [ilosc|waga|punkty] [min] · stop"));
            s.sendMessage(Msg.mm(" <err>/ryby szal [min|stop] · xp <gracz> <ilość> · reload"));
        }
        s.sendMessage(Msg.mm("<line>"));
    }

    private void give(CommandSender s, String[] a) {
        if (a.length < 4) { Msg.err(s, "Użycie: <a>/ryby daj wedke|przynete|rybe <gracz> <typ> [ilość]"); return; }
        Player t = Bukkit.getPlayerExact(a[2]);
        if (t == null) { Msg.err(s, "Gracz musi być online."); return; }
        switch (a[1].toLowerCase(Locale.ROOT)) {
            case "wedke" -> {
                RodTier r = RodTier.byName(a[3]);
                if (r == null) { Msg.err(s, "Nieznana wędka."); return; }
                Quests.giveOrDrop(t, pl.items().rod(r));
                Msg.ok(s, "Dano " + r.display() + " <t>graczowi <a>" + t.getName());
            }
            case "przynete" -> {
                Bait b = Bait.byName(a[3]);
                if (b == null) { Msg.err(s, "Nieznana przynęta."); return; }
                int n = a.length > 4 ? parseInt(a[4], 16) : 16;
                while (n > 0) { int k = Math.min(64, n); Quests.giveOrDrop(t, pl.items().bait(b, k)); n -= k; }
                Msg.ok(s, "Dano przynęty <a>" + b.name + "</a> graczowi <a>" + t.getName());
            }
            case "rybe" -> {
                Species sp = pl.fishes().get(a[3]);
                if (sp == null) { Msg.err(s, "Nieznany gatunek."); return; }
                FishEntry e = pl.fishing().roll(sp, 0, false, t.getName());
                if (a.length > 4) e = new FishEntry(e.species(), e.cm(), e.kg(), Math.max(1, Math.min(5, parseInt(a[4], 1))), e.catcher(), e.time());
                Quests.giveOrDrop(t, pl.items().fish(e));
                Msg.ok(s, "Dano " + sp.display() + " <t>graczowi <a>" + t.getName());
            }
            default -> Msg.err(s, "Wybierz: wedke, przynete, rybe.");
        }
    }

    private void noPerm(CommandSender s) { Msg.err(s, "Brak uprawnień."); }

    private static int parseInt(String s, int def) {
        double v = Msg.parseAmount(s);
        return v < 0 ? def : (int) Math.min(Integer.MAX_VALUE, v);
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        List<String> out = new ArrayList<>();
        boolean admin = s.hasPermission("lowienie.admin");
        if (a.length == 1) {
            out.addAll(SUBS);
            if (admin) out.addAll(ADMIN);
        } else if (a.length == 2) {
            switch (a[0].toLowerCase(Locale.ROOT)) {
                case "sprzedaj" -> out.add("wszystko");
                case "daj" -> { if (admin) out.addAll(List.of("wedke", "przynete", "rybe")); }
                case "zawody" -> { if (admin) out.addAll(List.of("start", "stop")); }
                case "szal" -> { if (admin) out.addAll(List.of("5", "10", "stop")); }
                case "staty", "xp" -> Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
                default -> {}
            }
        } else if (a.length == 3 && admin) {
            if (a[0].equalsIgnoreCase("daj")) Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
            if (a[0].equalsIgnoreCase("zawody") && a[1].equalsIgnoreCase("start"))
                for (Tournament.Mode m : Tournament.Mode.values()) out.add(m.name().toLowerCase(Locale.ROOT));
        } else if (a.length == 4 && admin && a[0].equalsIgnoreCase("daj")) {
            switch (a[1].toLowerCase(Locale.ROOT)) {
                case "wedke" -> { for (RodTier r : RodTier.values()) out.add(r.name().toLowerCase(Locale.ROOT)); }
                case "przynete" -> { for (Bait b : Bait.values()) out.add(b.name().toLowerCase(Locale.ROOT)); }
                case "rybe" -> pl.fishes().all().forEach(sp -> out.add(sp.id()));
                default -> {}
            }
        }
        String last = a[a.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(x -> !x.toLowerCase(Locale.ROOT).startsWith(last));
        return out;
    }
}
