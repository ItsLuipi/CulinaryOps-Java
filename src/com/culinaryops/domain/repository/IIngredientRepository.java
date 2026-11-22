/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.culinaryops.domain.repository;

import com.culinaryops.domain.model.Ingredient;
import java.util.List;
import java.util.Optional;
/**
 *
 * @author Luipi
 */


public interface IIngredientRepository {

    List<Ingredient> findAll();
    
    Optional<Ingredient> findById(String id);

    Ingredient save(Ingredient ingredient);

    boolean deleteById(String id);
}