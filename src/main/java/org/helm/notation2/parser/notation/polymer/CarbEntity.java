package org.helm.notation2.parser.notation.polymer;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * CarbEntity
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "classType")
public class CarbEntity extends PolymerEntity {

  public CarbEntity() {

  }

  /**
   * Constructs with the given string
   *
   * @param str Carb Entity
   */
  public CarbEntity(String str) {
    super(str);
    type = "CARB";
  }

}
