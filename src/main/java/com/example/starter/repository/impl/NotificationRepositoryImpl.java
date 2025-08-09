package com.example.starter.repository.impl;

import com.example.starter.model.Notification;
import com.example.starter.repository.NotificationRepository;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NotificationRepositoryImpl implements NotificationRepository {

  private final PgPool pgPool;

  public NotificationRepositoryImpl(PgPool pgPool) {
    this.pgPool = pgPool;
  }

  @Override
  public Future<Long> save(Notification notification) {
    String getNextIdSql = "SELECT COALESCE(MAX(id), 0) + 1 as next_id FROM notification";

    return pgPool.preparedQuery(getNextIdSql)
      .execute()
      .compose(rows -> {
        Long nextId = rows.iterator().next().getLong("next_id");

        String sql = """
                    INSERT INTO notification (
                        id, company_id, message, description, timestamp, read, type, metadata
                    ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
                    RETURNING id
                """;

        return pgPool.preparedQuery(sql)
          .execute(Tuple.of(
            nextId,
            notification.getCompanyId(),
            notification.getMessage(),
            notification.getDescription(),
            notification.getTimestamp() != null ? notification.getTimestamp() : LocalDateTime.now(),
            notification.getRead() != null ? notification.getRead() : false,
            notification.getType(),
            notification.getMetadata()
          ))
          .map(insertRows -> insertRows.iterator().next().getLong("id"));
      });
  }

  @Override
  public Future<Notification> findById(Long id) {
    String sql = "SELECT * FROM notification WHERE id = $1";

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .map(rows -> {
        if (rows.size() == 0) return null;
        return mapRowToNotification(rows.iterator().next());
      });
  }

  @Override
  public Future<List<Notification>> findAll() {
    String sql = """
            SELECT * FROM notification
            ORDER BY timestamp DESC
        """;

    return pgPool.preparedQuery(sql)
      .execute()
      .map(rows -> {
        List<Notification> notifications = new ArrayList<>();
        rows.forEach(row -> notifications.add(mapRowToNotification(row)));
        return notifications;
      });
  }

  @Override
  public Future<List<Notification>> findByCompanyId(Long companyId) {
    String sql = """
            SELECT * FROM notification
            WHERE company_id = $1
            ORDER BY timestamp DESC
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(companyId))
      .map(rows -> {
        List<Notification> notifications = new ArrayList<>();
        rows.forEach(row -> notifications.add(mapRowToNotification(row)));
        return notifications;
      });
  }

  @Override
  public Future<List<Notification>> findUnread() {
    String sql = """
            SELECT * FROM notification
            WHERE read = false
            ORDER BY timestamp DESC
        """;

    return pgPool.preparedQuery(sql)
      .execute()
      .map(rows -> {
        List<Notification> notifications = new ArrayList<>();
        rows.forEach(row -> notifications.add(mapRowToNotification(row)));
        return notifications;
      });
  }

  @Override
  public Future<List<Notification>> findByType(String type) {
    String sql = """
            SELECT * FROM notification
            WHERE type = $1
            ORDER BY timestamp DESC
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(type))
      .map(rows -> {
        List<Notification> notifications = new ArrayList<>();
        rows.forEach(row -> notifications.add(mapRowToNotification(row)));
        return notifications;
      });
  }

  @Override
  public Future<Void> markAsRead(Long id) {
    String sql = "UPDATE notification SET read = true WHERE id = $1";

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .map(rows -> null);
  }

  @Override
  public Future<Void> markMultipleAsRead(List<Long> ids) {
    if (ids.isEmpty()) {
      return Future.succeededFuture();
    }

    String placeholders = ids.stream()
      .map(id -> "?")
      .collect(Collectors.joining(","));

    String sql = "UPDATE notification SET read = true WHERE id IN (" + placeholders + ")";

    Tuple params = Tuple.tuple();
    ids.forEach(params::addValue);

    return pgPool.preparedQuery(sql)
      .execute(params)
      .map(rows -> null);
  }

  @Override
  public Future<Integer> countUnread() {
    String sql = "SELECT COUNT(*) as count FROM notification WHERE read = false";

    return pgPool.preparedQuery(sql)
      .execute()
      .map(rows -> rows.iterator().next().getInteger("count"));
  }

  @Override
  public Future<Integer> countUnreadForCompany(Long companyId) {
    String sql = "SELECT COUNT(*) as count FROM notification WHERE company_id = $1 AND read = false";

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(companyId))
      .map(rows -> rows.iterator().next().getInteger("count"));
  }

  @Override
  public Future<Void> delete(Long id) {
    String sql = "DELETE FROM notification WHERE id = $1";

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .map(rows -> null);
  }

  private Notification mapRowToNotification(Row row) {
    Notification notification = new Notification();
    notification.setId(row.getLong("id"));
    notification.setCompanyId(row.getLong("company_id"));
    notification.setMessage(row.getString("message"));
    notification.setDescription(row.getString("description"));
    notification.setTimestamp(row.getLocalDateTime("timestamp"));
    notification.setRead(row.getBoolean("read"));
    notification.setType(row.getString("type"));
    notification.setMetadata(row.getString("metadata"));
    return notification;
  }
}
