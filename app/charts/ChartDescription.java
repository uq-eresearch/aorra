package charts;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;

public class ChartDescription {

    private final ChartType type;

    private final Region region;

    private Map<String, String> parameters = Maps.newHashMap();

    private String title;

    public ChartDescription(ChartType type, Region region) {
      this.type = checkNotNull(type);
      this.region = checkNotNull(region);
    }

    public ChartDescription(ChartType type, Region region, String title) {
      this(type, region);
      this.title = title;
    }

    public ChartDescription(ChartType type, Region region, Map<String, String> parameters) {
        this(type, region);
        this.parameters = parameters;
    }

    public ChartDescription(ChartType type, Region region, Map<String, String> parameters, String title) {
        this(type, region, parameters);
        this.title = title;
    }

    public ChartType getType() {
      return type;
    }

    public Region getRegion() {
      return region;
    }

    public String getTitle() {
        return title!=null?title:type.getLabel();
    }

    public String getParameter(String name) {
      return parameters.get(name);
    }

    public Set<String> getParameterNames() {
      return Collections.unmodifiableSet(parameters.keySet());
    }

    public Map<String, String> getParameters() {
      return Collections.unmodifiableMap(parameters);
    }

    @Override
    public String toString() {
      final StringBuffer sb = new StringBuffer();
      sb.append(type+"-"+region);
      if (!parameters.isEmpty()) {
        sb.append("-");
        for (final Map.Entry<String, ?> e : parameters.entrySet()) {
          sb.append(String.format("[%s-%s]",
              e.getKey(), e.getValue().toString()));
        }
      }
      if(title!=null) {
          sb.append("-");
          sb.append(title);
      }
      return sb.toString();
    }

}
