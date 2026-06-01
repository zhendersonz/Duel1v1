package me.hendi.duel1v1.stats;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public class StatsManager {
    private final Plugin plugin;
    private FileConfiguration stats;
    private File statsFile;

    public StatsManager(Plugin plugin) {
        this.plugin = plugin;
        this.load();
    }

    private void load() {
        this.statsFile = new File(this.plugin.getDataFolder(), "stats.yml");
        if (!this.statsFile.exists()) {
            try {
                this.statsFile.getParentFile().mkdirs();
                this.statsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.stats = YamlConfiguration.loadConfiguration(this.statsFile);
    }

    public void save() {
        try {
            this.stats.save(this.statsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addKill(UUID uuid) {
        String path = uuid.toString() + ".kills";
        this.stats.set(path, this.stats.getInt(path) + 1);
        this.save();
    }

    public void addDeath(UUID uuid) {
        String path = uuid.toString() + ".deaths";
        this.stats.set(path, this.stats.getInt(path) + 1);
        this.save();
    }

    public void addBattle(UUID uuid) {
        String path = uuid.toString() + ".battles";
        this.stats.set(path, this.stats.getInt(path) + 1);
        this.save();
    }

    public int getKills(UUID uuid) {
        return this.stats.getInt(uuid.toString() + ".kills");
    }

    public int getDeaths(UUID uuid) {
        return this.stats.getInt(uuid.toString() + ".deaths");
    }

    public int getBattles(UUID uuid) {
        return this.stats.getInt(uuid.toString() + ".battles");
    }

    public double getKD(UUID uuid) {
        int deaths = this.getDeaths(uuid);
        if (deaths == 0) {
            return this.getKills(uuid);
        }
        return (double) this.getKills(uuid) / (double) deaths;
    }

    public void reset(UUID uuid) {
        this.stats.set(uuid.toString(), null);
        this.save();
    }

    public List<UUID> getTopPlayers(int limit) {
        return this.stats.getKeys(false).stream()
            .map(s -> {
                try {
                    return UUID.fromString(s);
                } catch (Exception e) {
                    return null;
                }
            })
            .filter(u -> u != null)
            .sorted((a, b) -> {
                double kdA = this.getKD(a);
                double kdB = this.getKD(b);
                int cmp = Double.compare(kdB, kdA);
                if (cmp != 0) return cmp;
                return Integer.compare(this.getKills(b), this.getKills(a));
            })
            .limit(limit)
            .collect(Collectors.toList());
    }
}
