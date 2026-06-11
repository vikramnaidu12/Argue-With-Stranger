package com.arguewithstranger.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload for POST /debates
 *
 * Used by admins (or users, depending on your business rule)
 * to create a new debate topic.
 *
 * The description is optional — a topic alone is enough to
 * start a debate, but a description gives context.
 */
@Data
public class CreateDebateRequest {

    @NotBlank(message = "Topic is required")
    @Size(min = 10, max = 255, message = "Topic must be between 10 and 255 characters")
    private String topic;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
}