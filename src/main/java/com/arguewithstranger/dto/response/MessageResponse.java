package com.arguewithstranger.dto.response;

import com.arguewithstranger.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response for GET /debates/{id}/messages
 * Also broadcast over WebSocket to /topic/debate/{debateId}
 *
 * This single DTO serves both the REST history endpoint and
 * the real-time WebSocket broadcast. When a message is sent
 * via WebSocket, the server persists it, converts it to this
 * DTO, and broadcasts the DTO — not the raw entity.
 *
 * senderSide indicates whether the sender was on the FAVOR
 * or AGAINST side, allowing the frontend to render messages
 * on the correct side of the chat UI without the client
 * needing to know the debate's participant map.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    private Long id;
    private Long debateId;
    private Long senderId;
    private String senderUsername;

    // "FAVOR", "AGAINST", or "SPECTATOR" — for UI positioning
    private String senderSide;

    private String content;
    private LocalDateTime timestamp;

    /**
     * Factory method: converts a Message entity to a MessageResponse.
     * The senderSide is not derivable from the Message alone —
     * it must be passed in by the service which has access to
     * the parent Debate.
     *
     * @param message    the persisted message entity
     * @param senderSide which side the sender is on
     * @return MessageResponse safe for REST and WebSocket serialization
     */
    public static MessageResponse fromEntity(
            Message message, String senderSide) {
        return MessageResponse.builder()
                .id(message.getId())
                .debateId(message.getDebate().getId())
                .senderId(message.getSender().getId())
                .senderUsername(message.getSender().getUsername())
                .senderSide(senderSide)
                .content(message.getContent())
                .timestamp(message.getTimestamp())
                .build();
    }
}