package com.culinaryops.domain.repository;

import com.culinaryops.domain.model.ServiceBudget;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia para la gestión de presupuestos comerciales.
 */
public interface IBudgetRepository {

    List<ServiceBudget> findAll();

    Optional<ServiceBudget> findById(String id);

    ServiceBudget save(ServiceBudget budget);

    boolean deleteById(String id);
}