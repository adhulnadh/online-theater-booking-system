package com.example.onlinebooking.websocket;

import com.example.onlinebooking.dto.SeatReleaseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket handler that broadcasts seat updates to a show-specific topic (/topic/show/{showId}).
 */
@Component
public class SeatWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(SeatWebSocketHandler.class);
    public static final String SHOW_TOPIC_PREFIX = "/topic/show/";

    private final SimpMessagingTemplate messagingTemplate;

    public SeatWebSocketHandler(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Requirement 2 & 3: Broadcasts an expired or released seat to the show-specific topic.
     *
     * @param showId     the show ID topic target (e.g. /topic/show/1)
     * @param seatId     the seat ID that just became available
     * @param seatNumber the seat number (e.g. A3)
     */
    public void broadcastSeatReleased(Long showId, Long seatId, String seatNumber) {
        if (showId == null) {
            log.warn("Cannot broadcast seat release: showId is null for seatId: {}", seatId);
            return;
        }

        String destination = SHOW_TOPIC_PREFIX + showId;
        SeatReleaseMessage message = SeatReleaseMessage.of(showId, seatId, seatNumber);

        log.info("Broadcasting seat release to topic {}: seatId={}, seatNumber={}",
                destination, seatId, seatNumber);

        messagingTemplate.convertAndSend(destination, message);
    }

    /**
     * Broadcasts when a seat is locked in real-time to the show topic.
     */
    public void broadcastSeatLocked(Long showId, Long seatId, String seatNumber, String lockedUntil) {
        if (showId == null) return;

        String destination = SHOW_TOPIC_PREFIX + showId;
        Map<String, Object> payload = new HashMap<>();
        payload.put("showId", showId);
        payload.put("seatId", seatId);
        payload.put("seatNumber", seatNumber);
        payload.put("status", "LOCKED");
        payload.put("action", "SEAT_LOCKED");
        payload.put("lockedUntil", lockedUntil);

        messagingTemplate.convertAndSend(destination, payload);
    }
}
