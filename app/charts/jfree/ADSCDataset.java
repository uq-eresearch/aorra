package charts.jfree;

import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;

public class ADSCDataset extends DefaultStatisticalCategoryDataset implements AttributedDataset {

  private AttributeMap attrMap = new AttributeMap();

  @Override
  public AttributeMap attrMap() {
    return attrMap;
  }

  public <T> T get(Attribute<T> attribute) {
    return attrMap().get(attribute);
  }

}
