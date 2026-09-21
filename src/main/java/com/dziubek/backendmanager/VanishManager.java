package com.dziubek.backendmanager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Trzyma graczy ukrytych sygnałem VANISH_ON z proxy (/vanish w mban-plugin-bungeecord) i naprawdę
 * chowa ich (hidePlayer) przed każdym graczem na tym serwerze, który NIE ma uprawnienia
 * "admin.wholeserver" - administratorzy z tym uprawnieniem widzą zvanishowanych normalnie.
 */
public class VanishManager {

    private static final String BYPASS_PERMISSION = "admin.wholeserver";

    private final BackendManagerPlugin plugin;
    private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();

    public VanishManager(BackendManagerPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isVanished(UUID uuid) {
        return vanished.contains(uuid);
    }

    /** VANISH_ON/OFF z proxy - aktualizuje stan i od razu chowa/pokazuje gracza wszystkim online (poza adminami). */
    public void setVanished(UUID uuid, boolean vanish) {
        if (vanish) {
            vanished.add(uuid);
        } else {
            vanished.remove(uuid);
        }
        Player target = Bukkit.getPlayer(uuid);
        if (target == null) {
            return;
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(target)) {
                applyVisibility(viewer, target, vanish);
            }
        }
    }

    /** Nowo dołączający gracz domyślnie widzi wszystkich - trzeba mu schować już zvanishowanych (chyba że ma bypass). */
    public void applyToNewViewer(Player viewer) {
        if (viewer.hasPermission(BYPASS_PERMISSION)) {
            return;
        }
        for (UUID uuid : vanished) {
            Player hidden = Bukkit.getPlayer(uuid);
            if (hidden != null && !hidden.equals(viewer)) {
                viewer.hidePlayer(plugin, hidden);
            }
        }
    }

    public void clear(UUID uuid) {
        vanished.remove(uuid);
    }

    private void applyVisibility(Player viewer, Player target, boolean vanish) {
        if (viewer.hasPermission(BYPASS_PERMISSION)) {
            viewer.showPlayer(plugin, target);
            return;
        }
        if (vanish) {
            viewer.hidePlayer(plugin, target);
        } else {
            viewer.showPlayer(plugin, target);
        }
    }
}
