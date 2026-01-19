package com.example.notificationservice.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketHandler.class);

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("Новое подключение WebSocket: id={}, всего активных: {}",
                session.getId(), sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        log.debug("Сообщение от {}: {}", session.getId(), payload);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("Отключение WebSocket: id={}, причина={}, осталось: {}",
                session.getId(), status.getReason(), sessions.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("Ошибка транспорта для сессии {}: {}",
                session.getId(), exception.getMessage());
        sessions.remove(session);
    }

    public void broadcast(String message) {
        TextMessage textMessage = new TextMessage(message);
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            } catch (IOException e) {
                log.warn("Ошибка отправки в сессию {}: {}", session.getId(), e.getMessage());
                sessions.remove(session);
            }
        }
    }

    public int getActiveConnections() {
        return sessions.size();
    }
}
