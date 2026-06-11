package com.arguewithstranger.service;

import com.arguewithstranger.dto.request.SendMessageRequest;
import com.arguewithstranger.dto.response.MessageResponse;
import com.arguewithstranger.entity.Debate;
import com.arguewithstranger.entity.Message;
import com.arguewithstranger.entity.User;
import com.arguewithstranger.exception.DebateException;
import com.arguewithstranger.exception.ResourceNotFoundException;
import com.arguewithstranger.repository.DebateRepository;
import com.arguewithstranger.repository.MessageRepository;
import com.arguewithstranger.repository.UserRepository;
import com.arguewithstranger.util.DebateStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Handles message persistence and chat history retrieval.
 *
 * Business rules enforced:
 *   - Messages can only be sent in ONGOING debates
 *   - Only the two debaters (FAVOR / AGAINST) can send messages
 *   - Spectators can read but never send
 *   - Messages are never deleted — full history is always available
 *
 * This service is called from two places:
 *   1. ChatWebSocketController — for real-time messages
 *   2. MessageController       — for loading chat history (REST)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final DebateRepository  debateRepository;
    private final UserRepository    userRepository;

    // ── Send Message ───────────────────────────────────────────

    /**
     * Persists a message sent by a debater and returns the
     * MessageResponse that will be broadcast to all subscribers.
     *
     * Rules enforced:
     *   1. Debate must be ONGOING (not OPEN or CLOSED)
     *   2. Sender must be a debater (FAVOR or AGAINST user)
     *      — spectators are rejected here
     *
     * The message is persisted BEFORE broadcasting — if the DB
     * write fails, nothing is broadcast. This guarantees that
     * the WebSocket message and the persisted record are always
     * consistent.
     *
     * @param request  debateId and message content
     * @param username the authenticated sender's username
     * @return MessageResponse ready for WebSocket broadcast
     */
    @Transactional
    public MessageResponse sendMessage(
            SendMessageRequest request,
            String username) {

        Debate debate = findDebateById(request.getDebateId());
        User   sender = loadUser(username);

        // Rule 1: Debate must be ONGOING
        if (debate.getStatus() != DebateStatus.ONGOING) {
            throw DebateException.messagingNotAllowed();
        }

        // Rule 2: Sender must be one of the two debaters
        if (!debate.isDebater(sender)) {
            throw DebateException.notADebater();
        }

        // Persist the message
        Message message = Message.builder()
                .debate(debate)
                .sender(sender)
                .content(request.getContent().trim())
                .build();

        Message saved = messageRepository.save(message);

        log.debug("Message persisted: debateId={}, sender={}, id={}",
                request.getDebateId(), username, saved.getId());

        // Determine which side the sender is on for UI positioning
        String senderSide = resolveSenderSide(debate, sender);

        return MessageResponse.fromEntity(saved, senderSide);
    }

    // ── Load History ───────────────────────────────────────────

    /**
     * Returns the complete chat history for a debate in
     * chronological order.
     *
     * Called when a user loads or rejoins a debate room.
     * The JOIN FETCH in MessageRepository ensures all sender
     * details are loaded in a single query — no N+1.
     *
     * Available for any debate status — history is readable
     * even after a debate is CLOSED.
     *
     * @param debateId the debate whose history to load
     * @return ordered list of MessageResponse objects
     */
    @Transactional(readOnly = true)
    public List<MessageResponse> getMessageHistory(Long debateId) {

        // Verify debate exists before loading messages
        Debate debate = findDebateById(debateId);

        List<Message> messages = messageRepository
                .findByDebateIdOrderByTimestampAsc(debateId);

        return messages.stream()
                .map(msg -> MessageResponse.fromEntity(
                        msg,
                        resolveSenderSide(debate, msg.getSender())
                ))
                .toList();
    }

    // ── Private Helpers ────────────────────────────────────────

    /**
     * Determines which side a user is on in a debate.
     *
     * Returns:
     *   "FAVOR"    — if the user is the favor debater
     *   "AGAINST"  — if the user is the against debater
     *   "SPECTATOR"— if the user is watching (should not
     *                happen for messages, but safe default)
     *
     * Used by the frontend to render messages on the correct
     * side of the chat UI (left = FAVOR, right = AGAINST).
     */
    private String resolveSenderSide(Debate debate, User sender) {
        if (debate.getFavorUser() != null
                && debate.getFavorUser().getId()
                .equals(sender.getId())) {
            return "FAVOR";
        }
        if (debate.getAgainstUser() != null
                && debate.getAgainstUser().getId()
                .equals(sender.getId())) {
            return "AGAINST";
        }
        return "SPECTATOR";
    }

    /**
     * Loads a Debate by ID or throws ResourceNotFoundException.
     */
    private Debate findDebateById(Long debateId) {
        return debateRepository.findById(debateId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Debate", "id", debateId));
    }

    /**
     * Loads a User by username or throws ResourceNotFoundException.
     */
    private User loadUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "username", username));
    }
}