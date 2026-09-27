package pl.lowienie;

import org.bukkit.OfflinePlayer;

/** Warstwa ekonomii – Vault (jeśli jest) albo wbudowany portfel. */
public interface Eco {

    double balance(OfflinePlayer p);

    boolean withdraw(OfflinePlayer p, double amount);

    void deposit(OfflinePlayer p, double amount);

    String format(double amount);

    String name();

    /** Czy to wbudowany portfel pluginu. */
    default boolean internal() { return false; }
}
