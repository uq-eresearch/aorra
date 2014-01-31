package charts.graphics;

import java.awt.Dimension;

import charts.Drawable;
import charts.jfree.ADCDataset;

public interface ManagementPracticeSystems {
  public Drawable createChart(ADCDataset dataset, Dimension dimension);
}
