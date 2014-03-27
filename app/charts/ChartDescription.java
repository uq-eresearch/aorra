package charts;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
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
        if(parameters != null) {
          this.parameters = parameters;
        }
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

    public boolean hasParameters() {
      return (parameters != null) && !parameters.isEmpty();
    }

    public String getParameterString() {
      final StringBuffer sb = new StringBuffer();
      if(hasParameters()) {
        List<String> keys = Lists.newArrayList(parameters.keySet());
        Collections.sort(keys);
        for(String key : keys) {
          String val = parameters.get(key);
          sb.append(String.format("[%s-%s]", key, val));
        }
      }
      return sb.toString();
    }

    @Override
    public String toString() {
      final StringBuffer sb = new StringBuffer();
      sb.append(type+"-"+region);
      if(hasParameters()) {
        sb.append("-");
        sb.append(getParameterString());
      }
      if(title!=null) {
          sb.append("-");
          sb.append(title);
      }
      return sb.toString();
    }

}
