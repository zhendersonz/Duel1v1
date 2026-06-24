package me.hendi.duel1v1.hook;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;

public class WorldGuardHook {

    private static boolean enabled = false;

    public static void init() {
        try {
            enabled = WorldGuard.getInstance() != null;
        } catch (NoClassDefFoundError | Exception e) {
            enabled = false;
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean isPvpAllowed(Location location) {
        if (!enabled || location == null || location.getWorld() == null) return true;
        try {
            RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            StateFlag.State state = query.queryState(
                BukkitAdapter.adapt(location),
                (LocalPlayer) null,
                Flags.PVP
            );
            return state != StateFlag.State.DENY;
        } catch (Exception e) {
            return true;
        }
    }
}
