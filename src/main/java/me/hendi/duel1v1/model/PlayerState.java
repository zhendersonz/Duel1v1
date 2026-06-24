package me.hendi.duel1v1.model;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public record PlayerState(
    ItemStack[] inventory,
    ItemStack[] armor,
    ItemStack offHandItem,
    Location location,
    GameMode gameMode,
    double health,
    int foodLevel,
    int level,
    float exp,
    float walkSpeed,
    float flySpeed,
    boolean allowFlight,
    boolean flying
) {
}