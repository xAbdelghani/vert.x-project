package com.example.starter.repository.impl;

import com.example.starter.model.User;
import com.example.starter.repository.UserRepository;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import java.util.ArrayList;
import java.util.List;

public class UserRepositoryImpl implements UserRepository {


  private final PgPool pgPool;

  public UserRepositoryImpl(PgPool pgPool) {
    this.pgPool = pgPool;
  }

  @Override
  public Future<User> findById(Long id) {
    return pgPool.preparedQuery("SELECT * FROM users WHERE id = $1")
      .execute(Tuple.of(id))
      .map(rows -> {
        if (rows.size() == 0) return null;
        return mapRow(rows.iterator().next());
      });
  }


  @Override
  public Future<List<User>> findAll() {
    return pgPool.query("SELECT * FROM users")
      .execute()
      .map(rows -> {
        List<User> users = new ArrayList<>();
        for (Row row : rows) {
          users.add(mapRow(row));
        }
        return users;
      });
  }


  @Override
  public Future<User> save(User user) {
    return pgPool.preparedQuery("INSERT INTO users (name) VALUES ($1) RETURNING *")
      .execute(Tuple.of(user.getName()))
      .map(rows -> mapRow(rows.iterator().next()));
  }


  @Override
  public Future<User> update(User user) {
    return pgPool.preparedQuery("UPDATE users SET name = $2 WHERE id = $1 RETURNING *")
      .execute(Tuple.of(user.getId(), user.getName()))
      .map(rows -> mapRow(rows.iterator().next()));
  }

  @Override
  public Future<Boolean> deleteById(Long id) {
    return pgPool.preparedQuery("DELETE FROM users WHERE id = $1")
      .execute(Tuple.of(id))
      .map(result -> result.rowCount() > 0);
  }

  private User mapRow(Row row) {
    User user = new User();
    user.setId(row.getLong("id"));
    user.setName(row.getString("name"));
    return user;
  }

}
