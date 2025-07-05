package com.example.starter.service;

import com.example.starter.model.User;
import io.vertx.core.Future;

import java.util.List;


public interface UserService {

  Future<User> getUser(Long id);

  Future<List<User>> getAllUsers();

  Future<User> createUser(String name);

  Future<User> updateUser(Long id, String name);

  Future<Boolean> deleteUser(Long id);

}
