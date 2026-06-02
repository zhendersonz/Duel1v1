package me.hendi.duel1v1.manager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import me.hendi.duel1v1.Duel1v1;
import me.hendi.duel1v1.stats.StatsManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

public class DuelManager {
    private final Duel1v1 plugin;
    private final StatsManager statsManager;
    private final Map<UUID, UUID> challenges = new HashMap<UUID, UUID>();
    private final Map<UUID, Long> challengeTime = new HashMap<UUID, Long>();
    private final Set<UUID> inMatch = new HashSet<UUID>();
    private final Map<UUID, PlayerState> savedStates = new HashMap<UUID, PlayerState>();
    private final Set<UUID> deadInMatch = new HashSet<UUID>();
    private final Map<UUID, Integer> savedLevel = new HashMap<UUID, Integer>();
    private final Map<UUID, Float> savedExp = new HashMap<UUID, Float>();
    private final Map<UUID, Long> denyCooldown = new HashMap<UUID, Long>();
    private final Map<UUID, Integer> matchKills = new HashMap<UUID, Integer>();
    private final Map<UUID, PlayerState> pendingStates = new HashMap<UUID, PlayerState>();
    private final Map<UUID, Location> playerPositions = new HashMap<UUID, Location>();
    private final Map<UUID, Location> playerOrigins = new HashMap<UUID, Location>();
    private final Map<UUID, Integer> activeFreezeTasks = new HashMap<UUID, Integer>();
    private final Map<UUID, Integer> queueTimeoutTasks = new HashMap<UUID, Integer>();
    private static final int KILL_LIMIT = 2;
    private static final long QUEUE_TIMEOUT_TICKS = 1200L;
    private static final NamespacedKey NPC_KEY;
    private static final String[] NPC_SCOREBOARD_TAGS = new String[]{"duel1v1_npc", "clearlag_ignore", "clearlagg_ignore", "ClearLagIgnore", "protected_entity"};
    private final Queue<UUID> duelQueue = new LinkedList<UUID>();
    private Location pos1;
    private Location pos2;
    private Location lobby;
    private boolean lobbySet;
    private UUID npcUid;
    private boolean npcSpawnInProgress;
    private Location npcSpawnLocation;
    private int npcWatchdogTask = -1;

    static {
        NPC_KEY = new NamespacedKey("duel1v1", "npc");
    }

    public DuelManager(Duel1v1 plugin, StatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
        this.loadArena();
        this.startNpcWatchdog();
    }

    private void loadArena() {
        ConfigurationSection arena = this.plugin.getConfig().getConfigurationSection("arena");
        if (arena != null) {
            this.pos1 = this.deserializeLocation(arena.getConfigurationSection("pos1"));
            this.pos2 = this.deserializeLocation(arena.getConfigurationSection("pos2"));
            this.lobby = this.deserializeLocation(arena.getConfigurationSection("lobby"));
        }
    }

