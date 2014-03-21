package charts.builder.spreadsheet;

import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jfree.data.category.CategoryDataset;
import org.supercsv.io.CsvListWriter;

import play.Logger;
import charts.ChartType;
import charts.Drawable;
import charts.builder.DataSource.MissingDataException;
import charts.builder.Value;
import charts.builder.csv.Csv;
import charts.builder.csv.CsvWriter;
import charts.graphics.AnnualRainfall;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;

import com.google.common.collect.ImmutableSet;

public class AnnualRainfallBuilder extends JFreeBuilder {

  private static final SubstitutionKey START_YEAR = new SubstitutionKey("startYear",
      "first year of annual rainfall data in the spreadsheet",
      new SubstitutionKey.Val() {
        @Override
        public String value(Context ctx) {
          CategoryDataset dataset = (CategoryDataset)((JFreeContext)ctx).dataset();
          if(!dataset.getColumnKeys().isEmpty()) {
            return dataset.getColumnKeys().get(0).toString();
          } else {
            return "";
          }
        }
      });
  private static final SubstitutionKey END_YEAR = new SubstitutionKey("endYear",
      "last year of annual rainfall data in the spreadsheet",
      new SubstitutionKey.Val() {
        @Override
        public String value(Context ctx) {
          CategoryDataset dataset = (CategoryDataset)((JFreeContext)ctx).dataset();
          if(!dataset.getColumnKeys().isEmpty()) {
            return dataset.getColumnKeys().get(dataset.getColumnKeys().size()-1).toString();
          } else {
            return "";
          }
        }
      });
  private static final SubstitutionKey MAR_REGION = new SubstitutionKey("marRegion",
      "mean annual rainfall region copied from spreadsheet A2-A7",
      new SubstitutionKey.Val() {
        @Override
        public String value(Context ctx) {
          try {
            return ctx.datasource().select(regionRow(ctx), 0).asString();
          } catch (MissingDataException e) {
            return ctx.region().getProperName();
          }
        }
      });

  public AnnualRainfallBuilder() {
    super(ChartType.ANNUAL_RAINFALL);
  }

  @Override
  protected ADCDataset createDataset(Context ctx) {
    Integer row = regionRow(ctx);
    if(row == null) {
      return null;
    }
    final String series = "rainfall";
    ADCDataset dataset = new ADCDataset();
    try {
      for (int i = 1; true; i++) {
        String year = ctx.datasource().select(0, i).asString();
        if (StringUtils.equalsIgnoreCase("Annual Average", year)) {
          break;
        }
        String rainfall = ctx.datasource().select(row, i).asString();
        if (StringUtils.isBlank(year) || StringUtils.isBlank(rainfall)) {
          break;
        }
        double val = Double.parseDouble(rainfall);
        dataset.addValue(val, series, Integer.toString(parseYear(year)));
      }
    } catch (Exception e) {
      Logger.debug("while building dataset for the annual rainfall chart", e);
    }
    return dataset;
  }

  private int parseYear(String y) {
    y = StringUtils.strip(y);
    if (y.contains(".")) {
      y = StringUtils.substringBefore(y, ".");
    }
    return Integer.parseInt(y);
  }

  @Override
  public boolean canHandle(SpreadsheetDataSource datasource) {
    try {
      for(int column = 1; column<datasource.getColumnCount(0);column++) {
        Value v = datasource.select(0, column);
        if(v.asInteger() == null) {
          return StringUtils.equalsIgnoreCase("Annual Average", StringUtils.strip(v.asString()));
        }
      }
    } catch (MissingDataException e) {}
    return false;
  }

  @Override
  public AttributeMap defaults(ChartType type) {
    return new AttributeMap.Builder().
        put(Attribute.TITLE, "Mean annual rainfall for ${startYear}-${endYear} - ${marRegion}").
        put(Attribute.X_AXIS_LABEL, "Year").
        put(Attribute.Y_AXIS_LABEL, "Rainfall (mm)").
        put(Attribute.SERIES_COLOR, Color.blue).
        build();
  }

  @Override
  protected Drawable getDrawable(JFreeContext ctx) {
    return new AnnualRainfall().createChart((ADCDataset)ctx.dataset(), new Dimension(750, 500));
  }

  @Override
  protected String getCsv(JFreeContext ctx) {
    final CategoryDataset dataset = (CategoryDataset)ctx.dataset();
    return Csv.write(new CsvWriter() {
      @Override
      public void write(CsvListWriter csv) throws IOException {
        csv.write("Year", "Rainfall (mm)");
        for (int i = 0; i < dataset.getColumnCount(); i++) {
          csv.write(dataset.getColumnKey(i), dataset.getValue(0, i));
        }
      }});
  }

  @Override
  public Set<SubstitutionKey> substitutionKeys() {
    return ImmutableSet.<SubstitutionKey>builder().
        addAll(super.substitutionKeys()).add(START_YEAR).add(END_YEAR).add(MAR_REGION).build();
  }

  private static Integer regionRow(Context ctx) {
    int row = 1;
    for(Value v : ctx.datasource().rangeRowSelect(0, 1, 6)) {
      if(StringUtils.containsIgnoreCase(v.asString(), ctx.region().getProperName()) || 
          StringUtils.containsIgnoreCase(
              v.asString(), StringUtils.split(ctx.region().getProperName())[0])) {
        return row;
      }
      row++;
    }
    return null;
  }
}
