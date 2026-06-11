package com.arguewithstranger.controller;

import com.arguewithstranger.dto.response.MessageResponse;
import com.arguewithstranger.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Handles chat history retrieval over REST.
 *
 * GET /debates/{id}/messages — returns full chat history
 *
 * Real-time messaging is handled by ChatWebSocketController,
 * not here. This endpoint exists purely for loading history
 * when a user joins or rejoins a debate room.
 */
@Slf4j
@RestController
@RequestMapping("/debates")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /**
     * GET /debates/{id}/messages
     *
     * Returns the complete ordered chat history for a debate.
     * Called when a user loads the debate room to populate
     * the chat window before connecting to the WebSocket.
     *
     * Available for all debate statuses — history is readable
     * even after a debate is CLOSED.
     *
     * @param id the debate ID
     * @return ordered list of MessageResponse (oldest first)
     */
    @GetMapping("/{id}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable Long id) {

        log.debug("Chat history requested for debate id={}", id);

        return ResponseEntity.ok(
                messageService.getMessageHistory(id));
    }
}