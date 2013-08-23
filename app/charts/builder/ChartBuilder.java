package charts.builder;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Document;

import charts.ChartRenderer;
import charts.Dimensions;
import charts.spreadsheet.DataSource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class ChartBuilder {

  private static List<ChartTypeBuilder> BUILDERS =
    new ImmutableList.Builder<ChartTypeBuilder>()
      .add(new MarineSpreadsheetChartBuilder())
      .add(new CotsOutbreakSpreadsheetBuilder())
      .add(new AnnualRainfallChartBuilder())
      .build();

  private final List<DataSource> datasources;

  public ChartBuilder(List<DataSource> datasources) {
    this.datasources = datasources;
  }

  public List<Chart> getCharts(ChartType type, Map<String, String[]> query) {
    final List<Chart> result = Lists.newLinkedList();
    for (final ChartTypeBuilder builder : BUILDERS) {
      if (builder.canHandle(type, datasources)) {
        result.addAll(builder.build(datasources, query));
      }
    }
    // make sure charts are sorted by region
    // https://github.com/uq-eresearch/aorra/issues/44
    Collections.sort(result, new Comparator<Chart>() {
      @Override
      public int compare(Chart c1, Chart c2) {
        if (getRegion(c1) == null) {
          if (getRegion(c2) == null)
            return 0;
          return -1;
        } else {
          return getRegion(c1).compareTo(getRegion(c2));
        }
      }
      private Region getRegion(Chart c) {
        if (c.getDescription() == null)
          return null;
        return c.getDescription().getRegion();
      }
    });
    return result;
  }

  public List<Chart> getCharts(Map<String, String[]> query) {
    return getCharts(null, query);
  }

}
