package charts.builder;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.IOException;
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
import charts.representations.Format;
import charts.representations.Representation;

public abstract class AbstractChart implements Chart {

  private final Map<String, String[]> query;

  public AbstractChart(Map<String, String[]> query) {
    this.query = query;
  }

  @Override
  public abstract ChartDescription getDescription();

  public abstract Dimensions getChart();

  public abstract String getCSV() throws UnsupportedFormatException;

  @Override
  public Representation outputAs(Format format)
      throws UnsupportedFormatException {
    switch (format) {
    case CSV:
      return format.createRepresentation(getCSV());
    case SVG:
      return format.createRepresentation(
          renderSVG(getChart()));
    case PNG:
      return format.createRepresentation(
          renderPNG(getChart(),
              getFloat(query.get("width")),
              getFloat(query.get("height"))));
    }
    throw new Chart.UnsupportedFormatException();
  }

  protected String renderSVG(Dimensions d) {
    return (new ChartRenderer(d)).render();
  }

  protected byte[] renderPNG(Dimensions d, Float width, Float height) {
    try {
      Document doc = toDocument(renderSVG(d), false);
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      PNGTranscoder t = new PNGTranscoder();
      if (width != null) {
        t.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, width);
      }
      if (height != null) {
        t.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, height);
      }
      t.transcode(new TranscoderInput(doc), new TranscoderOutput(os));
      return os.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (TranscoderException e) {
      throw new RuntimeException(e);
    }
  }

  protected Float getFloat(String[] values) {
    try {
      return Float.parseFloat(values[0]);
    } catch (Exception e) {
      return null;
    }
  }

  private Document toDocument(String svg, boolean relativeDimensions)
      throws IOException{
    // Turn back into DOM
    String parserName = XMLResourceDescriptor.getXMLParserClassName();
    SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parserName);
    Document doc = f.createDocument("file:///test.svg",
        new CharArrayReader(svg.toCharArray()));
    String h = doc.getDocumentElement().getAttribute("height");
    String w = doc.getDocumentElement().getAttribute("width");
    doc.getDocumentElement().setAttributeNS(null, "viewbox",
        String.format("0 0 %s %s", w, h));
    if(relativeDimensions) {
        doc.getDocumentElement().setAttribute("height", "100%");
        doc.getDocumentElement().setAttribute("width", "100%");
    }
    return doc;
  }
}
