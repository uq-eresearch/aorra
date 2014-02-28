package charts.builder.spreadsheet;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;

import charts.Chart;
import charts.ChartType;
import charts.Region;
import charts.builder.ChartTypeBuilder;
import charts.builder.DataSource;
import charts.builder.DataSource.MissingDataException;
import charts.jfree.AttributeMap;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public abstract class AbstractBuilder implements ChartTypeBuilder {

  private static final SubstitutionKey TYPE = new SubstitutionKey("type", "the chart type label" +
      " e.g. Crown of Thorns Outbreak for chart type COTS_OUTBREAK", new SubstitutionKey.Val() {
        @Override
        public String value(Context ctx) {
          return ctx.type().getLabel();
        }
      });
  private static final SubstitutionKey REGION = new SubstitutionKey("region", "the region name" +
      " e.g. Wet Tropics", new SubstitutionKey.Val() {
        @Override
        public String value(Context ctx) {
          return ctx.region().getProperName();
        }
      });

  public static class Context {
    private final SpreadsheetDataSource datasource;
    private final ChartType type;
    private final Region region;
    private final Map<String, String> parameters;
    public Context(SpreadsheetDataSource datasource, ChartType type,
        Region region, Map<String, String> parameters) {
      this.datasource = datasource;
      this.type = type;
      this.region = region;
      this.parameters = parameters;
    }
    public ChartType type() {
      return type;
    }
    public Region region() {
      return region;
    }
    public SpreadsheetDataSource datasource() {
      return datasource;
    }
    public Map<String, String> parameters() {
      return parameters;
    }
    @Override
    public String toString() {
      return String.format("%s - %s - %s", type, region, parameters);
    }
  }

  private final List<ChartType> types;

  public AbstractBuilder(ChartType type) {
    types = Lists.newArrayList(type);
  }

  public AbstractBuilder(List<ChartType> types) {
    this.types = types;
  }

  public boolean supports(ChartType type) {
    return types.contains(type);
  }

  protected abstract boolean canHandle(SpreadsheetDataSource datasource);

  protected abstract Chart build(Context context);

  protected Map<String, List<String>> getParameters(SpreadsheetDataSource datasource, ChartType type) {
      return Maps.newHashMap();
  }

  protected Map<String, List<String>> getParameters(DataSource datasource, ChartType type) {
    return getParameters((SpreadsheetDataSource)datasource, type);
  }

  @Override
  public List<Chart> build(DataSource datasource, ChartType type,
      List<Region> regions, Map<String, String> parameters) {
    List<Chart> charts = Lists.newArrayList();
    if((type == null) || types.contains(type)) {
      if (datasource instanceof SpreadsheetDataSource) {
        for(int i = 0; i<((SpreadsheetDataSource)datasource).sheets();i++) {
          SpreadsheetDataSource ds = ((SpreadsheetDataSource)datasource).toSheet(i);
          if(canHandle(ds)) {
            charts.addAll(build(ds, type, regions, parameters));
          }
        }
      }
    }
    return charts;
  }

  private List<Chart> build(SpreadsheetDataSource datasource, ChartType type,
      List<Region> regions, Map<String, String> parameters) {
    checkNotNull(regions, "Regions can be empty, but not null.");
    Map<String, List<String>> m = Maps.newHashMap();
    Map<String, List<String>> supportedParameters = getParameters(datasource, type);
    for(String key : supportedParameters.keySet()) {
        if ((parameters != null) && parameters.containsKey(key)) {
            m.put(key, asList(parameters.get(key)));
        } else {
            m.put(key, supportedParameters.get(key));
        }
    }
    final List<ChartType> t =
        type == null ? types : asList(type);
    final List<Region> r =
        regions.isEmpty() ? asList(Region.values()) : regions;
    final List<Chart> charts = Lists.newLinkedList();
    for (Object o : ChartPermutations.apply(t, r, m)) {
      ChartPermutation p = (ChartPermutation)o;
      final Chart chart = build(context(datasource, p.chartType(), p.region(), p.javaParams()));
      if (chart != null) {
        charts.add(chart);
      }
    }
    return charts;
  }

  protected boolean cellEquals(SpreadsheetDataSource datasource, String s, String cellreference) {
    try {
      return s.equals(StringUtils.strip(datasource.select(cellreference).asString()));
    } catch (MissingDataException e) {
      return false;
    }
  }

  protected ChartConfigurator configurator(Context ctx) {
    return new ChartConfigurator(defaults(ctx.type()),
        ctx.datasource(), new StrSubstitutor(substitutions(ctx)));
  }

  private Map<String, String> substitutions(Context ctx) {
    Map<String, String> m = Maps.newHashMap();
    for(SubstitutionKey key : substitutionKeys()) {
      m.put(key.getName(), key.getValue(ctx));
    }
    return m;
  }

  public AttributeMap defaults(ChartType type) {
    throw new ChartConfigurationNotSupported(this);
  }

  public Set<SubstitutionKey> substitutionKeys() {
    return ImmutableSet.of(TYPE, REGION);
  }

  ChartType type() {
    if(types.size() == 1) {
      return types.get(0);
    } else {
      throw new RuntimeException(String.format("this builder (%s) supports multiple types",
          this.getClass().getName()));
    }
  }

  protected Context context(SpreadsheetDataSource datasource, ChartType type,
      Region region, Map<String, String> parameters) {
    return new Context(datasource, type, region, parameters);
  }

  protected String formatNumber(String format, Number n) {
    return n!=null?String.format(format,n.doubleValue()):"";
  }
}
