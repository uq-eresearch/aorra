package charts.builder.spreadsheet;

import static java.lang.String.format;

import java.awt.Dimension;
import java.io.IOException;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jfree.data.category.CategoryDataset;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.graphics.TrackingTowardsTargets;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public class TrackingTowardsTargetsBuilder extends JFreeBuilder {

  private static final SubstitutionKey S_TARGET = new SubstitutionKey("target", "the target in %",
      new SubstitutionKey.Val() {
        @Override
        public String value(Context ctx) {
          return percentFormatter().format(getTarget(ctx.datasource(), ctx.type()));
        }
      });

  private static final SubstitutionKey S_BY = new SubstitutionKey("by", "year to reach the target",
      new SubstitutionKey.Val() {
        @Override
        public String value(Context ctx) {
          return getTargetBy(ctx.datasource(), ctx.type());
        }
      });

  private static final String TITLE_TYPO = "tracking towards tagets";
  private static final String TITLE = "tracking towards targets";

  private static final String IMPROVED_PRATICES = "% of farmers adopting improved practices";
  private static final String POLLUTANT_REDUCTION = "% reduction in pollutant load";
  private static final String TARGET = " target (${target} by ${by})";

  private static class Title {
    private String title;
    private String valueAxisLabel;
    public Title(String title, String valueAxisLabel) {
      super();
      this.title = title;
      this.valueAxisLabel = valueAxisLabel;
    }
    public String getTitle() {
      return title;
    }
    public String getValueAxisLabel() {
      return valueAxisLabel;
    }
  }

  private static NumberFormat percentFormatter() {
    NumberFormat percentFormat = NumberFormat.getPercentInstance();
    percentFormat.setMaximumFractionDigits(0);
    return percentFormat;
  }

  private static final ImmutableMap<ChartType, Title> TITLES =
      new ImmutableMap.Builder<ChartType, Title>()
        .put(ChartType.TTT_CANE_AND_HORT, new Title(
          "Cane and horticulture"+TARGET, IMPROVED_PRATICES))
        .put(ChartType.TTT_GRAZING, new Title(
          "Grazing"+TARGET, IMPROVED_PRATICES))
        .put(ChartType.TTT_NITRO_AND_PEST, new Title(
          "Total nitrogen and pesticide"+TARGET, POLLUTANT_REDUCTION))
        .put(ChartType.TTT_SEDIMENT, new Title(
          "Sediment"+TARGET, POLLUTANT_REDUCTION))
        .build();

  private static enum Series {
    CANE("Cane"), HORTICULTURE("Horticulture"), GRAZING("Grazing"), SEDIMENT(
        "Sediment"), TOTAL_NITROGEN("Total nitrogen"), PESTICIDES("Pesticides");

    private String name;

    private Series(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  private static final ImmutableMap<Series, Integer> ROW =
    new ImmutableMap.Builder<Series, Integer>()
      .put(Series.CANE, 1)
      .put(Series.HORTICULTURE, 2)
      .put(Series.GRAZING, 3)
      .put(Series.SEDIMENT, 4)
      .put(Series.TOTAL_NITROGEN, 5)
      .put(Series.PESTICIDES, 6)
      .build();

  public TrackingTowardsTargetsBuilder() {
    super(Lists.newArrayList(ChartType.TTT_CANE_AND_HORT,
        ChartType.TTT_GRAZING, ChartType.TTT_NITRO_AND_PEST,
        ChartType.TTT_SEDIMENT));
  }

  @Override
  public boolean canHandle(SpreadsheetDataSource datasource) {
    try {
      String title = datasource.select("A1").asString();
      return TITLE.equalsIgnoreCase(title) || TITLE_TYPO.equalsIgnoreCase(title);
    } catch (MissingDataException e) {
      return false;
    }
  }

  @Override
  protected ADCDataset createDataset(Context ctx) {
    if (ctx.region() == Region.GBR) {
      SpreadsheetDataSource ds = ctx.datasource();
      final ADCDataset dataset = new ADCDataset();
      try {
        switch (ctx.type()) {
        case TTT_CANE_AND_HORT:
          addSeries(ds, dataset, Series.CANE);
          addSeries(ds, dataset, Series.HORTICULTURE);
          break;
        case TTT_GRAZING:
          addSeries(ds, dataset, Series.GRAZING);
          break;
        case TTT_NITRO_AND_PEST:
          addSeries(ds, dataset, Series.TOTAL_NITROGEN);
          addSeries(ds, dataset, Series.PESTICIDES);
          break;
        case TTT_SEDIMENT:
          addSeries(ds, dataset, Series.SEDIMENT);
          break;
          //$CASES-OMITTED$
        default:
          throw new RuntimeException("chart type not supported "
              + ctx.type().toString());
        }
      } catch (MissingDataException e) {
        throw new RuntimeException(e);
      }
      return dataset;
    } else {
      return null;
    }
  }

  protected static Series getTargetSeries(ChartType chartType) {
    switch (chartType) {
    case TTT_CANE_AND_HORT:
      return Series.CANE;
    case TTT_GRAZING:
      return Series.GRAZING;
    case TTT_NITRO_AND_PEST:
      return Series.TOTAL_NITROGEN;
    case TTT_SEDIMENT:
      return Series.SEDIMENT;
      //$CASES-OMITTED$
    default:
      throw new RuntimeException("chart type not supported "+chartType);
    }
  }

  private void addSeries(SpreadsheetDataSource ds,
      ADCDataset dataset, Series series) throws MissingDataException {
    Integer row = ROW.get(series);
    if (row == null) {
      throw new RuntimeException("no row configured for series " + series);
    }
    List<String> columns = getColumns(ds);
    for (int col = 0; col < columns.size(); col++) {
      String s = ds.select(row, col + 3).asString();
      try {
        Double value = new Double(s);
        dataset.addValue(value, series.toString(), columns.get(col));
      } catch (Exception e) {
        dataset.addValue(null, series.toString(), columns.get(col));
      }
    }
  }

  private List<String> getColumns(SpreadsheetDataSource ds) throws MissingDataException {
    List<String> columns = Lists.newArrayList();
    for (int col = 3; true; col++) {
      String s = ds.select(0, col).asString();
      if (StringUtils.isBlank(s)) {
        break;
      }
      columns.add(s);
    }
    return columns;
  }

  private static double getTarget(SpreadsheetDataSource ds, ChartType type) {
    try {
      Series series = getTargetSeries(type);
      return ds.select(ROW.get(series), 1).asDouble();
    } catch(MissingDataException e) {
      throw new RuntimeException(e);
    }
    
  }

  private static String getTargetBy(SpreadsheetDataSource ds, ChartType type) {
    try {
      Series series = getTargetSeries(type);
      return ds.select(ROW.get(series), 2).asInteger().toString();
    } catch (MissingDataException e) {
      throw new RuntimeException(e);
    }
  }

  public String getDefaultTitle(ChartType type) {
    return TITLES.get(type).getTitle();
  }

  public String getDefaultRangeAxisTitle(ChartType type) {
    return TITLES.get(type).getValueAxisLabel();
  }

  @Override
  public AttributeMap defaults(ChartType type) {
    return new AttributeMap.Builder().
        put(Attribute.TITLE, getDefaultTitle(type)).
        put(Attribute.X_AXIS_LABEL, "").
        put(Attribute.Y_AXIS_LABEL, getDefaultRangeAxisTitle(type)).
        build();
  }

  @Override
  protected Drawable getDrawable(JFreeContext ctx) {
    return new TrackingTowardsTargets().createChart(
        ctx.type(), getTarget(ctx.datasource(), ctx.type()),
        (ADCDataset)ctx.dataset(), new Dimension(750, 500));
  }

  @Override
  protected String getCsv(JFreeContext ctx) {
    final CategoryDataset dataset = (CategoryDataset)ctx.dataset();
    final StringWriter sw = new StringWriter();
    try {
      final CsvListWriter csv = new CsvListWriter(sw,
          CsvPreference.STANDARD_PREFERENCE);
      @SuppressWarnings("unchecked")
      List<String> columnKeys = dataset.getColumnKeys();
      @SuppressWarnings("unchecked")
      List<String> rowKeys = dataset.getRowKeys();
      final List<String> heading = ImmutableList.<String>builder()
          .add(format("%s %s", ctx.region(), ctx.type()))
          .add(format("%% Target by " +
              getTargetBy(ctx.datasource(), ctx.type())))
          .addAll(columnKeys)
          .build();
      csv.write(heading);
      final double target = getTarget(ctx.datasource(), ctx.type());
      for (String row : rowKeys) {
        List<String> line = Lists.newLinkedList();
        line.add(row);
        line.add(format("%.0f", target * 100));
        for (String col : columnKeys) {
          Number n = dataset.getValue(row, col);
          line.add(n == null ? "" : format("%.0f", n.doubleValue()*100));
        }
        csv.write(line);
      }
      csv.close();
    } catch (IOException e) {
      // How on earth would you get an IOException with a StringWriter?
      throw new RuntimeException(e);
    }
    return sw.toString();
  }

  @Override
  public Set<SubstitutionKey> substitutionKeys() {
    return ImmutableSet.<SubstitutionKey>builder().
        addAll(super.substitutionKeys()).add(S_TARGET).add(S_BY).build();
  }

}
