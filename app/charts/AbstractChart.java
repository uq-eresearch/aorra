package charts;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import net.hanjava.svg.SVG2EMF;

import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.commons.compress.utils.IOUtils;
import org.docx4j.convert.in.xhtml.XHTMLImporter;
import org.docx4j.dml.wordprocessingDrawing.Inline;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage;
import org.w3c.dom.Document;

import play.api.libs.Files.TemporaryFile;
import play.api.libs.Files.TemporaryFile$;
import charts.representations.Format;
import charts.representations.Representation;

public abstract class AbstractChart implements Chart {

  private final Map<String, String[]> query;

  public AbstractChart(Map<String, String[]> query) {
    this.query = query;
  }

  @Override
  public abstract ChartDescription getDescription();

  public abstract Drawable getChart();

  public abstract String getCSV() throws UnsupportedFormatException;

  public abstract String getCommentary() throws UnsupportedFormatException;

  @Override
  public Representation outputAs(Format format)
      throws UnsupportedFormatException {
    switch (format) {
    case CSV:
      return format.createRepresentation(getCSV());
    case DOCX:
      return format.createRepresentation(
          renderDOCX(getChart()));
    case EMF: // For embedding in Word documents
      return format.createRepresentation(
          renderEMF(getChart()));
    case HTML:
      return format.createRepresentation(getCommentary());
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

  protected String renderSVG(Drawable d) {
    return (new ChartRenderer(d)).render();
  }

  protected byte[] renderDOCX(Drawable d) {
    final TemporaryFile tf = TemporaryFile$.MODULE$.apply("aorra_docx_", "");
    try {
      WordprocessingMLPackage wordMLPackage =
          WordprocessingMLPackage.createPackage();
      // Insert image
      org.docx4j.wml.P p;
      try {
        Inline inline = BinaryPartAbstractImage
            .createImagePart(wordMLPackage, renderEMF(d))
            .createImageInline(null, null, 0, 1, false);
        // From samples/ImageAdd.java in docx4j
        // Now add the inline in w:p/w:r/w:drawing
        org.docx4j.wml.ObjectFactory factory = Context.getWmlObjectFactory();
        p = factory.createP();
        org.docx4j.wml.R run = factory.createR();
        p.getContent().add(run);
        org.docx4j.wml.Drawing drawing = factory.createDrawing();
        run.getContent().add(drawing);
        drawing.getAnchorOrInline().add(inline);
      } catch (Exception e) {
        // Not really sure what could go wrong here
        throw new RuntimeException(e);
      }
      wordMLPackage.getMainDocumentPart()
        .getContent()
        .add(p);
      // Insert commentary
      try {
        final String xhtml = "<div>"+getCommentary()+"</div>";
          wordMLPackage.getMainDocumentPart()
            .getContent()
            .addAll(XHTMLImporter.convert(xhtml, null, wordMLPackage));
      } catch (UnsupportedFormatException e) {
        // Skip commentary
      }
      wordMLPackage.save(tf.file());
    } catch (InvalidFormatException e) {
      throw new RuntimeException(e);
    } catch (Docx4JException e) {
      throw new RuntimeException(e);
    }
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      final InputStream is = new FileInputStream(tf.file());
      IOUtils.copy(is, os);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return os.toByteArray();
  }

  protected byte[] renderEMF(Drawable d) {
    final InputStream is = new ByteArrayInputStream(renderSVG(d).getBytes());
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      SVG2EMF.convert("", is, os);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return os.toByteArray();
  }

  protected byte[] renderPNG(Drawable d, Float width, Float height) {
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
