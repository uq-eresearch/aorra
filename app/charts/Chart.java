package charts;

import java.awt.Dimension;
import java.util.Date;

import charts.representations.Format;
import charts.representations.Representation;

public interface Chart {

  @SuppressWarnings("serial")
  public class UnsupportedFormatException extends Exception {}

  public ChartDescription getDescription();

  public Representation outputAs(Format format, Dimension queryDimension)
      throws UnsupportedFormatException;

  public Date created();

}
