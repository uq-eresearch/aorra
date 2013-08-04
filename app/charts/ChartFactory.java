package charts;

import java.util.List;
import java.util.Map;

import charts.spreadsheet.DataSource;

import com.google.common.collect.Lists;

public class ChartFactory {

    private List<DataSource> datasources;

    public ChartFactory() {
    }

    public ChartFactory(List<DataSource> datasources) {
        this.datasources = datasources;
    }

    public List<Chart> getCharts(ChartType type, Map<String, String[]> properties) {
        List<Chart> result = Lists.newArrayList();
        for(ChartType t : ChartType.values()) {
            if((type == null) || type.equals(t)) {
                if(t.equals(ChartType.MARINE)) {
                    result.addAll(getMarineCharts(properties));
                }
            }
        }
        return result;
    }

    public List<Chart> getCharts(Map<String, String[]> properties) {
        return getCharts(null, properties);
    }

    private List<Chart> getMarineCharts(Map<String, String[]> properties) {
        List<Chart> result = Lists.newArrayList();
        if(datasources != null) {
            for(DataSource datasource : datasources) {
                if(Marine.isMarineSpreadsheet(datasource)) {
                    Marine marine = new Marine(datasource);
                    result.addAll(marine.getCharts(properties));
                }
            }
        }
        return result;
    }

}
