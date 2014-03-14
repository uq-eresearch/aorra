package charts.jfree;

public class AttributedDatasetImpl implements AttributedDataset {

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
