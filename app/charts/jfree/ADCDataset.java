package charts.jfree;

import org.jfree.data.category.DefaultCategoryDataset;

// Attributed Default Category Dataset
public class ADCDataset extends DefaultCategoryDataset implements AttributedDataset {

  private AttributeMap attrMap = new AttributeMap();

  @Override
  public AttributeMap attrMap() {
    return attrMap;
  }

  public <T> T get(Attribute<T> attribute) {
    return attrMap().get(attribute);
  }

}
