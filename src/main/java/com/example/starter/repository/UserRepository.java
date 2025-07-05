package com.example.starter.repository;

import com.example.starter.model.User;
import io.vertx.core.Future;
import java.util.List;

public interface UserRepository {

  Future<User> findById(Long id);

  Future<List<User>> findAll();

  Future<User> save(User user);

  Future<User> update(User user);

  Future<Boolean> deleteById(Long id);

}
