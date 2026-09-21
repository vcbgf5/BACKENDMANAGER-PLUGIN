package com.dziubek.backendmanager;

import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Awaryjny transport dla poleceń proxy, gdy na tym serwerze nie ma ani jednego gracza - zwykły
 * plugin messaging (ControlMessageListener) wymaga połączenia gracza jako transportu, więc bez
 * niego np. realny SHUTDOWN wydany na pustym serwerze nigdy by nie dotarł. Proxy wtedy łączy się
 * wprost gniazdem TCP na porcie gry + control-port-offset, podając control-secret z config.yml
 * (musi być identyczny po obu stronach - inaczej połączenie jest odrzucane).
 *
 * Protokół (jedno połączenie, linia po linii, UTF-8): sekret\n polecenie\n, gdzie polecenie to
 * "SHUTDOWN\t<powód>" albo "VANISH_ON\t<uuid>" / "VANISH_OFF\t<uuid>". Odpowiedź: "OK\n" albo "ERR\n".
 */
public class ControlSocketServer {

    private final BackendManagerPlugin plugin;
    private final int port;
    private final String secret;
    private final VanishManager vanish;

    private ServerSocket serverSocket;
    private Thread thread;
    private volatile boolean running;

    public ControlSocketServer(BackendManagerPlugin plugin, int port, String secret, VanishManager vanish) {
        this.plugin = plugin;
        this.port = port;
        this.secret = secret;
        this.vanish = vanish;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            plugin.getLogger().warning("Nie udało się otworzyć gniazda kontrolnego na porcie " + port + ": " + e.getMessage());
            return;
        }
        running = true;
        thread = new Thread(this::acceptLoop, "BackendManager-ControlSocket");
        thread.setDaemon(true);
        thread.start();
        plugin.getLogger().info("Gniazdo kontrolne nasłuchuje na porcie " + port
                + " (awaryjny transport dla proxy, gdy serwer jest pusty).");
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                handle(socket);
            } catch (IOException e) {
                if (running) {
                    plugin.getLogger().warning("Błąd gniazda kontrolnego: " + e.getMessage());
                }
            }
        }
    }

    private void handle(Socket socket) {
        try (socket) {
            socket.setSoTimeout(2000);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String receivedSecret = in.readLine();
            if (receivedSecret == null || !receivedSecret.equals(secret)) {
                return;
            }
            String line = in.readLine();
            boolean ok = line != null && execute(line);
            OutputStream out = socket.getOutputStream();
            out.write((ok ? "OK\n" : "ERR\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException ignored) {
        }
    }

    private boolean execute(String line) {
        String[] parts = line.split("\t");
        if (parts.length == 0) {
            return false;
        }
        try {
            return switch (parts[0]) {
                case "SHUTDOWN" -> {
                    String reason = parts.length > 1 ? parts[1] : "Prace techniczne";
                    plugin.getLogger().warning("Otrzymano SHUTDOWN (TCP - serwer był pusty) z proxy, powód: " + reason);
                    Bukkit.getScheduler().runTaskLater(plugin, Bukkit::shutdown, 20L);
                    yield true;
                }
                case "VANISH_ON", "VANISH_OFF" -> {
                    if (parts.length < 2) {
                        yield false;
                    }
                    UUID uuid = UUID.fromString(parts[1]);
                    boolean vanished = parts[0].equals("VANISH_ON");
                    Bukkit.getScheduler().runTask(plugin, () -> vanish.setVanished(uuid, vanished));
                    yield true;
                }
                default -> false;
            };
        } catch (RuntimeException e) {
            return false;
        }
    }
}
