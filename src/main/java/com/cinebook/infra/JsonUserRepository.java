package com.cinebook.infra;

import com.cinebook.domain.User;
import com.cinebook.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JSON file-backed implementation of {@link UserRepository}. */
public class JsonUserRepository implements UserRepository {

    private static final Logger log = LoggerFactory.getLogger(JsonUserRepository.class);
    private static final TypeReference<List<User>> TYPE_REF = new TypeReference<>() {};

    private final Path file;
    private final JsonFileAdapter adapter;
    private List<User> users;

    public JsonUserRepository(Path file, JsonFileAdapter adapter) {
        this.file = file;
        this.adapter = adapter;
        this.users = load();
    }

    @Override
    public Optional<User> findById(String id) {
        return users.stream()
                .filter(u -> u.getUserId().equals(id))
                .findFirst();
    }

    @Override
    public List<User> findAll() {
        return List.copyOf(users);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return users.stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    @Override
    public void save(User user) {
        users.removeIf(u -> u.getUserId().equals(user.getUserId()));
        users.add(user);
        persist();
    }

    @Override
    public void deleteById(String id) {
        users.removeIf(u -> u.getUserId().equals(id));
        persist();
    }

    private List<User> load() {
        try {
            return new ArrayList<>(adapter.readList(file, TYPE_REF));
        } catch (IOException e) {
            log.error("Failed to load users from {}", file, e);
            throw new UncheckedIOException(e);
        }
    }

    private void persist() {
        try {
            adapter.writeList(file, users);
        } catch (IOException e) {
            log.error("Failed to persist users to {}", file, e);
            throw new UncheckedIOException(e);
        }
    }
}
