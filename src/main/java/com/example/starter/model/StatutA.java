package com.example.starter.model;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class  StatutA {

  private Long id;

  private String libelle;
  // Relationships - loaded on demand
  private List<StatutHistorique> statutHistoriques;

}
