package com.dziubek.backendmanager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Odbiera polecenia proxy (BanManager, /vanish i /shutdown) kanałem "banmanager:control":
 * VANISH_ON/OFF - realne ukrycie gracza (VanishManager), SHUTDOWN - realne wyłączenie procesu.
 */
public class ControlMessageListener implements PluginMessageListener {

    private final BackendManagerPlugin plugin;
    private final VanishManager vanish;

    public ControlMessageListener(BackendManagerPlugin plugin, VanishManager vanish) {
        this.plugin = plugin;
        this.vanish = vanish;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(ControlChannel.CHANNEL)) {
            return;
        }
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            String action = in.readUTF();
            switch (action) {
                case "VANISH_ON" -> handleVanish(in, true);
                case "VANISH_OFF" -> handleVanish(in, false);
                case "SHUTDOWN" -> handleShutdown(in);
                default -> plugin.getLogger().warning("Nieznana akcja na banmanager:control: " + action);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Błąd odczytu wiadomości banmanager:control: " + e.getMessage());
        }
    }

    private void handleVanish(DataInputStream in, boolean vanished) throws IOException {
        in.readUTF(); // nazwa gracza - nieużywana bezpośrednio, tylko dla czytelności protokołu
        UUID uuid = UUID.fromString(in.readUTF());
        vanish.setVanished(uuid, vanished);
    }

    private void handleShutdown(DataInputStream in) throws IOException {
        String reason = in.readUTF();
        plugin.getLogger().warning("Otrzymano SHUTDOWN z proxy (powód: " + reason + ") - wyłączam serwer.");
        Bukkit.getScheduler().runTaskLater(plugin, Bukkit::shutdown, 20L);
    }
}
