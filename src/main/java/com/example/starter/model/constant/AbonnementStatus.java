package com.example.starter.model.constant;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class AbonnementStatus {

  // Status codes
  public static final String ACTIF = "ACTIF";
  public static final String SUSPENDU = "SUSPENDU";
  public static final String EXPIRE = "EXPIRE";
  public static final String RESILIE = "RESILIE";

  // Status metadata
  public static class StatusInfo {
    public final String code;
    public final String label;
    public final String description;
    public final String color;

    StatusInfo(String code, String label, String description, String color) {
      this.code = code;
      this.label = label;
      this.description = description;
      this.color = color;
    }

    public JsonObject toJson() {
      return new JsonObject()
        .put("code", code)
        .put("label", label)
        .put("description", description)
        .put("color", color);
    }
  }

  public static final StatusInfo ACTIF_INFO = new StatusInfo(
    ACTIF, "Actif", "Abonnement actif", "#52c41a"
  );

  public static final StatusInfo SUSPENDU_INFO = new StatusInfo(
    SUSPENDU, "Suspendu", "Abonnement suspendu", "#faad14"
  );

  public static final StatusInfo EXPIRE_INFO = new StatusInfo(
    EXPIRE, "Expiré", "Abonnement expiré", "#d4380d"
  );

  public static final StatusInfo RESILIE_INFO = new StatusInfo(
    RESILIE, "Résilié", "Abonnement résilié", "#cf1322"
  );

  public static StatusInfo getStatusInfo(String status) {
    switch (status) {
      case ACTIF: return ACTIF_INFO;
      case SUSPENDU: return SUSPENDU_INFO;
      case EXPIRE: return EXPIRE_INFO;
      case RESILIE: return RESILIE_INFO;
      default: return null;
    }
  }

  public static boolean isValidStatus(String status) {
    return getStatusInfo(status) != null;
  }

  public static io.vertx.core.json.JsonArray getAllStatuses() {
    io.vertx.core.json.JsonArray array = new JsonArray();
    array.add(ACTIF_INFO.toJson());
    array.add(SUSPENDU_INFO.toJson());
    array.add(EXPIRE_INFO.toJson());
    array.add(RESILIE_INFO.toJson());
    return array;
  }


}
