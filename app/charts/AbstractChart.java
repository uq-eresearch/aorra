package charts;

import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.hanjava.svg.SVG2EMF;

import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.fop.render.ps.EPSTranscoder;
import org.apache.fop.svg.PDFTranscoder;
import org.docx4j.convert.in.xhtml.XHTMLImporter;
import org.docx4j.dml.wordprocessingDrawing.Inline;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import play.api.libs.Files.TemporaryFile;
import play.api.libs.Files.TemporaryFile$;
import charts.representations.Format;
import charts.representations.Representation;

public abstract class AbstractChart implements Chart {

  private final Dimension queryDimensions;

  private String svgDocument = null;

  public AbstractChart(Dimension queryDimensions) {
    this.queryDimensions = queryDimensions;
  }

  @Override
  public abstract ChartDescription getDescription();

  public abstract Drawable getChart();

  public abstract String getCSV() throws UnsupportedFormatException;

  @Override
  public Representation outputAs(Format format)
      throws UnsupportedFormatException {
    switch (format) {
    case CSV:
      return format.createRepresentation(getCSV());
    case EMF: // For embedding in Word documents
      return format.createRepresentation(
          renderEMF(getChart()));
    case SVG:
      return format.createRepresentation(
          renderSVG(getChart(), queryDimensions));
    case PNG:
      return format.createRepresentation(
          renderPNG(getChart(), queryDimensions));
    case EPS:
      return format.createRepresentation(
          renderEPS(getChart(), queryDimensions));
    case PDF:
        return format.createRepresentation(
          renderPDF(getChart(), queryDimensions));
    }
    throw new Chart.UnsupportedFormatException();
  }

  protected byte[] renderPDF(Drawable d, Dimension dimensions) {
    try {
        Document doc = toDocument(getDrawableDocument(d), dimensions);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PDFTranscoder t = new PDFTranscoder();
        if (dimensions.getWidth() > 0.0) {
            t.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH,
                    (float) dimensions.getWidth());
        }
        if (dimensions.getHeight() > 0.0) {
            t.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT,
                    (float) dimensions.getHeight());
        }
        t.transcode(new TranscoderInput(doc), new TranscoderOutput(os));
        return os.toByteArray();
    } catch(IOException e) {
        throw new RuntimeException(e);
    } catch(TranscoderException e) {
        throw new RuntimeException(e);
    }
  }

  protected String renderEPS(Drawable d, Dimension dimensions) {
    try {
        Document doc = toDocument(getDrawableDocument(d), dimensions);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        EPSTranscoder t = new EPSTranscoder();
        if (dimensions.getWidth() > 0.0) {
            t.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH,
                    (float) dimensions.getWidth());
        }
        if (dimensions.getHeight() > 0.0) {
            t.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT,
                    (float) dimensions.getHeight());
        }
        t.transcode(new TranscoderInput(doc), new TranscoderOutput(os));
        return os.toString();
    } catch(IOException e) {
        throw new RuntimeException(e);
    } catch(TranscoderException e) {
        throw new RuntimeException(e);
    }
  }

  protected byte[] renderEMF(Drawable d) {
    final InputStream is = new ByteArrayInputStream(
        getDrawableDocument(d).getBytes());
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      SVG2EMF.convert("", is, os);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return os.toByteArray();
  }

  protected byte[] renderPNG(Drawable d, Dimension dimensions) {
    try {
      Document doc = toDocument(getDrawableDocument(d), dimensions);
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      PNGTranscoder t = new PNGTranscoder();
      if (dimensions.getWidth() > 0.0) {
        t.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH,
            (float) dimensions.getWidth());
      }
      if (dimensions.getHeight() > 0.0) {
        t.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT,
            (float) dimensions.getHeight());
      }
      t.transcode(new TranscoderInput(doc), new TranscoderOutput(os));
      return os.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (TranscoderException e) {
      throw new RuntimeException(e);
    }
  }

  protected String renderSVG(Drawable d, Dimension dimensions) {
    final Document doc;
    try {
      doc = new SAXSVGDocumentFactory(
          XMLResourceDescriptor.getXMLParserClassName())
          .createDocument("file:///test.svg",
              new CharArrayReader(getDrawableDocument(d).toCharArray()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    scaleSvg(doc, dimensions);
    final StringWriter sw = new StringWriter();
    try {
      TransformerFactory.newInstance().newTransformer()
        .transform(new DOMSource(doc), new StreamResult(sw));
    } catch (TransformerException e) {
      throw new RuntimeException(e);
    }
    return sw.toString();
  }

  protected String getDrawableDocument(Drawable d) {
    if (svgDocument == null) {
      svgDocument = (new ChartRenderer(d)).render();
    }
    return svgDocument;
  }

  protected Float getFloat(String[] values) {
    try {
      return Float.parseFloat(values[0]);
    } catch (Exception e) {
      return null;
    }
  }

  private Document scaleSvg(Document doc, Dimension dimensions) {
    final Element root = doc.getDocumentElement();
    final String h = root.getAttribute("height");
    final String w = root.getAttribute("width");
    root.setAttributeNS(null, "viewBox",
        String.format("0 0 %s %s", w, h));
    final Dimension scaledDimensions =
        scaleDimensions(extractSvgDimensions(doc), dimensions);
    // Set requested height / width
    final long newH = Math.round(scaledDimensions.getHeight());
    final long newW = Math.round(scaledDimensions.getWidth());
    root.setAttribute("height", newH+"");
    root.setAttribute("width", newW+"");
    return doc;
  }

  private Dimension extractSvgDimensions(Document svg) {
    final Element root = svg.getDocumentElement();
    return new Dimension(
        Integer.parseInt(root.getAttribute("width")),
        Integer.parseInt(root.getAttribute("height")));
  }

  /**
   * Sets missing dimensions to correct version for aspect ratio.
   *
   * @param dOrig Original image dimensions
   * @param dNew Proposed new image dimensions
   * @return Dimensions where neither height or width is 0
   */
  private Dimension scaleDimensions(Dimension dOrig, Dimension dNew) {
    final Dimension dScaled = new Dimension();
    if (dNew.getWidth() > 0) {
      if (dNew.getHeight() > 0) {
        return dNew; // Complete already
      }
      // Get new height in proportion
      dScaled.setSize(
          dNew.getWidth(),
          dNew.getWidth() * (dOrig.getHeight() / dOrig.getWidth()));
    } else {
      if (dNew.getHeight() <= 0) {
        return dOrig; // Blank new => use original
      }
      // Get new width in proportion
      dScaled.setSize(
          dNew.getHeight() * (dOrig.getWidth() / dOrig.getHeight()),
          dNew.getHeight());
    }
    return dScaled;
  }

  private Document toDocument(String svg, Dimension dimensions)
      throws IOException {
    // Turn back into DOM
    String parserName = XMLResourceDescriptor.getXMLParserClassName();
    SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parserName);
    Document doc = f.createDocument("file:///test.svg",
        new CharArrayReader(svg.toCharArray()));
    scaleSvg(doc, dimensions);
    return doc;
  }
}
