package charts.builder;

import charts.representations.Format;
import charts.representations.Representation;

public interface Chart {

  @SuppressWarnings("serial")
  public class UnsupportedFormatException extends Exception {}

  public ChartDescription getDescription();

  public Representation outputAs(Format format)
      throws UnsupportedFormatException;

}
