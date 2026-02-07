package com.makrit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final IrcServer server;
    private final Set<String> channels = new HashSet<>();
    private String nick;
    private String user;
    private OutputStream out;
    private boolean isRegistered = false;

    public ClientHandler(Socket socket, IrcServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = socket.getOutputStream();


            // sendMessage("NOTICE AUTH :*** Checking Ident");

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("Received: " + line);
                handleCommand(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            server.removeClient(this);
            try {
                socket.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private void handleCommand(String line) {
        String[] parts = line.split(" ", 2);
        String command = parts[0].toUpperCase();
        String params = parts.length > 1 ? parts[1] : "";

        switch (command) {
            case "NICK":
                if (params.isEmpty())
                    return;
                this.nick = params.split(" ")[0];
                checkRegistration();
                break;
            case "USER":
                if (params.isEmpty())
                    return;

                this.user = params.split(" ")[0];
                checkRegistration();
                break;
            case "PING":
                sendMessage("PONG :" + params);
                break;
            case "JOIN":
                if (!isRegistered) {
                    sendMessage("451 :You have not registered");
                    return;
                }
                String channel = params.trim();
                joinChannel(channel);
                break;
            case "PRIVMSG":
                if (!isRegistered)
                    return;
                handlePrivMsg(params);
                break;
            case "QUIT":
                try {
                    socket.close();
                } catch (IOException e) {
                }
                break;
            default:
                // Unknown command
                break;
        }
    }

    private void checkRegistration() {
        if (nick != null && user != null && !isRegistered) {
            isRegistered = true;
            sendMessage("001 " + nick + " :Welcome to the SimpleIRC Network " + nick);
            sendMessage("002 " + nick + " :Your host is generic, running version 1.0");
            sendMessage("003 " + nick + " :This server was created now");
            sendMessage("004 " + nick + " generic 1.0 i wops");
        }
    }

    private void joinChannel(String channel) {
        channels.add(channel);

        sendMessage(":" + nick + "!" + user + "@localhost JOIN :" + channel);

        sendMessage("332 " + nick + " " + channel + " :Welcome to " + channel);


        StringBuilder names = new StringBuilder();
        for (ClientHandler c : server.getClients()) {
            if (c.isInChannel(channel)) {
                names.append(c.getNick()).append(" ");
            }
        }
        sendMessage("353 " + nick + " = " + channel + " :" + names.toString().trim());
        sendMessage("366 " + nick + " " + channel + " :End of /NAMES list.");
    }

    private void handlePrivMsg(String params) {

        int colonIdx = params.indexOf(":");
        if (colonIdx == -1) {

            String[] parts = params.split(" ", 2);
            if (parts.length < 2)
                return;
            String target = parts[0];
            String message = parts[1];
            server.sendMessageToChannel(target, message, this);
            return;
        }

        String target = params.substring(0, colonIdx).trim();
        String message = params.substring(colonIdx + 1);


        server.sendMessageToChannel(target, message, this);
    }

    public void sendMessage(String msg) {
        if (out == null)
            return;
        try {
            out.write((msg + "\r\n").getBytes());
            out.flush();
        } catch (IOException e) {
            // client disconnected probably
        }
    }

    public boolean isInChannel(String channel) {
        return channels.contains(channel);
    }

    public String getNick() {
        return nick;
    }
}
