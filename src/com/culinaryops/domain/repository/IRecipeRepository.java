/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.culinaryops.domain.repository;

/**
 *
 * @author Luipi
 */
import com.culinaryops.domain.model.Recipe;
import java.util.List;
import java.util.Optional;
public interface IRecipeRepository {
    List<Recipe> findAll();

    Optional<Recipe> findById(String id);

    Recipe save(Recipe recipe);

    boolean deleteById(String id);


  
}
