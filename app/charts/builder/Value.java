package charts.builder;

import java.util.Date;

public interface Value {
  String asString();

  Double asDouble();

  Integer asInteger();

  String getValue();

  java.awt.Color asColor();

  Date asDate();

  Double asPercent();

}
