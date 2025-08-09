package com.example.starter.repository;

import com.example.starter.model.Notification;
import io.vertx.core.Future;

import java.util.List;

public interface NotificationRepository {

  Future<Long> save(Notification notification);
  Future<Notification> findById(Long id);
  Future<List<Notification>> findAll();
  Future<List<Notification>> findByCompanyId(Long companyId);
  Future<List<Notification>> findUnread();
  Future<List<Notification>> findByType(String type);
  Future<Void> markAsRead(Long id);
  Future<Void> markMultipleAsRead(List<Long> ids);
  Future<Integer> countUnread();
  Future<Integer> countUnreadForCompany(Long companyId);
  Future<Void> delete(Long id);

}
