package com.makrit;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class IrcServer implements Runnable {
    private final int port;
    private final Set<ClientHandler> clients = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final MessageStore messageStore;

    public IrcServer(int port, MessageStore messageStore) {
        this.port = port;
        this.messageStore = messageStore;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("IRC Server starting on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    public void sendMessageToChannel(String channel, String message, ClientHandler sender) {
        String senderNick = (sender != null) ? sender.getNick() : "SystemBot";

        for (ClientHandler client : clients) {
            if (client != sender && client.isInChannel(channel)) {
                client.sendMessage(":" + senderNick + " PRIVMSG " + channel + " :" + message);
            }
        }

        messageStore.addMessage(channel, senderNick, message);
    }

    public void sendMessageToChannel(String channel, String message, String senderNick) {
        for (ClientHandler client : clients) {
            if (client.isInChannel(channel)) {
                client.sendMessage(":" + senderNick + " PRIVMSG " + channel + " :" + message);
            }
        }
        messageStore.addMessage(channel, senderNick, message);
    }

    public Set<ClientHandler> getClients() {
        return clients;
    }

    public MessageStore getMessageStore() {
        return messageStore;
    }
}
