package com.example.starter.service.impl;

import com.example.starter.model.User;
import com.example.starter.repository.UserRepository;
import com.example.starter.service.UserService;
import io.vertx.core.Future;
import java.util.List;


public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;

  public UserServiceImpl(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public Future<User> getUser(Long id) {
    return userRepository.findById(id);
  }

  @Override
  public Future<List<User>> getAllUsers() {
    return userRepository.findAll();
  }

  @Override
  public Future<User> createUser(String name) {
    User user = new User(name);
    return userRepository.save(user);
  }

  @Override
  public Future<User> updateUser(Long id, String name) {
    User user = new User(name);
    user.setId(id);
    return userRepository.update(user);
  }

  @Override
  public Future<Boolean> deleteUser(Long id) {
    return userRepository.deleteById(id);
  }


}
