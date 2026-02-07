package com.makrit;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {

        MessageStore messageStore = new MessageStore();

        int ircPort = 6667;
        IrcServer ircServer = new IrcServer(ircPort, messageStore);
        new Thread(ircServer).start();

        int apiPort = 8080;
        ApiServer apiServer = new ApiServer(apiPort, ircServer);
        try {
            apiServer.start();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to start API server");
        }

        System.out.println("IRC Server running on port " + ircPort);
        System.out.println("API Server running on port " + apiPort);
    }
}
