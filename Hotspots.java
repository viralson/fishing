package pl.lowienie;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Ławice – bulgoczące miejsca na wodzie z premią do szczęścia. */
public final class Hotspots {

    public final class Spot {
        final Location loc;
        long until;
        int uses;

        Spot(Location loc, long until) {
            this.loc = loc;
            this.until = until;
        }

        public void use() {
            if (++uses >= pl.getConfig().getInt("lawice.polowow", 4)) until = 0;
        }
    }

    private final LowieniePlugin pl;
    private final List<Spot> spots = new ArrayList<>();

    public Hotspots(LowieniePlugin pl) { this.pl = pl; }

    public int count() { return spots.size(); }

    /** Cząsteczki, co 10 ticków. */
    public void particles() {
        long now = System.currentTimeMillis();
        Iterator<Spot> it = spots.iterator();
        while (it.hasNext()) {
            Spot s = it.next();
            if (now > s.until || !s.loc.isChunkLoaded()) { it.remove(); continue; }
            var w = s.loc.getWorld();
            w.spawnParticle(Particle.FISHING, s.loc, 8, 0.9, 0.05, 0.9, 0.02);
            w.spawnParticle(Particle.BUBBLE_POP, s.loc, 6, 0.8, 0.05, 0.8, 0.01);
            w.spawnParticle(Particle.SPLASH, s.loc, 10, 0.8, 0.1, 0.8, 0.1);
            if (ThreadLocalRandom.current().nextInt(4) == 0) w.spawnParticle(Particle.DOLPHIN, s.loc, 12, 0.7, 0.2, 0.7, 0);
        }
    }

    /** Próba pojawienia się ławic przy aktywnych wędkarzach, co 20 s. */
    public void spawnTick() {
        if (!pl.getConfig().getBoolean("lawice.wlaczone", true)) return;
        double chance = pl.getConfig().getDouble("lawice.szansa", 0.35);
        long life = pl.getConfig().getLong("lawice.czas-sekund", 60) * 1000;
        long now = System.currentTimeMillis();
        for (Player p : pl.getServer().getOnlinePlayers()) {
            Long last = pl.fishing().lastCasts().get(p.getUniqueId());
            if (last == null || now - last > 60_000) continue;
            if (near(p.getLocation(), 24) != null) continue;
            if (ThreadLocalRandom.current().nextDouble() > chance) continue;
            Location l = findWater(p.getLocation());
            if (l == null) continue;
            spots.add(new Spot(l, now + life));
            if (pl.store().get(p).hints)
                p.sendActionBar(Msg.mm("<a>≋ Ławica ryb</a> <t>pojawiła się niedaleko! <m>Zarzuć w bulgoczące miejsce."));
            p.playSound(l, Sound.ENTITY_DOLPHIN_SPLASH, 1f, 1f);
        }
    }

    private Location findWater(Location c) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < 16; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double d = 5 + r.nextDouble() * 9;
            int x = c.getBlockX() + (int) Math.round(Math.cos(a) * d);
            int z = c.getBlockZ() + (int) Math.round(Math.sin(a) * d);
            for (int y = c.getBlockY() + 2; y >= c.getBlockY() - 6; y--) {
                Block b = c.getWorld().getBlockAt(x, y, z);
                if (b.getType() == Material.WATER && b.getRelative(0, 1, 0).getType().isAir())
                    return b.getLocation().add(0.5, 0.95, 0.5);
            }
        }
        return null;
    }

    private Spot near(Location l, double radius) {
        for (Spot s : spots) {
            if (s.loc.getWorld() != l.getWorld()) continue;
            if (s.loc.distanceSquared(l) <= radius * radius) return s;
        }
        return null;
    }

    /** Ławica, w której wylądował spławik. */
    public Spot at(Location hook) {
        long now = System.currentTimeMillis();
        for (Spot s : spots) {
            if (s.until < now || s.loc.getWorld() != hook.getWorld()) continue;
            double dx = s.loc.getX() - hook.getX(), dz = s.loc.getZ() - hook.getZ();
            if (dx * dx + dz * dz <= 2.5 * 2.5 && Math.abs(s.loc.getY() - hook.getY()) < 2.5) return s;
        }
        return null;
    }

    public void clear() { spots.clear(); }
}
