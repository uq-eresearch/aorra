package charts.jfree;

public interface AttributedDataset {

  AttributeMap attrMap();

  public <T> T get(Attribute<T> attribute);

}
