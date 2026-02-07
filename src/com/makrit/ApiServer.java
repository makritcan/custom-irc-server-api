package com.makrit;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ApiServer {
    private final int port;
    private final IrcServer ircServer;
    private static final String ADMIN_PASS = "1234"; // Basitlik için hardcoded

    public ApiServer(int port, IrcServer ircServer) {
        this.port = port;
        this.ircServer = ircServer;
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/send", new SendHandler());
        server.createContext("/history", new HistoryHandler());
        server.createContext("/auth", new AuthHandler());
        server.createContext("/", new StaticFileHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("API Server starting on port " + port);
    }

    // Helper to read input stream fully
    private String readRequestBody(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    class AuthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("POST".equals(t.getRequestMethod())) {
                String body = readRequestBody(t.getRequestBody());
                String pass = extractJsonValue(body, "password");

                if (ADMIN_PASS.equals(pass)) {
                    sendResponse(t, 200, "{\"status\":\"ok\"}");
                } else {
                    sendResponse(t, 401, "{\"status\":\"error\"}");
                }
            } else {
                sendResponse(t, 405, "Method Not Allowed");
            }
        }
    }

    class SendHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("POST".equals(t.getRequestMethod())) {
                String body = readRequestBody(t.getRequestBody());
                String channel = extractJsonValue(body, "channel");
                String message = extractJsonValue(body, "message");
                String sender = extractJsonValue(body, "sender");

                // Anonim gönderim için varsayılan
                if (sender == null || sender.isEmpty())
                    sender = "Anonymous";

                if (channel != null && message != null) {
                    ircServer.sendMessageToChannel(channel, message, sender);
                    sendResponse(t, 200, "Sent");
                } else {
                    sendResponse(t, 400, "Missing channel or message");
                }
            } else {
                sendResponse(t, 405, "Method Not Allowed");
            }
        }
    }

    class HistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("GET".equals(t.getRequestMethod())) {
                String query = t.getRequestURI().getQuery();
                String channel = null;
                if (query != null) {
                    for (String param : query.split("&")) {
                        if (param.startsWith("channel=")) {
                            channel = param.substring(8);
                            break;
                        }
                    }
                }

                if (channel != null) {
                    if (!channel.startsWith("#"))
                        channel = "#" + channel;

                    List<MessageStore.Message> history = ircServer.getMessageStore().getMessages(channel);
                    StringBuilder json = new StringBuilder("[");
                    synchronized (history) {
                        for (int i = 0; i < history.size(); i++) {
                            MessageStore.Message m = history.get(i);
                            json.append(String.format("{\"sender\":\"%s\",\"content\":\"%s\"}",
                                    escapeJson(m.getSender()),
                                    escapeJson(m.getContent())));
                            if (i < history.size() - 1)
                                json.append(",");
                        }
                    }
                    json.append("]");

                    t.getResponseHeaders().add("Content-Type", "application/json");
                    sendResponse(t, 200, json.toString());
                } else {
                    sendResponse(t, 400, "Missing channel param");
                }
            } else {
                sendResponse(t, 405, "Method Not Allowed");
            }
        }
    }

    class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String path = t.getRequestURI().getPath();
            if ("/".equals(path))
                path = "/index.html";

            File file = new File("web" + path);

            if (file.exists() && !file.isDirectory()) {
                String mimeType = "text/plain";
                if (path.endsWith(".html"))
                    mimeType = "text/html";
                else if (path.endsWith(".css"))
                    mimeType = "text/css";
                else if (path.endsWith(".js"))
                    mimeType = "application/javascript";

                t.getResponseHeaders().add("Content-Type", mimeType);
                t.sendResponseHeaders(200, file.length());

                try (OutputStream os = t.getResponseBody(); FileInputStream fs = new FileInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int count;
                    while ((count = fs.read(buffer)) >= 0) {
                        os.write(buffer, 0, count);
                    }
                }
            } else {
                String response = "404 Not Found";
                t.sendResponseHeaders(404, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    private void sendResponse(HttpExchange t, int code, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        t.sendResponseHeaders(code, bytes.length);
        OutputStream os = t.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1)
            return null;
        start += search.length();

        while (start < json.length()
                && (json.charAt(start) == ' ' || json.charAt(start) == '"' || json.charAt(start) == ':')) {
            start++;
        }

        int end = json.indexOf("\"", start);
        if (end == -1)
            return null;

        return json.substring(start, end);
    }

    private String escapeJson(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
