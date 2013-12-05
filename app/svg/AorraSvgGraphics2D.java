package svg;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.Stroke;
import java.io.IOException;
import java.io.InputStream;

import org.apache.batik.svggen.DOMGroupManager;
import org.apache.batik.svggen.ExtensionHandler;
import org.apache.batik.svggen.ImageHandler;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.tika.io.IOUtils;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class AorraSvgGraphics2D extends SVGGraphics2D {

    private final String SCRIPT_NAME = "tooltip.js";

    private boolean inject = false;

    public AorraSvgGraphics2D(Document domFactory,
            ImageHandler imageHandler,
            ExtensionHandler extensionHandler,
            boolean textAsShapes) {
        super(domFactory, imageHandler, extensionHandler, textAsShapes);
    }

    @Override
    public Element getRoot(Element svgRoot) {
        svgRoot = super.getRoot(svgRoot);
        if(svgRoot != null && SVG_SVG_TAG.equals(svgRoot.getTagName()) && inject) {
            injectTooltip(svgRoot);
        }
        return svgRoot;
    }

    /**
     * @see #draw
     */
    public void drawWithTooltip(Shape s, String tooltip) {
        // Only BasicStroke can be converted to an SVG attribute equivalent.
        // If the GraphicContext's Stroke is not an instance of BasicStroke,
        // then the stroked outline is filled.
        Stroke stroke = gc.getStroke();
        if (stroke instanceof BasicStroke) {
            Element svgShape = shapeConverter.toSVG(s);
            if (svgShape != null) {
                
                domGroupManager.addElement(addTooltip(svgShape, tooltip), DOMGroupManager.DRAW);
            }
        } else {
            Shape strokedShape = stroke.createStrokedShape(s);
            fillWithTooltip(strokedShape, tooltip);
        }
    }

    /**
     * @see #fill
     */
    public void fillWithTooltip(Shape s, String tooltip) {
        Element svgShape = shapeConverter.toSVG(s);
        if (svgShape != null) {
            domGroupManager.addElement(addTooltip(svgShape, tooltip), DOMGroupManager.FILL);
        }
    }

    private Element addTooltip(Element svgElement, String tooltip) {
        inject = true;
        svgElement.setAttributeNS(null, SVG_ONMOUSEMOVE_ATTRIBUTE,
                "showTooltip(evt, '"+StringEscapeUtils.escapeJavaScript(tooltip)+"')");
        svgElement.setAttributeNS(null, SVG_ONMOUSEOUT_ATTRIBUTE, "hideTooltip(evt)");
        return svgElement;
    }

    private String script() {
        try {
            InputStream in = this.getClass().getResourceAsStream(SCRIPT_NAME);
            if(in == null) {
                throw new RuntimeException(String.format("resource not found %s", SCRIPT_NAME));
            }
            return IOUtils.toString(in);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void injectTooltip(Element svgRoot) {
        Document doc = svgRoot.getOwnerDocument();
        Element svgScript = doc.createElementNS(null, SVG_SCRIPT_TAG);
        CDATASection cdata = doc.createCDATASection(script());
        svgScript.appendChild(cdata);
        svgRoot.insertBefore(svgScript, svgRoot.getFirstChild());
        svgRoot.setAttributeNS(null, SVG_ONLOAD_ATTRIBUTE, "init(evt)");
        Element ttBackground = doc.createElementNS(null, SVG_RECT_TAG);
        ttBackground.setAttributeNS(null, "id", "tooltip_bg");
        ttBackground.setAttributeNS(null, "class", "tooltip_bg");
        ttBackground.setAttributeNS(null, "height", "17");
        ttBackground.setAttributeNS(null, "width", "1");
        ttBackground.setAttributeNS(null, "visibility", "hidden");
        ttBackground.setAttributeNS(null, "style", "fill: white;stroke: black;stroke-width: 1;opacity: 0.85;");
        Element ttText = doc.createElementNS(null, SVG_TEXT_TAG);
        ttText.setAttributeNS(null, "id", "tooltip");
        ttText.setAttributeNS(null, "class", "tooltip");
        ttText.setAttributeNS(null, "visibility", "hidden");
        ttText.setAttributeNS(null, "style", "font-size: 10px;font-family: sans-serif;stroke: none;");
        ttText.appendChild(doc.createTextNode("tooltip"));
        svgRoot.appendChild(ttBackground);
        svgRoot.appendChild(ttText);
    }

}
