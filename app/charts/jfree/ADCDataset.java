package charts.jfree;

import java.util.Map;

import org.jfree.data.category.DefaultCategoryDataset;

import com.google.common.collect.Maps;

// Attributed Default Category Dataset
public class ADCDataset extends DefaultCategoryDataset {

  private Map<String, Object> attributes = Maps.newHashMap();

  public <T> void add(Attribute<T> attribute, T value) {
    attributes.put(attribute.getName(), value);
  }

  public <T> T get(Attribute<T> attribute) {
    return attribute.getType().cast(attributes.get(attribute.getName()));
  }

}
