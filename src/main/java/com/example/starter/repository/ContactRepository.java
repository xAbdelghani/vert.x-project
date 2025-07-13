package com.example.starter.repository;

import com.example.starter.model.Contact;
import io.vertx.core.Future;
import java.util.List;

public interface ContactRepository {

  Future<Long> save(Contact contact);
  Future<Contact> findById(Long id);
  Future<List<Contact>> findAll();
  Future<List<Contact>> findByCompagnieId(Long compagnieId);
  Future<List<Contact>> findByFonctionId(Long fonctionId);
  Future<Void> update(Long id, Contact contact);
  Future<Void> delete(Long id);

}
