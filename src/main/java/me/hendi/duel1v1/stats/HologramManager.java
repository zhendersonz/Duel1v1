package me.hendi.duel1v1.stats;

import java.util.List;
import java.util.UUID;
import me.hendi.duel1v1.Duel1v1;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

public class HologramManager {
    private final Duel1v1 plugin;
    private final StatsManager statsManager;
    private TextDisplay hologram;
    private int updateTaskId;

    public HologramManager(Duel1v1 plugin, StatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
    }

    public void create(Player player) {
        this.remove();
        Location loc = player.getLocation().subtract(0.0, 0.5, 0.0);
        this.hologram = loc.getWorld().spawn(loc, TextDisplay.class, h -> {
            h.setBillboard(Display.Billboard.CENTER);
            h.setGravity(false);
            h.setInvulnerable(true);
            h.setSeeThrough(true);
            h.setShadowed(true);
            h.setLineWidth(400);
            h.setDefaultBackground(false);
            h.setBackgroundColor(Color.fromARGB(0));
        });
        this.update();
        this.plugin.getConfig().set("hologram.world", loc.getWorld().getName());
        this.plugin.getConfig().set("hologram.x", loc.getX());
        this.plugin.getConfig().set("hologram.y", loc.getY());
        this.plugin.getConfig().set("hologram.z", loc.getZ());
        this.plugin.saveConfig();
        this.startAutoUpdate();
    }

    public void remove() {
        if (this.hologram != null) {
            this.hologram.remove();
            this.hologram = null;
        }
        this.stopAutoUpdate();
    }

    public void delete() {
        this.remove();
        this.plugin.getConfig().set("hologram", null);
        this.plugin.saveConfig();
    }

    public void update() {
        if (this.hologram == null || !this.hologram.isValid()) {
            this.load();
            if (this.hologram == null) {
                return;
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\u00a76\u00a7l\u2550\u2550\u2550 \u2694\u00a7r\u00a7e RANKING DUELOS \u00a76\u00a7l\u2694 \u2550\u2550\u2550\u00a7r\n\n");
        sb.append("\u00a76\u2694\u00a7e TOP KILLS:\n");
        this.appendTop(sb, this.statsManager.getTopKills(5));
        sb.append("\n\u00a7e\u2694\u00a7e TOP BATALHAS:\n");
        this.appendTop(sb, this.statsManager.getTopBattles(5));
        sb.append("\n\u00a7c\u2694\u00a7e TOP MORTES:\n");
        this.appendTop(sb, this.statsManager.getTopDeaths(5));
        this.hologram.text(Component.text(sb.toString()));
    }

    private void appendTop(StringBuilder sb, List<UUID> list) {
        if (list.isEmpty()) {
            sb.append(" \u00a77Nenhum dado ainda.\n");
            return;
        }
        int i = 1;
        for (UUID uuid : list) {
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            if (name == null) {
                name = uuid.toString().substring(0, 8);
            }
            int kills = this.statsManager.getKills(uuid);
            int deaths = this.statsManager.getDeaths(uuid);
            int battles = this.statsManager.getBattles(uuid);
            double kd = this.statsManager.getKD(uuid);
            sb.append(String.format(" \u00a77#%d\u00a7e %-16s \u00a7fK:\u00a7a%d \u00a7cD:%d \u00a7eB:%d \u00a7bKD:%.2f\n", i, name, kills, deaths, battles, kd));
            ++i;
        }
    }

    public void load() {
        ConfigurationSection section = this.plugin.getConfig().getConfigurationSection("hologram");
        if (section == null) {
            return;
        }
        Location loc = this.deserializeLocation(section);
        if (loc == null || loc.getWorld() == null) {
            return;
        }
        this.hologram = loc.getWorld().spawn(loc, TextDisplay.class, h -> {
            h.setBillboard(Display.Billboard.CENTER);
            h.setGravity(false);
            h.setInvulnerable(true);
            h.setSeeThrough(true);
            h.setShadowed(true);
            h.setLineWidth(400);
            h.setDefaultBackground(false);
            h.setBackgroundColor(Color.fromARGB(0));
        });
        this.update();
        this.startAutoUpdate();
    }

    private void startAutoUpdate() {
        this.stopAutoUpdate();
        this.updateTaskId = Bukkit.getScheduler().runTaskTimer(this.plugin, this::update, 0L, 600L).getTaskId();
    }

    private void stopAutoUpdate() {
        if (this.updateTaskId != 0) {
            Bukkit.getScheduler().cancelTask(this.updateTaskId);
            this.updateTaskId = 0;
        }
    }

    private Location deserializeLocation(ConfigurationSection section) {
        String world = section.getString("world");
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        return new Location(Bukkit.getWorld(world), x, y, z);
    }
}
