package com.chatapplication.websocketconfig;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
@Service
public class WebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private static final String TOPIC = "SpringChatMessage";
    private final Map<String, WebSocketSession> onlineUsers = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToUsername = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("New WebSocket connection established: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        JsonNode json = mapper.readTree(message.getPayload());
        String type = json.get("type").asText();

        if (type.equals("register")) {
            String username = json.get("username").asText();
            onlineUsers.put(username, session);
            sessionToUsername.put(session.getId(), username);
            System.out.println(username + " is now online.");
            sendUserListToAll();  // notify others
        }

        if (type.equals("message")) {
            kafkaTemplate.send(TOPIC, message.getPayload());
        }
    }

    @KafkaListener(topics = TOPIC, groupId = "chat-group")
    public void listenFromKafka(String kafkaMessage) throws IOException {
        JsonNode json = mapper.readTree(kafkaMessage);

        String to = json.get("to").asText();
        String from = json.get("from").asText();
        String content = json.get("content").asText();

        WebSocketSession toSession = onlineUsers.get(to);
        if (toSession != null && toSession.isOpen()) {
            ObjectNode outMsg = mapper.createObjectNode();
            outMsg.put("from", from);
            outMsg.put("content", content);
            toSession.sendMessage(new TextMessage(outMsg.toString()));
        } 
    }

    private void sendUserListToAll() throws IOException {
        ObjectNode userListMsg = mapper.createObjectNode();
        userListMsg.put("type", "userList");

        ArrayNode arrayNode = mapper.createArrayNode();
        for (String username : onlineUsers.keySet()) {
            arrayNode.add(username);
        }

        userListMsg.set("users", arrayNode);

        String msg = userListMsg.toString();
        for (WebSocketSession session : onlineUsers.values()) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(msg));
            }
        }
    }


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws IOException {
        String user = sessionToUsername.remove(session.getId());
        if (user != null) {
            onlineUsers.remove(user);
            System.out.println(user + " went offline.");
            sendUserListToAll();  // notify others
        }
    }
}

