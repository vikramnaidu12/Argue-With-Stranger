package com.arguewithstranger.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIResponse {

    private int logic;

    private int evidence;

    private int persuasion;

    private String winner;

    private String summary;

}