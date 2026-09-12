package es.jaie55.boatracing.integrations;

import es.jaie55.boatracing.BoatRacingPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Optional Vault economy bridge. The class is only instantiated when the Vault plugin is
 * present, so servers without Vault never touch the Vault classes.
 */
public class VaultEconomy {

    private final BoatRacingPlugin plugin;
    private Economy economy;

    public VaultEconomy(BoatRacingPlugin plugin) {
        this.plugin = plugin;
        setup();
    }

    public void setup() {
        try {
            RegisteredServiceProvider<Economy> registration = Bukkit.getServicesManager().getRegistration(Economy.class);
            this.economy = registration != null ? registration.getProvider() : null;
        } catch (Throwable throwable) {
            this.economy = null;
        }
        if (economy != null) {
            plugin.getLogger().info("Vault economy detected: " + economy.getName());
        } else {
            plugin.getLogger().info("Vault found but no economy provider is registered; cosmetic shop uses permissions only.");
        }
    }

    public boolean isEnabled() {
        return economy != null;
    }

    public double balance(OfflinePlayer player) {
        if (economy == null || player == null) return 0.0;
        try {
            return economy.getBalance(player);
        } catch (Throwable throwable) {
            return 0.0;
        }
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (economy == null || player == null || amount <= 0) return false;
        try {
            return economy.withdrawPlayer(player, amount).transactionSuccess();
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Vault withdraw failed for " + player.getName() + ": " + throwable.getMessage());
            return false;
        }
    }

    public String format(double amount) {
        if (economy == null) return String.valueOf(amount);
        try {
            return economy.format(amount);
        } catch (Throwable throwable) {
            return String.valueOf(amount);
        }
    }
}
