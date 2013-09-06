package charts

import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.RenderingHints
import java.io.CharArrayWriter
import org.apache.batik.dom.svg.SVGDOMImplementation
import org.apache.batik.svggen.DefaultExtensionHandler
import org.apache.batik.svggen.ImageHandlerBase64Encoder
import org.apache.batik.svggen.SVGGraphics2D
import org.w3c.dom.svg.SVGDocument

class ChartRenderer(val chart: Drawable) {

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

    val d = chart.getDimension(g2)
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON)
    chart.draw(g2)
    g2.setSVGCanvasSize(d)

    val cw = new CharArrayWriter()
    g2.stream(cw, true)
    g2.dispose

    cw.toString()
  }

  protected def registerFonts() {
    val ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    Seq("Regular", "Bold").map(f => s"LiberationSans-$f.ttf").foreach { font =>
      val fontStream = classOf[ChartRenderer].getResourceAsStream(font)
      try {
        ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, fontStream))
      } catch { // It's in the classpath, so failure should never happen
        case e: Throwable => throw new RuntimeException(e)
      }
    }
  }

}
