package com.dziubek.backendmanager;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Nowo dołączający gracz domyślnie widzi wszystkich - trzeba mu ręcznie schować już zvanishowanych. */
public class VanishJoinQuitListener implements Listener {

    private final VanishManager vanish;

    public VanishJoinQuitListener(VanishManager vanish) {
        this.vanish = vanish;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        vanish.applyToNewViewer(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        vanish.clear(event.getPlayer().getUniqueId());
    }
}
