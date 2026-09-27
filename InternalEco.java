package pl.lowienie;

import org.bukkit.OfflinePlayer;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Wbudowany portfel – gdy serwer nie ma Vaulta. */
public final class InternalEco implements Eco {

    private final LowieniePlugin pl;
    final Map<UUID, Double> balances = new ConcurrentHashMap<>();

    public InternalEco(LowieniePlugin pl) { this.pl = pl; }

    private double start() { return pl.getConfig().getDouble("portfel.saldo-poczatkowe", 1000); }

    @Override
    public double balance(OfflinePlayer p) { return balances.computeIfAbsent(p.getUniqueId(), u -> start()); }

    @Override
    public boolean withdraw(OfflinePlayer p, double amount) {
        if (amount <= 0) return true;
        double b = balance(p);
        if (b + 1e-9 < amount) return false;
        balances.put(p.getUniqueId(), b - amount);
        pl.store().dirty();
        return true;
    }

    @Override
    public void deposit(OfflinePlayer p, double amount) {
        if (amount <= 0) return;
        balances.put(p.getUniqueId(), balance(p) + amount);
        pl.store().dirty();
    }

    @Override
    public String format(double amount) {
        String sym = pl.getConfig().getString("portfel.symbol", "$");
        return String.format(Locale.ROOT, "%,.2f", amount).replace(',', ' ') + sym;
    }

    @Override public String name() { return "wbudowany portfel"; }

    @Override public boolean internal() { return true; }
}
