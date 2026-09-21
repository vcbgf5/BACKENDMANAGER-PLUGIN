package com.dziubek.backendmanager;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;

public class BackendManagerPlugin extends JavaPlugin {

    private VanishManager vanish;

    @Override
    public void onEnable() {
        vanish = new VanishManager(this);

        Messenger messenger = getServer().getMessenger();
        messenger.registerIncomingPluginChannel(this, ControlChannel.CHANNEL, new ControlMessageListener(this, vanish));

        getServer().getPluginManager().registerEvents(new VanishJoinQuitListener(vanish), this);

        getLogger().info("BackendManager włączony - nasłuchuje na " + ControlChannel.CHANNEL);
    }

    public VanishManager getVanish() {
        return vanish;
    }
}
