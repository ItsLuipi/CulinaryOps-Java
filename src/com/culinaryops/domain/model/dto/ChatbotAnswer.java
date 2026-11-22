/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.culinaryops.domain.model.dto;

/**
 *
 * @author roco
 */
import java.util.List;

public class ChatbotAnswer {

    private final String intentHandled;
    private final String message;
    private final List<String> suggestions;

    public ChatbotAnswer(String intentHandled, String message, List<String> suggestions) {
        this.intentHandled = intentHandled;
        this.message = message;
        this.suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
    }

    public String getIntentHandled() {
        return intentHandled;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }
}