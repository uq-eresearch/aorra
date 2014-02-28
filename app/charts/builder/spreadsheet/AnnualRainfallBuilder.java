package charts.builder.spreadsheet;

import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jfree.data.category.CategoryDataset;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import play.Logger;
import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.graphics.AnnualRainfall;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;

import com.google.common.collect.ImmutableMap;
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

  private static final ImmutableMap<Region, Integer> ROW =
      new ImmutableMap.Builder<Region, Integer>()
        .put(Region.BURDEKIN, 1)
        .put(Region.FITZROY, 2)
        .put(Region.MACKAY_WHITSUNDAY, 3)
        .put(Region.BURNETT_MARY, 4)
        .put(Region.WET_TROPICS, 5)
        .put(Region.GBR, 6)
        .build();

  public AnnualRainfallBuilder() {
    super(ChartType.ANNUAL_RAINFALL);
  }

  @Override
  protected ADCDataset createDataset(Context ctx) {
    Integer row = ROW.get(ctx.region());
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
      return "Great Barrier Reef".equalsIgnoreCase(datasource.select("A7")
          .getValue());
    } catch (MissingDataException e) {
      return false;
    }
  }

  @Override
  public AttributeMap defaults(ChartType type) {
    return new AttributeMap.Builder().
        put(Attribute.TITLE, "Mean annual rainfall for ${startYear}-${endYear} - ${region}").
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
    CategoryDataset dataset = (CategoryDataset)ctx.dataset();
    final StringWriter sw = new StringWriter();
    try {
      final CsvListWriter csv = new CsvListWriter(sw,
          CsvPreference.STANDARD_PREFERENCE);
      csv.write("Year", "Rainfall (mm)");
      for (int i = 0; i < dataset.getColumnCount(); i++) {
        csv.write(dataset.getColumnKey(i), dataset.getValue(0, i));
      }
      csv.close();
    } catch (IOException e) {
      // How on earth would IOException occur with a StringWriter?
      throw new RuntimeException(e);
    }
    return sw.toString();
  }

  @Override
  public Set<SubstitutionKey> substitutionKeys() {
    return ImmutableSet.<SubstitutionKey>builder().
        addAll(super.substitutionKeys()).add(START_YEAR).add(END_YEAR).build();
  }
}
