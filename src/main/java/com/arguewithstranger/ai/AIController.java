package com.arguewithstranger.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;

    @PostMapping("/analyze/{debateId}")
    public AIResponse analyze(
            @PathVariable Long debateId){

        return aiService.analyze(debateId);

    }

}