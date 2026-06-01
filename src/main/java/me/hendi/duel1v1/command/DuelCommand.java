package me.hendi.duel1v1.command;

import me.hendi.duel1v1.Duel1v1;
import me.hendi.duel1v1.manager.DuelManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DuelCommand
implements CommandExecutor {
    private final Duel1v1 plugin;
    private final DuelManager duelManager;

    public DuelCommand(Duel1v1 plugin, DuelManager duelManager) {
        this.plugin = plugin;
        this.duelManager = duelManager;
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

    private void sendHelp(Player player) {
        player.sendMessage("\u00a76\u00a7l=== DUELO 1v1 ===");
        player.sendMessage("\u00a7e/duelo <jogador> \u00a77- Desafiar jogador");
        player.sendMessage("\u00a7e/duelo aceitar <jogador> \u00a77- Aceitar desafio");
        player.sendMessage("\u00a7e/duelo recusar <jogador> \u00a77- Recusar desafio");
        player.sendMessage("\u00a7e/duelo sair \u00a77- Sair da partida");
        player.sendMessage("\u00a7e/duelo cancelar \u00a77- Sair da fila de duelos");
        if (player.hasPermission("duel.admin")) {
            player.sendMessage("\u00a7e/duelo setpos <pos1|pos2|lobby> \u00a77- Definir posi\u00e7\u00f5es da arena");
        }
    }
}

