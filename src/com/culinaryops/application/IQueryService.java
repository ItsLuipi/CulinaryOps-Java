package com.culinaryops.application;

import java.util.List;

public interface IQueryService<T> {

    List<T> findAll();

    T findById(String id);

    List<T> search(String textQuery);
}
