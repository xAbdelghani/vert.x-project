package com.example.starter.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Fonction {
  private Long id;
  private String qualite;

  // Relationships - loaded on demand
  private List<Contact> contacts;
}
