package charts.builder;

import java.awt.Dimension;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jfree.ui.Drawable;

import charts.Dimensions;
import charts.spreadsheet.DataSource;

import com.google.common.collect.Lists;

public abstract class DefaultSpreadsheetChartBuilder implements ChartTypeBuilder {

    private ChartType type;

    public DefaultSpreadsheetChartBuilder(ChartType type) {
        this.type = type;
    }

    abstract boolean canHandle(DataSource datasource);

    abstract List<Chart> build(DataSource datasource, Map<String, String[]> query);

    @Override
    public boolean canHandle(ChartType type, List<DataSource> datasources) {
        if(type == null || type.equals(this.type)) {
            for(DataSource ds : datasources) {
                if(canHandle(ds)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<Chart> build(List<DataSource> datasources,
            Map<String, String[]> query) {
        List<Chart> charts = Lists.newArrayList();
        for(DataSource datasource : datasources) {
            if(canHandle(datasource)) {
                charts.addAll(build(datasource, query));
            }
        }
        return charts;
    }

    Dimensions createDimensions(Drawable d, Map<String, String[]> query) {
        return new DimensionsWrapper(d, new Dimension(
                getParam(query, "width", 750), getParam(query, "height", 500)));
    }

    int getParam(Map<String, String[]> query, String name, int def) {
        try {
            String tmp = getParam(query, name);
            if(StringUtils.isNotBlank(tmp)) {
                return Integer.parseInt(tmp);
            } else {
                return def;
            }
        } catch(Exception e) {
            return def;
        }
    }

    String getParam(Map<String, String[]> query, String name) {
        String[] tmp = query.get(name);
        if(tmp!=null && tmp.length > 0) {
            return tmp[0];
        }
        return null;
    }

}
