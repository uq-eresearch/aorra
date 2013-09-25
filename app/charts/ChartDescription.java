package charts;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;

public class ChartDescription {

    private final ChartType type;

    private final Region region;

    private Map<String, String> parameters = Maps.newHashMap();

    public ChartDescription(ChartType type, Region region) {
      this.type = checkNotNull(type);
      this.region = checkNotNull(region);
    }

    public ChartDescription(ChartType type, Region region, Map<String, String> parameters) {
        this(type, region);
        this.parameters = parameters;
    }

    public ChartType getType() {
      return type;
    }

    public Region getRegion() {
      return region;
    }

    public String getParameter(String name) {
        return parameters.get(name);
    }

    public Set<String> getParameterNames() {
        return parameters.keySet();
    }

}
