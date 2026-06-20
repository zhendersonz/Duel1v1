package me.hendi.duel1v1.stats;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import me.hendi.duel1v1.Duel1v1;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

public class HologramManager {
    private final Duel1v1 plugin;
    private final StatsManager statsManager;
    private final List<ArmorStand> lines = new ArrayList<>();
    private int updateTaskId;

    public HologramManager(Duel1v1 plugin, StatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
    }

    public void create(Player player) {
        this.remove();
        Location loc = player.getLocation().add(0.0, 1.0, 0.0);
        this.buildLines(loc);
        this.plugin.getConfig().set("hologram.world", loc.getWorld().getName());
        this.plugin.getConfig().set("hologram.x", loc.getX());
        this.plugin.getConfig().set("hologram.y", loc.getY());
        this.plugin.getConfig().set("hologram.z", loc.getZ());
        this.plugin.saveConfig();
        this.startAutoUpdate();
    }

    public void remove() {
        this.stopAutoUpdate();
        for (ArmorStand as : this.lines) {
            as.remove();
        }
        this.lines.clear();
    }

    public void delete() {
        this.remove();
        this.plugin.getConfig().set("hologram", null);
        this.plugin.saveConfig();
    }

    public void update() {
        if (this.lines.isEmpty()) {
            this.load();
            if (this.lines.isEmpty()) {
                return;
            }
        }
        List<UUID> top = this.statsManager.getTopPlayers(5);
        List<String> textLines = new ArrayList<>();
        textLines.add("\u00a76\u00a7l\u2550\u2550\u2550 \u2694 RANKING DUELOS \u2694 \u2550\u2550\u2550");
        if (top.isEmpty()) {
            textLines.add(" \u00a77Nenhum dado ainda.");
        } else {
            int i = 1;
            for (UUID uuid : top) {
                String name = Bukkit.getOfflinePlayer(uuid).getName();
                if (name == null) {
                    name = uuid.toString().substring(0, 8);
                }
                int kills = this.statsManager.getKills(uuid);
                int deaths = this.statsManager.getDeaths(uuid);
                int battles = this.statsManager.getBattles(uuid);
                double kd = this.statsManager.getKD(uuid);
                String color = (i == 1) ? "\u00a76" : "\u00a7e";
                textLines.add(color + "#" + i + " " + name + "  \u00a77Kills:\u00a7e" + kills + "  \u00a77Mortes:\u00a7e" + deaths + "  \u00a77K/D:\u00a7e" + String.format("%.1f", kd));
                ++i;
            }
        }
        int max = Math.min(textLines.size(), this.lines.size());
        for (int i = 0; i < max; ++i) {
            this.lines.get(i).setCustomName(textLines.get(i));
        }
        if (textLines.size() > this.lines.size()) {
            Location base = this.lines.isEmpty() ? null : this.lines.get(0).getLocation().clone();
            if (base == null) {
                return;
            }
            for (int i = this.lines.size(); i < textLines.size(); ++i) {
                Location loc = base.clone().subtract(0.0, i * 0.25, 0.0);
                ArmorStand as = this.spawnLine(loc, textLines.get(i));
                this.lines.add(as);
            }
        }
        if (textLines.size() < this.lines.size()) {
            while (this.lines.size() > textLines.size()) {
                ArmorStand extra = this.lines.remove(this.lines.size() - 1);
                extra.remove();
            }
        }
    }

    private ArmorStand spawnLine(Location loc, String text) {
        return loc.getWorld().spawn(loc, ArmorStand.class, as -> {
            as.setVisible(false);
            as.setAI(false);
            as.setGravity(false);
            as.setInvulnerable(true);
            as.setCollidable(false);
            as.setSilent(true);
            as.setCustomNameVisible(true);
            as.setCustomName(text);
        });
    }

    private void buildLines(Location base) {
        List<String> textLines = new ArrayList<>();
        textLines.add("\u00a76\u00a7l\u2550\u2550\u2550 \u2694 RANKING DUELOS \u2694 \u2550\u2550\u2550");
        for (int i = 0; i < 5; ++i) {
            String color = (i == 0) ? "\u00a76" : "\u00a7e";
            textLines.add(color + "#" + (i + 1) + " \u00a78-  \u00a77Kills:\u00a78-  \u00a77Mortes:\u00a78-  \u00a77K/D:\u00a78-");
        }
        int idx = 0;
        for (String line : textLines) {
            Location loc = base.clone().subtract(0.0, idx * 0.25, 0.0);
            ArmorStand as = this.spawnLine(loc, line);
            this.lines.add(as);
            ++idx;
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
        loc.getWorld().getNearbyEntities(loc, 0.5, 1.0, 0.5).stream()
            .filter(e -> e instanceof ArmorStand)
            .forEach(e -> e.remove());
        this.buildLines(loc);
        this.update();
        this.startAutoUpdate();
    }

    private void startAutoUpdate() {
        this.stopAutoUpdate();
        this.updateTaskId = Bukkit.getScheduler().runTaskTimer(this.plugin, this::update, 600L, 600L).getTaskId();
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
