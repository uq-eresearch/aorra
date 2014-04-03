package charts.builder.spreadsheet;

import java.awt.Color;
import java.io.IOException;
import java.util.Map;

import org.supercsv.io.CsvListWriter;

import charts.AbstractChart;
import charts.Chart;
import charts.ChartDescription;
import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource;
import charts.builder.DataSource.MissingDataException;
import charts.builder.csv.Csv;
import charts.builder.csv.CsvWriter;
import charts.graphics.BeerCoaster;
import charts.graphics.BeerCoaster.Category;
import charts.graphics.BeerCoaster.Condition;
import charts.graphics.BeerCoaster.Indicator;
import charts.graphics.Colors;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;
import charts.jfree.AttributedDataset;
import charts.jfree.AttributedDatasetImpl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class MarineBuilder extends AbstractBuilder {

  private static final ImmutableMap<Region, Integer> OFFSETS =
      new ImmutableMap.Builder<Region, Integer>()
        .put(Region.CAPE_YORK, 0)
        .put(Region.WET_TROPICS, 1)
        .put(Region.BURDEKIN, 2)
        .put(Region.MACKAY_WHITSUNDAY, 3)
        .put(Region.FITZROY, 4)
        .put(Region.BURNETT_MARY, 5)
        .put(Region.GBR, 6)
        .build();

  private static enum Field {
    WATER_QUALITY(Category.WATER_QUALITY, "E", 9),
    CORAL(Category.CORAL, "P", 9),
    SEAGRASS(Category.SEAGRASS, "J", 9),
    CHLOROPHYLL_A(Indicator.CHLOROPHYLL_A, "C", 9),
    TOTAL_SUSPENDED_SOLIDS(Indicator.TOTAL_SUSPENDED_SOLIDS, "D", 9),
    SETTLEMENT(Indicator.SETTLEMENT, "M", 9),
    JUVENILE(Indicator.JUVENILE, "O", 9),
    ALGAE(Indicator.ALGAE, "N", 9),
    COVER(Indicator.COVER, "L", 9),
    ABUNDANCE(Indicator.ABUNDANCE, "G", 9),
    REPRODUCTION(Indicator.REPRODUCTION, "H", 9),
    NUTRIENT_STATUS(Indicator.NUTRIENT_STATUS, "I", 9),
    OVERALL("F", 20)
    ;
    private Category category;
    private Indicator indicator;
    private String column;
    private int row;

    private Field(String column, int row) {
      this.column = column;
      this.row = row;
    }

    private Field(Category category, String column, int row) {
      this(column, row);
      this.category = category;
    }

    private Field(Indicator indicator, String column, int row) {
      this(column, row);
      this.indicator = indicator;
    }

    public void setCondition(BeerCoaster bc, DataSource datasource, Region region) {
      Condition c = condition(datasource, region);
      if(category != null) {
        bc.setCondition(category, c);
      } else if(indicator != null) {
        bc.setCondition(indicator, c);
      } else {
        bc.setOverallCondition(c);
      }
    }

    private Condition condition(DataSource datasource, Region region) {
      return determineCondition(getValue(datasource, column, row, OFFSETS.get(region)));
    }

    private Double getValue(DataSource ds, String column, int row, int rowOffset) {
      try {
        return ds.select(String.format("%s%s", column, row+rowOffset)).asDouble();
      } catch(MissingDataException e) {
        return null;
      }
    }

    private Condition determineCondition(Double index) {
      if (index == null) {
        return Condition.NOT_EVALUATED;
      } else if (index >= 80) {
        return Condition.VERY_GOOD;
      } else if (index >= 60) {
        return Condition.GOOD;
      } else if (index >= 40) {
        return Condition.MODERATE;
      } else if (index >= 20) {
        return Condition.POOR;
      } else {
        return Condition.VERY_POOR;
      }
    }
  }

  public MarineBuilder() {
    super(ChartType.MARINE);
  }

  @Override
  public boolean canHandle(SpreadsheetDataSource datasource) {
    return isMarineSpreadsheet(datasource);
  }

  private boolean isMarineSpreadsheet(DataSource datasource) {
    try {
      return "MARINE SUMMARY".equalsIgnoreCase(
          datasource.select("B18").getValue());
    } catch (MissingDataException e) {
      return false;
    }
  }

  public abstract static class MarineChart extends AbstractChart {
    public abstract BeerCoaster beercoaster();
  }

  @Override
  public Chart build(final Context context) {
    AttributedDataset a = new AttributedDatasetImpl();
    configurator(context).configure(a, context.type());
    final BeerCoaster beercoaster = getDrawable(context.datasource(), context.region(), a);
    if (beercoaster != null) {

      final ChartDescription description = new ChartDescription(context.type(), context.region());
      return new MarineChart() {
        @Override
        public ChartDescription getDescription() {
          return description;
        }

        @Override
        public Drawable getChart() {
          return beercoaster;
        }

        @Override
        public BeerCoaster beercoaster() {
          return beercoaster;
        }

        @Override
        public String getCSV() {
          return Csv.write(new CsvWriter() {
            @Override
            public void write(CsvListWriter csv) throws IOException {
              csv.write("Overall marine condition", beercoaster
                  .getOverallCondition().getLabel());
              for (Category c : Category.values()) {
                csv.write(c.getName(), beercoaster.getCondition(c).getLabel());
                for (Indicator i : Indicator.values()) {
                  if (i.getCategory() == c) {
                    csv.write(i.getName(), beercoaster.getCondition(i).getLabel());
                  }
                }
              }
            }});
        }
      };
    } else {
      return null;
    }
  }

  private BeerCoaster getDrawable(DataSource datasource, Region region, AttributedDataset a) {
    BeerCoaster bc = new BeerCoaster(conditionColors(a));
    for(Field f : Field.values()) {
      f.setCondition(bc, datasource, region);
    }
    return bc;
  }

  private Map<Condition, Color> conditionColors(AttributedDataset a) {
    Map<Condition, Color> m = Maps.newHashMap();
    m.put(Condition.NOT_EVALUATED, a.get(Attribute.CONDITION_NOT_EVALUATED));
    m.put(Condition.VERY_GOOD, a.get(Attribute.CONDITION_VERY_GOOD));
    m.put(Condition.GOOD, a.get(Attribute.CONDITION_GOOD));
    m.put(Condition.MODERATE, a.get(Attribute.CONDITION_MODERATE));
    m.put(Condition.POOR, a.get(Attribute.CONDITION_POOR));
    m.put(Condition.VERY_POOR, a.get(Attribute.CONDITION_VERY_POOR));
    return m;
  }

  @Override
  public AttributeMap defaults(ChartType type) {
    return new AttributeMap.Builder().
        put(Attribute.CONDITION_NOT_EVALUATED, Colors.NOT_EVALUATED).
        put(Attribute.CONDITION_VERY_GOOD, Colors.VERY_GOOD).
        put(Attribute.CONDITION_GOOD, Colors.GOOD).
        put(Attribute.CONDITION_MODERATE, Colors.MODERATE).
        put(Attribute.CONDITION_POOR, Colors.POOR).
        put(Attribute.CONDITION_VERY_POOR, Colors.VERY_POOR).
        build();
  }

}
