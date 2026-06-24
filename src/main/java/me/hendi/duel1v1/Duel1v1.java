package me.hendi.duel1v1;

import me.hendi.duel1v1.command.DuelCommand;
import me.hendi.duel1v1.listener.DuelListener;
import me.hendi.duel1v1.manager.DuelManager;
import me.hendi.duel1v1.stats.HologramManager;
import me.hendi.duel1v1.stats.StatsManager;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class Duel1v1
extends JavaPlugin {
    private DuelManager duelManager;
    private StatsManager statsManager;
    private HologramManager hologramManager;
    private PendingStatesManager pendingStatesManager;

    public void onEnable() {
        this.saveDefaultConfig();
        this.pendingStatesManager = new PendingStatesManager(this);
        this.pendingStatesManager.load();
        this.statsManager = new StatsManager(this);
        this.duelManager = new DuelManager(this, this.statsManager, this.pendingStatesManager);
        this.hologramManager = new HologramManager(this, this.statsManager);
        this.getCommand("duelo").setExecutor((CommandExecutor)new DuelCommand(this, this.duelManager, this.statsManager, this.hologramManager));
        this.getCommand("duelonpc").setExecutor((CommandExecutor)(sender, command, label, args) -> {
            if (!(sender instanceof Player)) {
                sender.sendMessage("\u00a7cApenas jogadores podem usar este comando!");
                return true;
            }
            Player player = (Player)sender;
            if (!player.hasPermission("duel.admin")) {
                player.sendMessage("\u00a7cSem permiss\u00e3o!");
                return true;
            }
            if (args.length > 0 && (args[0].equalsIgnoreCase("remover") || args[0].equalsIgnoreCase("remove"))) {
                this.duelManager.deleteNpc();
                player.sendMessage("\u00a7aNPC de duelo removido!");
            } else {
                this.duelManager.spawnNpc(player);
                player.sendMessage("\u00a7aNPC de duelo criado!");
            }
            return true;
        });
        this.getServer().getPluginManager().registerEvents((Listener)new DuelListener(this, this.duelManager), (Plugin)this);
        this.duelManager.loadNpc();
        this.hologramManager.load();
        this.getLogger().info("Duel1v1 ativado!");
    }

    public void onDisable() {
        if (this.pendingStatesManager != null) {
            this.pendingStatesManager.save();
        }
        if (this.hologramManager != null) {
            this.hologramManager.remove();
        }
        if (this.duelManager != null) {
            this.duelManager.cleanup();
            this.duelManager.removeNpc();
        }
        this.getLogger().info("Duel1v1 desativado!");
    }

    public DuelManager getDuelManager() {
        return this.duelManager;
    }

    public StatsManager getStatsManager() {
        return this.statsManager;
    }

    public HologramManager getHologramManager() {
        return this.hologramManager;
    }
}
