package com.dziubek.backendmanager;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;

public class BackendManagerPlugin extends JavaPlugin {

    private VanishManager vanish;
    private ControlSocketServer socketServer;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        vanish = new VanishManager(this);

        Messenger messenger = getServer().getMessenger();
        messenger.registerIncomingPluginChannel(this, ControlChannel.CHANNEL, new ControlMessageListener(this, vanish));

        getServer().getPluginManager().registerEvents(new VanishJoinQuitListener(vanish), this);

        int offset = getConfig().getInt("control-port-offset", 2000);
        String secret = getConfig().getString("control-secret", "zmien-to-haslo");
        int socketPort = getServer().getPort() + offset;
        socketServer = new ControlSocketServer(this, socketPort, secret, vanish);
        socketServer.start();

        getLogger().info("BackendManager włączony - nasłuchuje na " + ControlChannel.CHANNEL);
    }

    @Override
    public void onDisable() {
        if (socketServer != null) {
            socketServer.stop();
        }
    }

    public VanishManager getVanish() {
        return vanish;
    }
}
