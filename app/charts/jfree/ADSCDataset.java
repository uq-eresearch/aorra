package charts.jfree;

import java.util.Map;

import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;

import com.google.common.collect.Maps;

public class ADSCDataset extends DefaultStatisticalCategoryDataset implements AttributedDataset {

  private Map<String, Object> attributes = Maps.newHashMap();

  @Override
  public <T> void add(Attribute attribute, T value) {
    attributes.put(attribute.getName(), value);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T get(Attribute attribute) {
    return (T)attributes.get(attribute.getName());
  }

}
