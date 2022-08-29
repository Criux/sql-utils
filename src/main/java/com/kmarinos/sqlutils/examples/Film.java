package com.kmarinos.sqlutils.examples;

import java.math.BigDecimal;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Film {

  String title;
  int releaseYear;
  BigDecimal replacementCost;
  Date lastUpdate;
}
