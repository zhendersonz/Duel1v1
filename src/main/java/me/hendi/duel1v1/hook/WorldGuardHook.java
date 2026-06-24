package me.hendi.duel1v1.hook;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.protection.events.DisallowedPVPEvent;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import me.hendi.duel1v1.manager.DuelManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

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

    public static Listener createPvPListener(DuelManager duelManager) {
        return new WorldGuardPvPListener(duelManager);
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

    private record WorldGuardPvPListener(DuelManager duelManager) implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onDisallowedPvP(DisallowedPVPEvent event) {
            if (duelManager.isOpponent(event.getAttacker(), event.getDefender())) {
                event.setCancelled(true);
            }
        }
    }
}
