package me.hendi.duel1v1.command;

import java.util.UUID;
import me.hendi.duel1v1.Duel1v1;
import me.hendi.duel1v1.manager.DuelManager;
import me.hendi.duel1v1.stats.HologramManager;
import me.hendi.duel1v1.stats.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

public class DuelCommand
implements CommandExecutor {
    private final Duel1v1 plugin;
    private final DuelManager duelManager;
    private final StatsManager statsManager;
    private final HologramManager hologramManager;

    public DuelCommand(Duel1v1 plugin, DuelManager duelManager, StatsManager statsManager, HologramManager hologramManager) {
        this.plugin = plugin;
        this.duelManager = duelManager;
        this.statsManager = statsManager;
        this.hologramManager = hologramManager;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cApenas jogadores podem usar este comando!");
            return true;
        }
        Player player = (Player)sender;
        if (args.length == 0) {
            this.sendHelp(player);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "accept":
            case "aceitar": {
                this.handleAccept(player, args);
                break;
            }
            case "deny":
            case "recusar": {
                this.handleDeny(player, args);
                break;
            }
            case "leave":
            case "sair": {
                this.duelManager.leaveMatch(player);
                break;
            }
            case "cancelar": {
                String err = this.duelManager.leaveQueue(player);
                if (err == null) break;
                player.sendMessage(err);
                break;
            }
            case "setpos": {
                this.handleSetPos(player, args);
                break;
            }
            case "stats": {
                this.handleStats(player, args);
                break;
            }
            case "holograma": {
                this.handleHolograma(player, args);
                break;
            }
            case "resetar": {
                this.handleResetPlayer(player, args);
                break;
            }
            default: {
                this.handleChallenge(player, args);
            }
        }
        return true;
    }

    private void handleChallenge(Player player, String[] args) {
        Player target = Bukkit.getPlayer((String)args[0]);
        if (target == null) {
            player.sendMessage("\u00a7cJogador n\u00e3o encontrado!");
            return;
        }
        String error = this.duelManager.challenge(player, target);
        if (error != null) {
            player.sendMessage(error);
        }
    }

    private void handleAccept(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("\u00a7cUse: /duelo aceitar <jogador>");
            return;
        }
        Player target = Bukkit.getPlayer((String)args[1]);
        if (target == null) {
            player.sendMessage("\u00a7cJogador n\u00e3o encontrado!");
            return;
        }
        String error = this.duelManager.accept(player, target);
        if (error != null) {
            player.sendMessage(error);
        }
    }

    private void handleDeny(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("\u00a7cUse: /duelo recusar <jogador>");
            return;
        }
        Player target = Bukkit.getPlayer((String)args[1]);
        if (target == null) {
            player.sendMessage("\u00a7cJogador n\u00e3o encontrado!");
            return;
        }
        String error = this.duelManager.deny(player, target);
        if (error != null) {
            player.sendMessage(error);
        }
    }

    private void handleSetPos(Player player, String[] args) {
        if (!player.hasPermission("duel.admin")) {
            player.sendMessage("\u00a7cSem permiss\u00e3o!");
            return;
        }
        if (args.length < 2) {
            player.sendMessage("\u00a7cUse: /duelo setpos <pos1|pos2|lobby>");
            return;
        }
        String pos = args[1].toLowerCase();
        if (!(pos.equals("pos1") || pos.equals("pos2") || pos.equals("lobby"))) {
            player.sendMessage("\u00a7cUse: /duelo setpos <pos1|pos2|lobby>");
            return;
        }
        this.duelManager.saveArenaPos(pos, player);
        player.sendMessage("\u00a7aPosi\u00e7\u00e3o " + pos + " salva!");
    }

    private void handleStats(Player player, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("resetar")) {
            if (!player.hasPermission("duel.admin")) {
                player.sendMessage("\u00a7cSem permiss\u00e3o!");
                return;
            }
            if (args.length < 3) {
                player.sendMessage("\u00a7cUse: /duelo stats resetar <jogador>");
                return;
            }
            Player target = Bukkit.getPlayer((String)args[2]);
            if (target == null) {
                player.sendMessage("\u00a7cJogador n\u00e3o encontrado!");
                return;
            }
            this.statsManager.reset(target.getUniqueId());
            player.sendMessage("\u00a7aEstat\u00edsticas de " + target.getName() + " resetadas!");
            return;
        }
        UUID targetUuid;
        String targetName;
        if (args.length >= 2) {
            Player target = Bukkit.getPlayer((String)args[1]);
            if (target == null) {
                player.sendMessage("\u00a7cJogador n\u00e3o encontrado!");
                return;
            }
            targetUuid = target.getUniqueId();
            targetName = target.getName();
        } else {
            targetUuid = player.getUniqueId();
            targetName = player.getName();
        }
        int kills = this.statsManager.getKills(targetUuid);
        int deaths = this.statsManager.getDeaths(targetUuid);
        int battles = this.statsManager.getBattles(targetUuid);
        double kd = this.statsManager.getKD(targetUuid);
        player.sendMessage("\u00a76\u00a7l=== ESTAT\u00cdSTICAS ===");
        player.sendMessage("\u00a77Jogador: \u00a7e" + targetName);
        player.sendMessage("\u00a77Kills: \u00a7a" + kills);
        player.sendMessage("\u00a77Mortes: \u00a7c" + deaths);
        player.sendMessage("\u00a77Batalhas: \u00a7e" + battles);
        player.sendMessage("\u00a77K/D: \u00a7b" + String.format("%.2f", kd));
    }

    private void handleHolograma(Player player, String[] args) {
        if (!player.hasPermission("duel.admin")) {
            player.sendMessage("\u00a7cSem permiss\u00e3o!");
            return;
        }
        if (args.length < 2) {
            player.sendMessage("\u00a7cUse: /duelo holograma criar|remover|atualizar");
            return;
        }
        switch (args[1].toLowerCase()) {
            case "criar": {
                this.hologramManager.create(player);
                player.sendMessage("\u00a7aHolograma de ranking criado!");
                break;
            }
            case "remover": {
                if (args.length >= 3 && (args[2].equalsIgnoreCase("olhando") || args[2].equalsIgnoreCase("look"))) {
                    RayTraceResult result = player.getWorld().rayTraceEntities(
                        player.getEyeLocation(), player.getEyeLocation().getDirection(), 10.0, 0.5,
                        e -> e instanceof ArmorStand
                    );
                    if (result != null && result.getHitEntity() instanceof ArmorStand) {
                        ArmorStand target = (ArmorStand) result.getHitEntity();
                        this.hologramManager.removeLine(target);
                        player.sendMessage("\u00a7aHolograma removido!");
                    } else {
                        player.sendMessage("\u00a7cNenhum holograma encontrado!");
                    }
                } else {
                    this.hologramManager.delete();
                    player.sendMessage("\u00a7cHolograma removido!");
                }
                break;
            }
            case "atualizar": {
                this.hologramManager.update();
                player.sendMessage("\u00a7aHolograma atualizado!");
                break;
            }
            default: {
                player.sendMessage("\u00a7cUse: /duelo holograma criar|remover|atualizar");
            }
        }
    }

    private void handleResetPlayer(Player player, String[] args) {
        Player target = player;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage("\u00a7cJogador n\u00e3o encontrado!");
                return;
            }
            if (!player.hasPermission("duel.admin")) {
                player.sendMessage("\u00a7cSem permiss\u00e3o para resetar outros jogadores!");
                return;
            }
        }
        target.setWalkSpeed(0.2f);
        target.setFlySpeed(0.1f);
        target.setAllowFlight(false);
        target.setFlying(false);
        target.setGameMode(org.bukkit.GameMode.SURVIVAL);
        target.sendMessage("\u00a7aSeu estado foi resetado!");
        if (!player.equals(target)) {
            player.sendMessage("\u00a7aEstado de " + target.getName() + " resetado!");
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage("\u00a76\u00a7l=== DUELO 1v1 ===");
        player.sendMessage("\u00a7e/duelo <jogador> \u00a77- Desafiar jogador");
        player.sendMessage("\u00a7e/duelo aceitar <jogador> \u00a77- Aceitar desafio");
        player.sendMessage("\u00a7e/duelo recusar <jogador> \u00a77- Recusar desafio");
        player.sendMessage("\u00a7e/duelo sair \u00a77- Sair da partida");
        player.sendMessage("\u00a7e/duelo cancelar \u00a77- Sair da fila de duelos");
        player.sendMessage("\u00a7e/duelo stats [jogador] \u00a77- Ver estat\u00edsticas");
        if (player.hasPermission("duel.admin")) {
            player.sendMessage("\u00a7e/duelo setpos <pos1|pos2|lobby> \u00a77- Definir posi\u00e7\u00f5es da arena");
            player.sendMessage("\u00a7e/duelo holograma criar|remover|atualizar \u00a77- Gerenciar holograma");
            player.sendMessage("\u00a7e/duelo stats resetar <jogador> \u00a77- Resetar estat\u00edsticas");
            player.sendMessage("\u00a7e/duelo resetar [jogador] \u00a77- Resetar estado do jogador (travaram)");
        }
        player.sendMessage("\u00a7e/duelo resetar \u00a77- Resetar seu estado (se ficou travado)");
    }
}
