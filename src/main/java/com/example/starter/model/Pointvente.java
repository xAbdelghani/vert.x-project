package com.example.starter.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class Pointvente {
  private Long id;
  private String nomp;
  private String emailp;
  private String telephonep;
  private LocalDate dateDebutp;
  private LocalDate dateFinp;

  // Relationships - loaded on demand
  private List<RelationPointventeCompagnie> relations;
}

