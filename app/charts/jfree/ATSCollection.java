package charts.jfree;

import org.jfree.data.time.TimeSeriesCollection;

public class ATSCollection extends TimeSeriesCollection implements AttributedDataset {

  private AttributeMap attrMap = new AttributeMap();

  @Override
  public AttributeMap attrMap() {
    return attrMap;
  }

  @Override
  public <T> T get(Attribute<T> attribute) {
    return attrMap().get(attribute);
  }
}
