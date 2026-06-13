package com.arguewithstranger.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .configure(
                            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                            false
                    );

    public String generate(String prompt) {

        try {

            GeminiRequest.Part part =
                    new GeminiRequest.Part(prompt);

            GeminiRequest.Content content =
                    new GeminiRequest.Content(List.of(part));

            GeminiRequest requestBody =
                    new GeminiRequest(List.of(content));

            String json =
                    objectMapper.writeValueAsString(requestBody);

            String url =
                    "https://generativelanguage.googleapis.com/v1beta/models/"
                            + model
                            + ":generateContent?key="
                            + apiKey;

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(json))
                            .build();

            HttpClient client = HttpClient.newHttpClient();

            HttpResponse<String> response =
                    client.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            System.out.println("========== GEMINI RESPONSE ==========");
            System.out.println(response.statusCode());
            System.out.println(response.body());
            System.out.println("=====================================");

            if (response.statusCode() == 429) {
                throw new RuntimeException(
                        "Gemini quota exceeded. Please try again later."
                );
            }

            if (response.statusCode() != 200) {
                throw new RuntimeException(
                        "Gemini API Error: "
                                + response.statusCode()
                                + "\n"
                                + response.body()
                );
            }

            GeminiResponse geminiResponse =
                    objectMapper.readValue(
                            response.body(),
                            GeminiResponse.class
                    );

            if (geminiResponse.getCandidates() == null
                    || geminiResponse.getCandidates().isEmpty()) {

                return "No AI response generated.";
            }

            GeminiResponse.Candidate candidate =
                    geminiResponse.getCandidates().get(0);

            if (candidate.getContent() == null
                    || candidate.getContent().getParts() == null
                    || candidate.getContent().getParts().isEmpty()) {

                return "No AI response generated.";
            }

            String result = candidate
                    .getContent()
                    .getParts()
                    .get(0)
                    .getText();

            result = result.replace("```json", "")
                    .replace("```", "")
                    .trim();

            return result;

        } catch (Exception e) {

            System.out.println("========== GEMINI ERROR ==========");
            e.printStackTrace();
            System.out.println("==================================");

            throw new RuntimeException(
                    "Failed to call Gemini: " + e.getMessage(),
                    e
            );
        }
    }
}