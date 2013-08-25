package boxrenderer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class XmlUtils {

    public static Document parse(File file)
            throws SAXException, IOException, ParserConfigurationException {
        return getDocumentBuilder().parse(file);
    }

    public static Document parse(InputSource source) 
            throws ParserConfigurationException, SAXException, IOException {
        return getDocumentBuilder().parse(source);
    }

    public static Document newDocument() throws ParserConfigurationException {
        return getDocumentBuilder().newDocument();
    }

    public static DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        return dbFactory.newDocumentBuilder();
    }

    public static String serialize(Node doc) throws Exception {
        StringWriter outText = new StringWriter();
        serialize(new DOMSource(doc), outText);
        return outText.toString();
    }

    public static void serialize(Source source, Writer writer) throws Exception {
        serialize(source, writer,
                ImmutableMap.of(OutputKeys.ENCODING, "UTF-8", OutputKeys.INDENT, "yes"));
    }

    public static void serialize(Source source, Writer writer,
            Map<String, String> properties) throws Exception {
        StreamResult sr = new StreamResult(writer);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t = null;
        t = tf.newTransformer();
        for(Map.Entry<String, String> me : properties.entrySet()) {
            t.setOutputProperty(me.getKey(), me.getValue());
        }
        t.transform(source,sr);
    }

    public static NodeList evalXpath(String xpath, Object item) throws XPathExpressionException {
        XPath xp = XPathFactory.newInstance().newXPath();
        XPathExpression expr = xp.compile(xpath);
        return (NodeList)expr.evaluate(item, XPathConstants.NODESET);
    }

    public static Map<String, String> getAttributes(Node node) {
        NamedNodeMap map = node.getAttributes();
        Map<String, String> result = Maps.newHashMap();
        if(map!=null) {
            for (int i=0; i<map.getLength();i++) {
                result.put(map.item(i).getNodeName(), map.item(i).getNodeValue());
            }
        }
        return result;
    }

    public static Document xslt(InputStream stylesheet, Document input) throws FileNotFoundException,
    TransformerException, ParserConfigurationException  {
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer(new StreamSource(stylesheet));
        Document result = newDocument();
        DOMResult domResult = new DOMResult(result); 
        transformer.transform(new DOMSource(input), domResult);
        return result;
    }
}
