package com.makrit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageStore {
    private final Map<String, List<Message>> messages = new ConcurrentHashMap<>();

    public void addMessage(String channel, String sender, String content) {
        messages.computeIfAbsent(channel, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(new Message(sender, content, System.currentTimeMillis()));
    }

    public List<Message> getMessages(String channel) {
        return messages.getOrDefault(channel, Collections.emptyList());
    }

    public static class Message {
        private final String sender;
        private final String content;
        private final long timestamp;

        public Message(String sender, String content, long timestamp) {
            this.sender = sender;
            this.content = content;
            this.timestamp = timestamp;
        }

        public String getSender() {
            return sender;
        }

        public String getContent() {
            return content;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
