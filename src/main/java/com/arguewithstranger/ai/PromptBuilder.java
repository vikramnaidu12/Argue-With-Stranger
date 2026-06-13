package com.arguewithstranger.ai;

import com.arguewithstranger.entity.Message;

import java.util.List;

public class PromptBuilder {

    public static String build(List<Message> messages){

        StringBuilder prompt = new StringBuilder();

        prompt.append("""
You are an impartial debate judge.

Analyze the following debate.

Return ONLY JSON in this format:

{
"winner":"",
"logic":0,
"evidence":0,
"persuasion":0,
"summary":""
}

Debate:
""");

        for(Message m : messages){

            prompt.append("\n");

            prompt.append(m.getSender().getUsername());

            prompt.append(": ");

            prompt.append(m.getContent());

        }

        return prompt.toString();

    }

}