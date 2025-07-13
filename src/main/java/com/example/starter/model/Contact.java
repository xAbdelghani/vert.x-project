package com.example.starter.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Contact {

  private Long id;
  private String nomc;
  private String prenomc;
  private String fax;
  private String telephonec;
  private String emailc;
  private String remarquec;
  private Long compagnieId;
  private Long fonctionId;

  // Related entities - loaded on demand
  private Compagnie compagnie;
  private Fonction fonction;

}
