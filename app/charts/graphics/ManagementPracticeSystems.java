package charts.graphics;

import java.awt.Dimension;

import org.jfree.data.category.CategoryDataset;

import charts.Drawable;

public interface ManagementPracticeSystems {
  public Drawable createChart(CategoryDataset dataset, String title, Dimension dimension);
}
