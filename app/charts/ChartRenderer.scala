package charts

import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import org.apache.batik.dom.svg.SVGDOMImplementation
import org.w3c.dom.svg.SVGDocument
import org.apache.batik.svggen.SVGGraphics2D
import java.io.CharArrayWriter
import java.awt.GraphicsEnvironment
import java.awt.Font
import org.apache.batik.svggen.DefaultExtensionHandler
import org.apache.batik.svggen.ImageHandlerBase64Encoder

class ChartRenderer(val chart: Dimensions) {

  def render() = {
    // Make sure our fonts are available
    registerFonts()
    // Get a DOMImplementation.
    val impl = SVGDOMImplementation.getDOMImplementation()
    val svgNS = SVGDOMImplementation.SVG_NAMESPACE_URI
    val doc = impl.createDocument(svgNS, "svg", null).asInstanceOf[SVGDocument]

    // Create an instance of the SVG Generator.
    val g2 = new SVGGraphics2D(doc,
        // Turn BufferedImage textures into "data:image/png" URIs
        new ImageHandlerBase64Encoder(),
        new DefaultExtensionHandler(),
        true // Render text as paths to avoid missing fonts
    )
    g2.setFont(new Font("Liberation Sans", Font.PLAIN, 11))

    val d = chart.getDimension
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON)
    chart.draw(g2, new Rectangle2D.Double(0, 0, d.width, d.height))
    g2.setSVGCanvasSize(d)

    val cw = new CharArrayWriter()
    g2.stream(cw, true)
    g2.dispose

    cw.toString()
  }

  protected def registerFonts() {
    val ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    Seq("Regular", "Bold").map(f => s"LiberationSans-$f.ttf").foreach { font =>
      val fontStream = classOf[GraphUtils].getResourceAsStream(font)
      try {
        ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, fontStream))
      } catch { // It's in the classpath, so failure should never happen
        case e: Throwable => throw new RuntimeException(e)
      }
    }
  }

}
