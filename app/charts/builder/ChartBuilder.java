package charts.builder;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import charts.spreadsheet.DataSource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class ChartBuilder {

    private static List<ChartTypeBuilder> BUILDERS = new ImmutableList.Builder<ChartTypeBuilder>()
            .add(new MarineSpreadsheetChartBuilder())
            .add(new CotsOutbreakSpreadsheetBuilder())
            .build();

    private List<DataSource> datasources;

    public ChartBuilder() {
    }

    public ChartBuilder(List<DataSource> datasources) {
        this.datasources = datasources;
    }

    public List<Chart> getCharts(ChartType type, Map<String, String[]> query) {
        List<Chart> result = Lists.newArrayList();
        for(ChartTypeBuilder builder : BUILDERS) {
            if(builder.canHandle(type, datasources)) {
                result.addAll(builder.build(datasources, query));
            }
        }
        // make sure charts are sorted by region 
        // https://github.com/uq-eresearch/aorra/issues/44
        Collections.sort(result, new Comparator<Chart>() {
            @Override
            public int compare(Chart c1, Chart c2) {
                Region r1 = null;
                Region r2 = null;
                if(c1.getDescription()!=null) {
                    r1 = c1.getDescription().getRegion();
                }
                if(c2.getDescription()!=null) {
                    r2 = c2.getDescription().getRegion();
                }
                if(r1 != null) {
                    return r1.compareTo(r2);
                } else if(r2 == null){
                    return 0;
                } else {
                    return -1;
                }
            }});
        return result;
    }

    public List<Chart> getCharts(Map<String, String[]> query) {
        return getCharts(null, query);
    }

}
