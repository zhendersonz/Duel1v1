package me.hendi.duel1v1.listener;

import java.util.UUID;
import me.hendi.duel1v1.Duel1v1;
import me.hendi.duel1v1.manager.DuelManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;

public class DuelListener
implements Listener {
    private final Duel1v1 plugin;
    private final DuelManager duelManager;

    public DuelListener(Duel1v1 plugin, DuelManager duelManager) {
        this.plugin = plugin;
        this.duelManager = duelManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        this.duelManager.restoreOnRejoin(player);
        if (!player.isOp()) {
            return;
        }
        if (this.plugin.getConfig().getBoolean("welcome-sent", false)) {
            return;
        }
        player.sendMessage("§a§lDuel1v1 §7- §ePlugin criado por §6§lZHendersonZ");
        player.sendMessage("§7Obrigado por usar o plugin!");
        this.plugin.getConfig().set("welcome-sent", true);
        this.plugin.saveConfig();
    }

    @EventHandler
    public void onNpcInteract(PlayerInteractEntityEvent event) {
        if (this.duelManager.isNpc(event.getRightClicked())) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            String err = this.duelManager.joinQueue(player);
            if (err != null) {
                player.sendMessage(err);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = (Player) event.getEntity();
        if (!this.duelManager.isInMatch(player)) {
            return;
        }
        event.setDeathMessage(null);
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.setDroppedExp(0);
        event.getDrops().clear();
        this.duelManager.handleDeath(player);
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        this.duelManager.handleRespawn(player, event);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        this.duelManager.handleQuit(player);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        HumanEntity humanEntity = event.getPlayer();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        if (!event.getView().getTitle().startsWith(this.duelManager.getGuiTitle())) {
            return;
        }
        if (!this.duelManager.hasChallenge(player)) {
            return;
        }
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            Player sender;
            UUID[] challengers;
            if (player.isOnline() && this.duelManager.hasChallenge(player) && (challengers = this.duelManager.getChallengers(player)) != null && (sender = Bukkit.getPlayer((UUID)challengers[0])) != null) {
                this.duelManager.openDuelGui(player, sender);
            }
        });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player target;
        UUID[] challengers;
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        if (!event.getView().getTitle().startsWith(this.duelManager.getGuiTitle())) {
            return;
        }
        event.setCancelled(true);
        if (event.getCurrentItem() == null) {
            return;
        }
        if (event.getCurrentItem().getType() == Material.EMERALD_BLOCK) {
            Player target2;
            challengers = this.duelManager.getChallengers(player);
            if (challengers != null && (target2 = Bukkit.getPlayer((UUID)challengers[0])) != null) {
                this.duelManager.accept(player, target2);
            }
        } else if (event.getCurrentItem().getType() == Material.REDSTONE_BLOCK && (challengers = this.duelManager.getChallengers(player)) != null && (target = Bukkit.getPlayer((UUID)challengers[0])) != null) {
            this.duelManager.deny(player, target);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player damaged = (Player)entity;
        Entity entity2 = event.getDamager();
        if (!(entity2 instanceof Player)) {
            return;
        }
        Player damager = (Player)entity2;
        boolean damagedInMatch = this.duelManager.isInMatch(damaged);
        boolean damagerInMatch = this.duelManager.isInMatch(damager);
        if (damagedInMatch && !damagerInMatch) {
            event.setCancelled(true);
        } else if (damagerInMatch && !damagedInMatch) {
            event.setCancelled(true);
        }
    }
}

