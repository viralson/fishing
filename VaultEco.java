package pl.lowienie;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;

/** Ekonomia serwera przez Vault (EssentialsX, CMI itp.). */
public final class VaultEco implements Eco {

    private final Economy econ;

    private VaultEco(Economy econ) { this.econ = econ; }

    /** Zwraca null, jeśli na serwerze nie ma pluginu ekonomii. */
    public static VaultEco create(LowieniePlugin pl) {
        var rsp = pl.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null || rsp.getProvider() == null) return null;
        return new VaultEco(rsp.getProvider());
    }

    @Override public double balance(OfflinePlayer p) { return econ.getBalance(p); }

    @Override
    public boolean withdraw(OfflinePlayer p, double amount) {
        if (amount <= 0) return true;
        if (!econ.has(p, amount)) return false;
        return econ.withdrawPlayer(p, amount).transactionSuccess();
    }

    @Override
    public void deposit(OfflinePlayer p, double amount) {
        if (amount > 0) econ.depositPlayer(p, amount);
    }

    @Override public String format(double amount) { return econ.format(amount); }

    @Override public String name() { return "Vault (" + econ.getName() + ")"; }
}
