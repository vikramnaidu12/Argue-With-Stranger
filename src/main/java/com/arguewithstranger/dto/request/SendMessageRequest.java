package com.arguewithstranger.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload sent over WebSocket to /app/sendMessage
 *
 * debateId is included in the message payload (not just the
 * WebSocket topic) so the server knows which debate the message
 * belongs to without relying on session state.
 *
 * This DTO is deserialized by the WebSocket message converter
 * (Jackson) rather than the REST dispatcher — validation
 * annotations are enforced manually in the controller via
 * a Validator bean, or checked at the service layer.
 */
@Data
public class SendMessageRequest {

    private Long debateId;

    @NotBlank(message = "Message content cannot be empty")
    @Size(max = 2000, message = "Message must not exceed 2000 characters")
    private String content;
}