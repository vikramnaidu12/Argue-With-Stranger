package com.arguewithstranger.ai;

import com.arguewithstranger.entity.Message;
import com.arguewithstranger.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AIService {

    private final MessageRepository messageRepository;
    private final GeminiService geminiService;

    public AIResponse analyze(Long debateId){

        List<Message> messages =
                messageRepository
                        .findByDebateIdOrderByTimestampAsc(debateId);

        System.out.println("=================================");
        System.out.println("MESSAGE COUNT = " + messages.size());

        for (Message m : messages) {
            System.out.println(
                    m.getSender().getUsername()
                            + " : "
                            + m.getContent()
            );
        }

        System.out.println("=================================");

        String prompt =
                PromptBuilder.build(messages);

        String aiResult =
                geminiService.generate(prompt);

        System.out.println("===============");
        System.out.println(aiResult);
        System.out.println("===============");

        aiResult = aiResult
                .replace("```json", "")
                .replace("```", "")
                .trim();

        try {

            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();

            AIResponse response =
                    mapper.readValue(aiResult, AIResponse.class);

            return response;

        }
        catch (Exception e){

            e.printStackTrace();

            return new AIResponse(
                    0,
                    0,
                    0,
                    "Unavailable",
                    e.getMessage()
            );

        }

    }

}