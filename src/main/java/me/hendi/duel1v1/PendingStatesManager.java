package me.hendi.duel1v1;

import me.hendi.duel1v1.model.PlayerState;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PendingStatesManager {
    private final Duel1v1 plugin;
    private final File file;
    private final Map<UUID, PlayerState> pending = new HashMap<>();

    public PendingStatesManager(Duel1v1 plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "pending.yml");
    }

    public void load() {
        if (!this.file.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(this.file);
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                ConfigurationSection section = config.getConfigurationSection(key);
                if (section != null) {
                    PlayerState state = deserialize(section);
                    this.pending.put(uuid, state);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        FileConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerState> entry : this.pending.entrySet()) {
            ConfigurationSection section = config.createSection(entry.getKey().toString());
            serialize(entry.getValue(), section);
        }
        try {
            config.save(this.file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void put(UUID uuid, PlayerState state) {
        this.pending.put(uuid, state);
        this.save();
    }

    public PlayerState remove(UUID uuid) {
        PlayerState state = this.pending.remove(uuid);
        if (state != null) {
            this.save();
        }
        return state;
    }

    public PlayerState get(UUID uuid) {
        return this.pending.get(uuid);
    }

    public boolean has(UUID uuid) {
        return this.pending.containsKey(uuid);
    }

    public void clear() {
        this.pending.clear();
        this.save();
    }

    public Map<UUID, PlayerState> getAll() {
        return new HashMap<>(this.pending);
    }

    private void serialize(PlayerState state, ConfigurationSection section) {
        ItemStack[] inv = state.inventory();
        if (inv != null) {
            for (int i = 0; i < inv.length; i++) {
                if (inv[i] != null) {
                    section.set("inventory." + i, inv[i]);
                }
            }
        }
        ItemStack[] armor = state.armor();
        if (armor != null) {
            for (int i = 0; i < armor.length; i++) {
                if (armor[i] != null) {
                    section.set("armor." + i, armor[i]);
                }
            }
        }
        if (state.offHandItem() != null) {
            section.set("offhand", state.offHandItem());
        }
        if (state.location() != null) {
            Location loc = state.location();
            section.set("location.world", loc.getWorld().getName());
            section.set("location.x", loc.getX());
            section.set("location.y", loc.getY());
            section.set("location.z", loc.getZ());
            section.set("location.yaw", loc.getYaw());
            section.set("location.pitch", loc.getPitch());
        }
        section.set("gameMode", state.gameMode().name());
        section.set("health", state.health());
        section.set("foodLevel", state.foodLevel());
        section.set("level", state.level());
        section.set("exp", state.exp());
        section.set("walkSpeed", state.walkSpeed());
        section.set("flySpeed", state.flySpeed());
        section.set("allowFlight", state.allowFlight());
        section.set("flying", state.flying());
    }

    private PlayerState deserialize(ConfigurationSection section) {
        ItemStack[] inv = new ItemStack[36];
        ConfigurationSection invSection = section.getConfigurationSection("inventory");
        if (invSection != null) {
            for (String key : invSection.getKeys(false)) {
                int slot = Integer.parseInt(key);
                if (slot >= 0 && slot < 36) {
                    inv[slot] = invSection.getItemStack(key);
                }
            }
        }
        ItemStack[] armor = new ItemStack[4];
        ConfigurationSection armorSection = section.getConfigurationSection("armor");
        if (armorSection != null) {
            for (String key : armorSection.getKeys(false)) {
                int slot = Integer.parseInt(key);
                if (slot >= 0 && slot < 4) {
                    armor[slot] = armorSection.getItemStack(key);
                }
            }
        }
        Location location = null;
        ConfigurationSection locSection = section.getConfigurationSection("location");
        if (locSection != null) {
            String world = locSection.getString("world");
            double x = locSection.getDouble("x");
            double y = locSection.getDouble("y");
            double z = locSection.getDouble("z");
            float yaw = (float) locSection.getDouble("yaw", 0);
            float pitch = (float) locSection.getDouble("pitch", 0);
            if (world != null && Bukkit.getWorld(world) != null) {
                location = new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
            }
        }
        GameMode gameMode = GameMode.valueOf(section.getString("gameMode", "SURVIVAL"));
        double health = section.getDouble("health", 20);
        int foodLevel = section.getInt("foodLevel", 20);
        int level = section.getInt("level", 0);
        float exp = (float) section.getDouble("exp", 0);
        float walkSpeed = (float) section.getDouble("walkSpeed", 0.2);
        float flySpeed = (float) section.getDouble("flySpeed", 0.1);
        boolean allowFlight = section.getBoolean("allowFlight", false);
        boolean flying = section.getBoolean("flying", false);

        ItemStack offhand = section.getItemStack("offhand");

        return new PlayerState(inv, armor, offhand, location, gameMode, health, foodLevel, level, exp, walkSpeed, flySpeed, allowFlight, flying);
    }
}