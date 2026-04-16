package com.cinebook.repository;

import java.util.List;
import java.util.Optional;

/** Generic repository interface abstracting file-based persistence. */
public interface Repository<T> {

    /** Find an entity by its unique identifier. */
    Optional<T> findById(String id);

    /** Return all entities. */
    List<T> findAll();

    /** Persist an entity (insert or update). */
    void save(T entity);

    /** Delete an entity by its identifier. */
    void deleteById(String id);
}
