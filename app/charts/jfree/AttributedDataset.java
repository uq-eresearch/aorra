package charts.jfree;

public interface AttributedDataset {

  public <T> void add(Attribute attribute, T value);

  public <T> T get(Attribute attribute);

}
