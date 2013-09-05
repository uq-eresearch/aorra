package charts.builder;

import java.util.List;
import java.util.Map;


public interface ChartTypeBuilder {

    public boolean canHandle(ChartType type, List<DataSource> datasources);

    public List<Chart> build(List<DataSource> datasources, ChartType type, Map<String, String[]> query);

}