    private Location deserializeLocation(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String world = section.getString("world");
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float)section.getDouble("yaw", 0.0);
        float pitch = (float)section.getDouble("pitch", 0.0);
        return new Location(Bukkit.getWorld((String)world), x, y, z, yaw, pitch);
    }

    public void saveArenaPos(String pos, Player player) {
        Location loc = player.getLocation();
        double x = (double)loc.getBlockX() + 0.5;
        double y = loc.getY();
        double z = (double)loc.getBlockZ() + 0.5;
        float yaw = loc.getYaw();
        float pitch = loc.getPitch();
        String world = player.getWorld().getName();
        String prefix = "arena." + pos + ".";
        this.plugin.getConfig().set(prefix + "world", (Object)world);
        this.plugin.getConfig().set(prefix + "x", (Object)x);
        this.plugin.getConfig().set(prefix + "y", (Object)y);
        this.plugin.getConfig().set(prefix + "z", (Object)z);
        this.plugin.getConfig().set(prefix + "yaw", (Object)Float.valueOf(yaw));
        this.plugin.getConfig().set(prefix + "pitch", (Object)Float.valueOf(pitch));
        this.plugin.saveConfig();
        this.loadArena();
        if (pos.equals("lobby")) {
            this.lobbySet = true;
        }
        player.sendMessage("\u00a7aPosi\u00e7\u00e3o " + pos + " salva! (" + String.format("%.1f", x) + ", " + String.format("%.1f", y) + ", " + String.format("%.1f", z) + " no mundo " + world + ")");
    }

    public String challenge(Player sender, Player target) {
        if (sender.equals((Object)target)) {
            return "\u00a7cVoc\u00ea n\u00e3o pode desafiar a si mesmo!";
        }
        if (this.inMatch.contains(sender.getUniqueId())) {
            return "\u00a7cVoc\u00ea j\u00e1 est\u00e1 em uma partida!";
        }
        if (this.inMatch.contains(target.getUniqueId())) {
            return "\u00a7cEste jogador j\u00e1 est\u00e1 em uma partida!";
        }
        if (this.pos1 == null || this.pos2 == null || this.lobby == null) {
            return "\u00a7cA arena n\u00e3o foi configurada! Use /duelo setpos";
        }
        if (this.denyCooldown.containsKey(sender.getUniqueId())) {
            long remaining = (this.denyCooldown.get(sender.getUniqueId()) - System.currentTimeMillis()) / 1000L;
            if (remaining > 0L) {
                return "\u00a7cAguarde " + remaining + "s antes de desafiar este jogador novamente.";
            }
            this.denyCooldown.remove(sender.getUniqueId());
        }
        this.challenges.put(target.getUniqueId(), sender.getUniqueId());
        this.challengeTime.put(target.getUniqueId(), System.currentTimeMillis());
        sender.sendMessage("\u00a7aDesafio enviado para " + target.getName() + "!");
        target.sendMessage("\u00a7e" + sender.getName() + " \u00a7ate desafiou para um duelo!");
        this.openDuelGui(target, sender);
        this.checkTimeout(target);
        return null;
    }

    public void openDuelGui(Player target, Player sender) {
        String title = "\u00a76\u00a7lDESAFIO DE \u00a7c" + sender.getName();
        Inventory gui = Bukkit.createInventory(null, (int)9, (String)title);
        ItemStack aceitar = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta aceitarMeta = aceitar.getItemMeta();
        aceitarMeta.setDisplayName("\u00a7a\u00a7lACEITAR");
        aceitar.setItemMeta(aceitarMeta);
        ItemStack recusar = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta recusarMeta = recusar.getItemMeta();
        recusarMeta.setDisplayName("\u00a7c\u00a7lRECUSAR");
        recusar.setItemMeta(recusarMeta);
        ItemStack vidro = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta vidroMeta = vidro.getItemMeta();
        vidroMeta.setDisplayName(" ");
        vidro.setItemMeta(vidroMeta);
        for (int i = 0; i < 9; ++i) {
            gui.setItem(i, vidro);
        }
        gui.setItem(3, aceitar);
        gui.setItem(5, recusar);
        target.openInventory(gui);
    }

    private void checkTimeout(Player target) {
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (this.challenges.containsKey(target.getUniqueId())) {
                Player sender = Bukkit.getPlayer((UUID)this.challenges.get(target.getUniqueId()));
                this.challenges.remove(target.getUniqueId());
                this.challengeTime.remove(target.getUniqueId());
                if (target.isOnline() && target.getOpenInventory().getTitle().startsWith("\u00a76\u00a7lDESAFIO DE")) {
                    target.closeInventory();
                }
                if (sender != null) {
                    sender.sendMessage("\u00a7c" + target.getName() + " n\u00e3o aceitou o desafio a tempo.");
                }
                target.sendMessage("\u00a7cDesafio expirado.");
            }
        }, (long)this.plugin.getConfig().getInt("challenge-timeout", 60) * 20L);
    }

    public String accept(Player player, Player target) {
        UUID targetId = target.getUniqueId();
        if (!this.challenges.containsKey(player.getUniqueId()) || !this.challenges.get(player.getUniqueId()).equals(targetId)) {
            return "\u00a7cVoc\u00ea n\u00e3o tem um desafio pendente deste jogador!";
        }
        this.challenges.remove(player.getUniqueId());
        this.challengeTime.remove(player.getUniqueId());
        if (player.getOpenInventory().getTitle().startsWith("\u00a76\u00a7lDESAFIO DE")) {
            player.closeInventory();
        }
        this.startMatch(player, target);
        return null;
    }

    public String deny(Player player, Player target) {
        UUID targetId = target.getUniqueId();
        if (!this.challenges.containsKey(player.getUniqueId()) || !this.challenges.get(player.getUniqueId()).equals(targetId)) {
            return "\u00a7cVoc\u00ea n\u00e3o tem um desafio pendente deste jogador!";
        }
        this.challenges.remove(player.getUniqueId());
        this.challengeTime.remove(player.getUniqueId());
        this.denyCooldown.put(target.getUniqueId(), System.currentTimeMillis() + 60000L);
        if (player.getOpenInventory().getTitle().startsWith("\u00a76\u00a7lDESAFIO DE")) {
            player.closeInventory();
        }
        player.sendMessage("\u00a7cDesafio recusado.");
        target.sendMessage("\u00a7c" + player.getName() + " recusou seu desafio.");
        return null;
    }

    private void startMatch(Player p1, Player p2) {
        this.inMatch.add(p1.getUniqueId());
        this.inMatch.add(p2.getUniqueId());
        this.savedStates.put(p1.getUniqueId(), this.saveState(p1));
        this.savedStates.put(p2.getUniqueId(), this.saveState(p2));
        this.savedLevel.put(p1.getUniqueId(), p1.getLevel());
        this.savedExp.put(p1.getUniqueId(), Float.valueOf(p1.getExp()));
        this.savedLevel.put(p2.getUniqueId(), p2.getLevel());
        this.savedExp.put(p2.getUniqueId(), Float.valueOf(p2.getExp()));
        this.matchKills.put(p1.getUniqueId(), 0);
        this.matchKills.put(p2.getUniqueId(), 0);
        this.playerPositions.put(p1.getUniqueId(), this.pos1);
        this.playerPositions.put(p2.getUniqueId(), this.pos2);
        // Save original locations for teleporting back after duel
        this.playerOrigins.put(p1.getUniqueId(), p1.getLocation().clone());
        this.playerOrigins.put(p2.getUniqueId(), p2.getLocation().clone());
        this.statsManager.addBattle(p1.getUniqueId());
        this.statsManager.addBattle(p2.getUniqueId());
        int freezeTask = this.startFreeze(p1, p2, p1.getLocation(), p2.getLocation());
        p1.sendTitle("\u00a76\u00a7lDUELO!", "\u00a7eQue ven\u00e7a o melhor!", 0, 40, 10);
        p2.sendTitle("\u00a76\u00a7lDUELO!", "\u00a7eQue ven\u00e7a o melhor!", 0, 40, 10);
        for (int i = 5; i >= 1; --i) {
            int count = i;
            Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                p1.sendTitle("\u00a76\u00a7l" + count, "", 0, 15, 5);
                p2.sendTitle("\u00a76\u00a7l" + count, "", 0, 15, 5);
            }, (long)(5 - i) * 20L);
        }
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            this.stopFreeze(freezeTask, p1, p2);
            if (!this.inMatch.contains(p1.getUniqueId()) || !this.inMatch.contains(p2.getUniqueId())) {
                return;
            }
            this.resetPlayer(p1);
            this.resetPlayer(p2);
            p1.teleport(this.pos1);
            p2.teleport(this.pos2);
            p1.setNoDamageTicks(0);
            p2.setNoDamageTicks(0);
            this.applyKit(p1);
            this.applyKit(p2);
            p1.sendTitle("\u00a7a\u00a7lV\u00c3O!", "", 0, 10, 10);
            p2.sendTitle("\u00a7a\u00a7lV\u00c3O!", "", 0, 10, 10);
            Bukkit.broadcastMessage((String)("\u00a76\u00a7lDUELO! \u00a7e" + p1.getName() + " \u00a77vs \u00a7e" + p2.getName()));
            this.refreshEntity(p1, this.pos1);
            this.refreshEntity(p2, this.pos2);
        }, 100L);
    }

    public void endMatch(Player loser, Player winner) {
        if (!this.inMatch.contains(loser.getUniqueId()) || !this.inMatch.contains(winner.getUniqueId())) {
            return;
        }
        UUID loserUUID = loser.getUniqueId();
        UUID winnerUUID = winner.getUniqueId();
        
        PlayerState loserState;
        this.cancelActiveFreeze(loser);
        this.cancelActiveFreeze(winner);
        int wins = this.matchKills.getOrDefault(winnerUUID, 0);
        int deaths = this.matchKills.getOrDefault(loserUUID, 0);
        
        // Remove from active match
        this.inMatch.remove(loserUUID);
        this.inMatch.remove(winnerUUID);
        this.deadInMatch.remove(winner.getUniqueId());
        this.matchKills.remove(loserUUID);
        this.matchKills.remove(winnerUUID);
        this.playerPositions.remove(loserUUID);
        this.playerPositions.remove(winnerUUID);
        this.savedLevel.remove(loserUUID);
        this.savedLevel.remove(winnerUUID);
        this.savedExp.remove(loserUUID);
        this.savedExp.remove(winnerUUID);
        
        PlayerState winnerState = this.savedStates.remove(winnerUUID);
        if (winnerState != null) {
            this.applyRestore(winner, winnerState);
        }
        if ((loserState = this.savedStates.remove(loserUUID)) != null) {
            this.pendingStates.put(loserUUID, loserState);
            loser.getInventory().clear();
            loser.getInventory().setArmorContents(null);
        }
        
        // Get original locations for both players
        Location winnerOrigin = this.playerOrigins.remove(winnerUUID);
        Location loserOrigin = this.playerOrigins.remove(loserUUID);
        
        // If original location exists, use it; otherwise use targetLocation as fallback
        Location winnerTarget = winnerOrigin != null ? winnerOrigin : this.targetLocation(winner);
        Location loserTarget = loserOrigin != null ? loserOrigin : this.targetLocation(loser);
        
        Bukkit.broadcastMessage((String)("\u00a76\u00a7lVIT\u00d3RIA! \u00a7a" + winner.getName() + " \u00a77venceu \u00a7e" + loser.getName() + " \u00a77(" + wins + "-" + deaths + ")"));
        this.spawnFireworks(winner);
        
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            // Teleport each player to their original location
            winner.teleport(winnerTarget);
            loser.teleport(loserTarget);
        }, 60L);
    }

    public void leaveMatch(Player player) {
        if (!this.inMatch.contains(player.getUniqueId())) {
            player.sendMessage("\u00a7cVoc\u00ea n\u00e3o est\u00e1 em uma partida!");
            return;
        }
        for (Map.Entry<UUID, PlayerState> entry : this.savedStates.entrySet()) {
            if (!this.inMatch.contains(entry.getKey()) || entry.getKey().equals(player.getUniqueId())) continue;
            Player opponent = Bukkit.getPlayer((UUID)entry.getKey());
            if (opponent != null) {
                this.matchKills.put(opponent.getUniqueId(), this.matchKills.getOrDefault(opponent.getUniqueId(), 0) + 1);
                this.statsManager.addKill(opponent.getUniqueId());
                this.statsManager.addDeath(player.getUniqueId());
                this.endMatch(player, opponent);
            }
            return;
        }
        PlayerState state = this.savedStates.remove(player.getUniqueId());
        if (state != null) {
            this.pendingStates.put(player.getUniqueId(), state);
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
        }
        this.inMatch.remove(player.getUniqueId());
        this.deadInMatch.remove(player.getUniqueId());
        this.savedLevel.remove(player.getUniqueId());
        this.savedExp.remove(player.getUniqueId());
        this.matchKills.remove(player.getUniqueId());
    }

    public void handleDeath(Player player) {
        if (!this.inMatch.contains(player.getUniqueId())) {
            return;
        }
        this.deadInMatch.add(player.getUniqueId());
        Player killer = player.getKiller();
        Player winner = null;
        if (killer != null && this.inMatch.contains(killer.getUniqueId())) {
            winner = killer;
        } else {
            for (Map.Entry<UUID, PlayerState> entry : this.savedStates.entrySet()) {
                if (!this.inMatch.contains(entry.getKey()) || entry.getKey().equals(player.getUniqueId())) continue;
                winner = Bukkit.getPlayer((UUID)entry.getKey());
                break;
            }
        }
        if (winner != null) {
            int winnerKills = this.matchKills.getOrDefault(winner.getUniqueId(), 0) + 1;
            this.matchKills.put(winner.getUniqueId(), winnerKills);
            this.statsManager.addKill(winner.getUniqueId());
            this.statsManager.addDeath(player.getUniqueId());
            Bukkit.broadcastMessage((String)("\u00a7e" + winner.getName() + " \u00a77mata \u00a7c" + player.getName() + " \u00a77(" + winnerKills + "-" + String.valueOf(this.matchKills.getOrDefault(player.getUniqueId(), 0)) + ")"));
            if (winnerKills >= KILL_LIMIT) {
                this.endMatch(player, winner);
            }
        }
    }

    public void handleRespawn(Player player, PlayerRespawnEvent event) {
        if (this.deadInMatch.contains(player.getUniqueId())) {
            Player opponent;
            Player finalOpponent;
            this.deadInMatch.remove(player.getUniqueId());
            if (this.inMatch.contains(player.getUniqueId()) && (finalOpponent = (opponent = this.findOpponent(player))) != null && this.inMatch.contains(finalOpponent.getUniqueId())) {
                Location playerPos = this.playerPositions.getOrDefault(player.getUniqueId(), this.pos1);
                Location opponentPos = this.playerPositions.getOrDefault(finalOpponent.getUniqueId(), this.pos2);
                event.setRespawnLocation(playerPos);
                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                    player.teleport(playerPos);
                    finalOpponent.teleport(opponentPos);
                    player.setNoDamageTicks(200);
                    finalOpponent.setNoDamageTicks(200);
                    int totalKills = this.matchKills.getOrDefault(player.getUniqueId(), 0) + this.matchKills.getOrDefault(finalOpponent.getUniqueId(), 0);
                    int round = totalKills + 1;
                    player.sendTitle("\u00a76\u00a7lRODADA " + round, "\u00a7ePrepare-se!", 0, 40, 10);
                    finalOpponent.sendTitle("\u00a76\u00a7lRODADA " + round, "\u00a7ePrepare-se!", 0, 40, 10);
                    int ft = this.startFreeze(player, finalOpponent, playerPos, opponentPos);
                    for (int i = 5; i >= 1; --i) {
                        int count = i;
                        Player p = player;
                        Player op = finalOpponent;
                        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                            p.sendTitle("\u00a76\u00a7l" + count, "", 0, 15, 5);
                            op.sendTitle("\u00a76\u00a7l" + count, "", 0, 15, 5);
                        }, (5L - (long)i) * 20L);
                    }
                    int ftask = ft;
                    Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                        this.stopFreeze(ftask, player, finalOpponent);
                        if (!this.inMatch.contains(player.getUniqueId()) || !this.inMatch.contains(finalOpponent.getUniqueId())) {
                            return;
                        }
                        player.setNoDamageTicks(0);
                        finalOpponent.setNoDamageTicks(0);
                        this.resetPlayer(player);
                        this.resetPlayer(finalOpponent);
                        this.applyKit(player);
                        this.applyKit(finalOpponent);
                        player.sendTitle("\u00a7a\u00a7lV\u00c3O!", "", 0, 10, 10);
                        finalOpponent.sendTitle("\u00a7a\u00a7lV\u00c3O!", "", 0, 10, 10);
                        this.refreshEntity(player, playerPos);
                        this.refreshEntity(finalOpponent, opponentPos);
                    }, 100L);
                });
                return;
            }
            PlayerState pending = this.pendingStates.remove(player.getUniqueId());
            if (pending != null) {
                Location loc = this.targetLocation(player);
                this.applyRestore(player, pending);
                player.teleport(loc);
                event.setRespawnLocation(loc);
                return;
            }
        } else if (this.savedStates.containsKey(player.getUniqueId())) {
            this.restoreState(player);
        }
        this.deadInMatch.remove(player.getUniqueId());
        this.savedLevel.remove(player.getUniqueId());
        this.savedExp.remove(player.getUniqueId());
        this.activeFreezeTasks.remove(player.getUniqueId());
        Location loc = this.targetLocation(player);
        event.setRespawnLocation(loc);
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            player.setLevel(0);
            player.setExp(0.0f);
            player.setGameMode(GameMode.SURVIVAL);
            player.setWalkSpeed(0.2f);
            player.setFlySpeed(0.1f);
            player.setAllowFlight(false);
            player.setFlying(false);
            player.teleport(loc);
        });
    }

    public void handleQuit(Player player) {
        this.deadInMatch.remove(player.getUniqueId());
        this.savedLevel.remove(player.getUniqueId());
        this.savedExp.remove(player.getUniqueId());
        this.cancelActiveFreeze(player);
        if (!this.inMatch.contains(player.getUniqueId())) {
            return;
        }
        this.leaveMatch(player);
    }

    private int startFreeze(Player p1, Player p2, Location l1, Location l2) {
        p1.setAllowFlight(true);
        p2.setAllowFlight(true);
        p1.setFlying(false);
        p2.setFlying(false);
        p1.setWalkSpeed(0.0f);
        p2.setWalkSpeed(0.0f);
        p1.setFlySpeed(0.0f);
        p2.setFlySpeed(0.0f);
        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask((Plugin)this.plugin, () -> {
            p1.teleport(l1);
            p2.teleport(l2);
            p1.setVelocity(p1.getVelocity().setY(0));
            p2.setVelocity(p2.getVelocity().setY(0));
        }, 0L, 1L);
        this.activeFreezeTasks.put(p1.getUniqueId(), taskId);
        this.activeFreezeTasks.put(p2.getUniqueId(), taskId);
        return taskId;
    }

    private void stopFreeze(int taskId, Player p1, Player p2) {
        Bukkit.getScheduler().cancelTask(taskId);
        this.activeFreezeTasks.remove(p1.getUniqueId());
        this.activeFreezeTasks.remove(p2.getUniqueId());
        p1.setWalkSpeed(0.2f);
        p2.setWalkSpeed(0.2f);
        p1.setFlySpeed(0.1f);
        p2.setFlySpeed(0.1f);
        p1.setFlying(false);
        p2.setFlying(false);
    }

    private void cancelActiveFreeze(Player player) {
        Integer taskId = this.activeFreezeTasks.remove(player.getUniqueId());
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId.intValue());
        }
        if (player.isOnline()) {
            player.setWalkSpeed(0.2f);
            player.setFlySpeed(0.1f);
            player.setFlying(false);
        }
    }

    private void refreshEntity(Player player, Location target) {
        player.setAllowFlight(true);
        player.setFlying(false);
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(target);
        player.setNoDamageTicks(0);
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            player.setGameMode(GameMode.SURVIVAL);
            player.setFlying(false);
            player.teleport(target.clone().add(0.0, 0.02, 0.0));
            player.setNoDamageTicks(0);
        }, 5L);
    }

    private void spawnFireworks(Player player) {
        Random random = new Random();
        for (int i = 0; i < 3; ++i) {
            Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                Color color = Color.fromRGB((int)random.nextInt(256), (int)random.nextInt(256), (int)random.nextInt(256));
                FireworkEffect effect = FireworkEffect.builder().withColor(color).with(FireworkEffect.Type.values()[random.nextInt(FireworkEffect.Type.values().length)]).withFade(Color.fromRGB((int)random.nextInt(256), (int)random.nextInt(256), (int)random.nextInt(256))).trail(random.nextBoolean()).flicker(random.nextBoolean()).build();
                for (int f = 0; f < 3; ++f) {
                    Location loc = player.getLocation().clone().add((double)(random.nextInt(5) - 2), 1.0, (double)(random.nextInt(5) - 2));
                    Firework fw = (Firework)player.getWorld().spawnEntity(loc, EntityType.FIREWORK_ROCKET);
                    FireworkMeta meta = fw.getFireworkMeta();
                    meta.addEffect(effect);
                    meta.setPower(1);
                    fw.setFireworkMeta(meta);
                }
            }, (long)i * 20L);
        }
    }

    private PlayerState saveState(Player player) {
        int i;
        ItemStack[] inv = player.getInventory().getContents();
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (i = 0; i < inv.length; ++i) {
            if (inv[i] == null) continue;
            inv[i] = inv[i].clone();
        }
        for (i = 0; i < armor.length; ++i) {
            if (armor[i] == null) continue;
            armor[i] = armor[i].clone();
        }
        return new PlayerState(inv, armor, player.getLocation(), player.getGameMode(), player.getHealth(), player.getFoodLevel(), player.getLevel(), player.getExp(), player.getWalkSpeed(), player.getFlySpeed(), player.getAllowFlight(), player.isFlying());
    }

    private void restoreState(Player player) {
        PlayerState state = this.savedStates.remove(player.getUniqueId());
        if (state != null) {
            this.applyRestore(player, state);
        }
    }

    private void applyRestore(Player player, PlayerState state) {
        player.getInventory().clear();
        player.getInventory().setContents(state.inventory());
        player.getInventory().setArmorContents(state.armor());
        player.teleport(state.location());
        player.setGameMode(state.gameMode());
        player.setHealth(state.health());
        player.setFoodLevel(state.foodLevel());
        player.setLevel(state.level());
        player.setExp(state.exp());
        player.setWalkSpeed(state.walkSpeed());
        player.setFlySpeed(state.flySpeed());
        player.setAllowFlight(state.allowFlight());
        player.setFlying(state.flying() && state.allowFlight());
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    public void restoreOnRejoin(Player player) {
        this.deadInMatch.remove(player.getUniqueId());
        this.savedLevel.remove(player.getUniqueId());
        this.savedExp.remove(player.getUniqueId());
        this.activeFreezeTasks.remove(player.getUniqueId());
        PlayerState state = this.pendingStates.remove(player.getUniqueId());
        if (state != null) {
            this.applyRestore(player, state);
            player.sendMessage("\u00a7aSeus itens foram restaurados ap\u00f3s desconectar durante um duelo.");
        }
    }

    private Player findOpponent(Player player) {
        for (Map.Entry<UUID, PlayerState> entry : this.savedStates.entrySet()) {
            if (!this.inMatch.contains(entry.getKey()) || entry.getKey().equals(player.getUniqueId())) continue;
            return Bukkit.getPlayer((UUID)entry.getKey());
        }
        return null;
    }

    private void resetPlayer(Player player) {
        player.setMaxHealth(20.0);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(10.0f);
        player.setLevel(0);
        player.setExp(0.0f);
        player.setFireTicks(0);
        player.setFallDistance(0.0f);
        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);
        player.setGameMode(GameMode.SURVIVAL);
        player.setNoDamageTicks(0);
        player.setAllowFlight(true);
        player.setFlying(false);
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
    }

    private Location targetLocation(Player player) {
        return this.lobbySet ? this.lobby : player.getWorld().getSpawnLocation();
    }

    private void applyKit(Player player) {
        Material material;
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.setArmorContents(null);
        for (String itemStr : this.plugin.getConfig().getStringList("kit.items")) {
            String[] parts = itemStr.split(":");
            material = Material.getMaterial((String)parts[0].toUpperCase());
            if (material == null) continue;
            int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
            inv.addItem(new ItemStack[]{new ItemStack(material, amount)});
        }
        int slot = 0;
        for (String armorStr : this.plugin.getConfig().getStringList("kit.armor")) {
            material = Material.getMaterial((String)armorStr.toUpperCase());
            if (material == null) continue;
            switch (slot) {
                case 0: {
                    inv.setHelmet(new ItemStack(material));
                    break;
                }
                case 1: {
                    inv.setChestplate(new ItemStack(material));
                    break;
                }
                case 2: {
                    inv.setLeggings(new ItemStack(material));
                    break;
                }
                case 3: {
                    inv.setBoots(new ItemStack(material));
                }
            }
            ++slot;
        }
        String offhand = this.plugin.getConfig().getString("kit.offhand");
        if (offhand != null && Material.getMaterial(offhand.toUpperCase()) != null) {
            inv.setItemInOffHand(new ItemStack(Material.getMaterial(offhand.toUpperCase())));
        }
    }

    public void cleanup() {
        for (UUID uid : this.inMatch) {
            Player p = Bukkit.getPlayer((UUID)uid);
            if (p == null) continue;
            this.restoreState(p);
            Location loc = this.targetLocation(p);
            p.teleport(loc);
        }
        for (Integer taskId : new HashSet<Integer>(this.activeFreezeTasks.values())) {
            Bukkit.getScheduler().cancelTask(taskId.intValue());
        }
        this.activeFreezeTasks.clear();
        this.inMatch.clear();
        this.deadInMatch.clear();
        this.savedLevel.clear();
        this.savedExp.clear();
        this.matchKills.clear();
        this.playerPositions.clear();
        for (Integer taskId : this.queueTimeoutTasks.values()) {
            Bukkit.getScheduler().cancelTask(taskId.intValue());
        }
        this.queueTimeoutTasks.clear();
        this.cancelNpcWatchdog();
    }

    public String joinQueue(Player player) {
        UUID uid;
        if (this.inMatch.contains(player.getUniqueId())) {
            return "\u00a7cVoc\u00ea j\u00e1 est\u00e1 em uma partida!";
        }
        if (this.duelQueue.contains(player.getUniqueId())) {
            return "\u00a7cVoc\u00ea j\u00e1 est\u00e1 na fila!";
        }
        if (this.pos1 == null || this.pos2 == null || this.lobby == null) {
            return "\u00a7cA arena n\u00e3o foi configurada!";
        }
        this.duelQueue.add(player.getUniqueId());
        this.scheduleQueueTimeout(player);
        int pos = 1;
        Iterator iterator = this.duelQueue.iterator();
        while (iterator.hasNext() && !(uid = (UUID)iterator.next()).equals(player.getUniqueId())) {
            ++pos;
        }
        Bukkit.broadcastMessage((String)("\u00a7e" + player.getName() + " \u00a77entrou na fila de duelos! (posi\u00e7\u00e3o " + pos + "\u00ba)"));
        this.tryStartQueueMatch();
        return null;
    }

    public String leaveQueue(Player player) {
        if (!this.duelQueue.remove(player.getUniqueId())) {
            return "\u00a7cVoc\u00ea n\u00e3o est\u00e1 na fila!";
        }
        this.cancelQueueTimeout(player.getUniqueId());
        player.sendTitle("", "", 0, 1, 0);
        player.sendMessage("\u00a7cVoc\u00ea saiu da fila de duelos.");
        return null;
    }

    public boolean isInQueue(Player player) {
        return this.duelQueue.contains(player.getUniqueId());
    }

    public int getQueuePosition(Player player) {
        int pos = 1;
        for (UUID uid : this.duelQueue) {
            if (uid.equals(player.getUniqueId())) {
                return pos;
            }
            ++pos;
        }
        return -1;
    }

    private void tryStartQueueMatch() {
        if (this.duelQueue.size() < 2) {
            return;
        }
        if (!this.inMatch.isEmpty()) {
            return;
        }
        UUID id1 = this.duelQueue.poll();
        UUID id2 = this.duelQueue.poll();
        if (id1 == null || id2 == null) {
            return;
        }
        this.cancelQueueTimeout(id1);
        this.cancelQueueTimeout(id2);
        Player p1 = Bukkit.getPlayer((UUID)id1);
        Player p2 = Bukkit.getPlayer((UUID)id2);
        if (p1 == null || !p1.isOnline() || p2 == null || !p2.isOnline()) {
            if (p1 != null && p1.isOnline()) {
                this.duelQueue.add(id1);
                this.scheduleQueueTimeout(p1);
            }
            if (p2 != null && p2.isOnline()) {
                this.duelQueue.add(id2);
                this.scheduleQueueTimeout(p2);
            }
            return;
        }
        this.startMatch(p1, p2);
    }

    private void scheduleQueueTimeout(Player player) {
        final UUID uid = player.getUniqueId();
        this.cancelQueueTimeout(uid);
        player.sendTitle("\u00a7eDigite \u00a7c/duelo cancelar", "\u00a77Para cancelar sua entrada no duelo", 10, 70, 20);
        int taskId = new BukkitRunnable() {
            private int remaining = 60;

            public void run() {
                Player queued = Bukkit.getPlayer(uid);
                if (!duelQueue.contains(uid) || queued == null || !queued.isOnline()) {
                    queueTimeoutTasks.remove(uid);
                    cancel();
                    return;
                }
                if (remaining <= 0) {
                    duelQueue.remove(uid);
                    queueTimeoutTasks.remove(uid);
                    queued.sendActionBar(Component.empty());
                    queued.sendMessage("\u00a7cNenhum jogador entrou no duelo em 60 segundos.");
                    queued.sendMessage("\u00a7eUse /duelo para entrar na fila novamente.");
                    cancel();
                    return;
                }
                queued.sendActionBar(Component.text("\u00a7eAguardando oponente: \u00a7c" + remaining + "s \u00a77| \u00a7f/duelo cancelar"));
                --remaining;
            }
        }.runTaskTimer(plugin, 0L, 20L).getTaskId();
        this.queueTimeoutTasks.put(uid, taskId);
    }

    private void cancelQueueTimeout(UUID uid) {
        Integer taskId = this.queueTimeoutTasks.remove(uid);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId.intValue());
        }
        Player player = Bukkit.getPlayer(uid);
        if (player != null && player.isOnline()) {
            player.sendActionBar(Component.empty());
        }
    }

    public boolean isInMatch(Player player) {
        return this.inMatch.contains(player.getUniqueId());
    }

    public String getGuiTitle() {
        return "\u00a76\u00a7lDESAFIO DE";
    }

    public boolean hasChallenge(Player player) {
        return this.challenges.containsKey(player.getUniqueId());
    }

    public UUID[] getChallengers(Player player) {
        if (!this.challenges.containsKey(player.getUniqueId())) {
            return null;
        }
        return new UUID[]{this.challenges.get(player.getUniqueId())};
    }

    public void spawnNpc(Player player) {
        Location loc = player.getLocation();
        this.removeNpc();
        Villager npc = this.spawnProtectedNpc(loc);
        this.npcUid = npc.getUniqueId();
        String prefix = "npc.";
        this.plugin.getConfig().set(prefix + "world", loc.getWorld().getName());
        this.plugin.getConfig().set(prefix + "x", loc.getX());
        this.plugin.getConfig().set(prefix + "y", loc.getY());
        this.plugin.getConfig().set(prefix + "z", loc.getZ());
        this.plugin.getConfig().set(prefix + "yaw", (double)loc.getYaw());
        this.plugin.getConfig().set(prefix + "pitch", (double)loc.getPitch());
        this.plugin.saveConfig();
    }

    public void removeNpc() {
        if (this.npcUid != null) {
            for (org.bukkit.World world : Bukkit.getWorlds()) {
                Entity entity = world.getEntity(this.npcUid);
                if (entity != null) {
                    entity.remove();
                    break;
                }
            }
            this.npcUid = null;
        }
    }

    public void deleteNpc() {
        this.removeNpc();
        this.plugin.getConfig().set("npc", null);
        this.plugin.saveConfig();
    }

    public boolean isNpc(Entity entity) {
        return entity.getPersistentDataContainer().has(NPC_KEY, PersistentDataType.BOOLEAN);
    }

    public void handleNpcCreatureSpawn(CreatureSpawnEvent event) {
        if (this.isNpcSpawnAttempt(event.getLocation(), event.getEntityType())) {
            event.setCancelled(false);
            this.protectNpc((Villager)event.getEntity());
        }
    }

    public void loadNpc() {
        ConfigurationSection npcSection = this.plugin.getConfig().getConfigurationSection("npc");
        if (npcSection == null) {
            return;
        }
        Location loc = this.deserializeLocation(npcSection);
        if (loc == null || loc.getWorld() == null) {
            return;
        }
        Villager npc = this.spawnProtectedNpc(loc);
        this.npcUid = npc.getUniqueId();
    }

    private Villager spawnProtectedNpc(Location loc) {
        this.npcSpawnInProgress = true;
        this.npcSpawnLocation = loc.clone();
        try {
            return loc.getWorld().spawn(loc, Villager.class, this::protectNpc);
        }
        finally {
            this.npcSpawnInProgress = false;
            this.npcSpawnLocation = null;
        }
    }

    private void protectNpc(Villager npc) {
        npc.setAI(false);
        npc.setInvulnerable(true);
        npc.setSilent(true);
        npc.setCollidable(false);
        npc.setGravity(false);
        npc.setPersistent(true);
        npc.setRemoveWhenFarAway(false);
        npc.setCustomNameVisible(true);
        npc.customName(Component.text("\u00a7e\u00a7lCLIQUE PARA DUELAR"));
        npc.getPersistentDataContainer().set(NPC_KEY, PersistentDataType.BOOLEAN, true);
        npc.setMetadata("Duel1v1NPC", new FixedMetadataValue(this.plugin, true));
        npc.setMetadata("NPC", new FixedMetadataValue(this.plugin, true));
        npc.setMetadata("ClearLagIgnore", new FixedMetadataValue(this.plugin, true));
        npc.setMetadata("clearlag_ignore", new FixedMetadataValue(this.plugin, true));
        npc.setMetadata("clearlagg_ignore", new FixedMetadataValue(this.plugin, true));
        for (String tag : NPC_SCOREBOARD_TAGS) {
            npc.addScoreboardTag(tag);
        }
    }

    private boolean isNpcSpawnAttempt(Location loc, EntityType type) {
        if (!this.npcSpawnInProgress || type != EntityType.VILLAGER || this.npcSpawnLocation == null || loc.getWorld() == null) {
            return false;
        }
        return loc.getWorld().equals(this.npcSpawnLocation.getWorld()) && loc.distanceSquared(this.npcSpawnLocation) <= 4.0;
    }

    private void startNpcWatchdog() {
        this.cancelNpcWatchdog();
        this.npcWatchdogTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            ConfigurationSection npcSection = this.plugin.getConfig().getConfigurationSection("npc");
            if (npcSection == null) {
                return;
            }
            if (this.npcUid != null) {
                for (org.bukkit.World world : Bukkit.getWorlds()) {
                    Entity entity = world.getEntity(this.npcUid);
                    if (entity instanceof Villager) {
                        this.protectNpc((Villager)entity);
                        return;
                    }
                }
            }
            Location loc = this.deserializeLocation(npcSection);
            if (loc == null || loc.getWorld() == null) {
                return;
            }
            Villager npc = this.spawnProtectedNpc(loc);
            this.npcUid = npc.getUniqueId();
        }, 200L, 200L).getTaskId();
    }

    private void cancelNpcWatchdog() {
        if (this.npcWatchdogTask != -1) {
            Bukkit.getScheduler().cancelTask(this.npcWatchdogTask);
            this.npcWatchdogTask = -1;
        }
    }

    public Location getLobby() {
        return this.lobby;
    }

    public boolean isArenaConfigured() {
        return this.pos1 != null && this.pos2 != null && this.lobby != null;
    }

    private record PlayerState(ItemStack[] inventory, ItemStack[] armor, Location location, GameMode gameMode, double health, int foodLevel, int level, float exp, float walkSpeed, float flySpeed, boolean allowFlight, boolean flying) {
    }
}
