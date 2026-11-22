/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.culinaryops.domain.service;

/**
 *
 * @author roco
 */
import com.culinaryops.domain.model.Ingredient;
import com.culinaryops.domain.model.dto.ChatbotAnswer;

import java.util.List;

public interface InventoryQueryHandler {

    boolean supports(String normalizedQuery);

    ChatbotAnswer handle(String normalizedQuery, List<Ingredient> ingredients);
}
