package charts;

import java.util.Map;


public class ChartDescription {

    private ChartType type;

    private Map<String, String> properties;

    public ChartDescription(ChartType type, Map<String, String> properties) {
        super();
        this.type = type;
        this.properties = properties;
    }

    public ChartType getType() {
        return type;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

}
