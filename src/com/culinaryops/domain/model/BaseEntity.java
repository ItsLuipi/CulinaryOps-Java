/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.culinaryops.domain.model;

import java.time.LocalDateTime;

/**
 * Clase base abstracta para entidades con identidad y marcas de auditoría.
 *
 * @author Luipi
 */
public abstract class BaseEntity {

    protected final String id;
    protected final LocalDateTime createdAt;
    protected LocalDateTime updatedAt;

    protected BaseEntity(String id) {
        this(id, LocalDateTime.now(), LocalDateTime.now());
    }

    protected BaseEntity(String id, LocalDateTime createdAt, LocalDateTime updatedAt) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id de la entidad no puede ser nulo ni estar vacío.");
        }
        LocalDateTime now = LocalDateTime.now();
        this.id = id.trim();
        this.createdAt = (createdAt != null) ? createdAt : now;
        this.updatedAt = (updatedAt != null) ? updatedAt : this.createdAt;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    protected void markUpdated() {
        this.updatedAt = LocalDateTime.now();
    }
}
