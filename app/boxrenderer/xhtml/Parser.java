package boxrenderer.xhtml;

import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import boxrenderer.Box;
import boxrenderer.BreakBox;
import boxrenderer.ContentBox;
import boxrenderer.ContentBoxImpl;
import boxrenderer.FontManager;
import boxrenderer.Resolver;
import boxrenderer.TableBox;
import boxrenderer.TableCellBox;
import boxrenderer.TableRowBox;
import boxrenderer.TextBox;
import boxrenderer.XmlUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.osbcp.cssparser.CSSParser;
import com.osbcp.cssparser.PropertyValue;
import com.osbcp.cssparser.Rule;
import com.osbcp.cssparser.Selector;


public class Parser {

  private static final Logger logger = LoggerFactory.getLogger(Parser.class);

  private final Resolver resolver;

  private final List<String> inherited = ImmutableList.<String>builder()
      .add("font-family")
      .add("font-size")
      .add("font-weight")
      .add("color")
      .build();

  private final List<Rule> cssrules = Lists.newArrayList();

  private final FontManager fontManager;

  private final CssStyleFactory cssStyleFactory;

  public Parser(Resolver resolver) {
    this.resolver = resolver;
    fontManager = new FontManager(resolver);
    cssStyleFactory = new CssStyleFactory(resolver);
  }

  public Box parse(Document xhtml) throws Exception {
    cssrules.clear();
    Element elRoot = xhtml.getDocumentElement();
    return parse(null, elRoot);
  }

  private Box parse(Box box, Element elCurrent) throws Exception {
    NodeList list = elCurrent.getChildNodes();
    for (int i=0; i<list.getLength(); i++) {
      Node node = list.item(i);
      if (node instanceof Element) {
        Element el = (Element)list.item(i);
        String tag = el.getTagName().toLowerCase();
        if("body".equals(tag)) {
          box = setupBox(new ContentBoxImpl(), null, el);
          parse(box, el);
        } else if("table".equals(tag)) {
          ((ContentBox)box).addContent(parse(setupBox(new TableBox(), box, el), el));
        } else if("tr".equals(tag)) {
          ((TableBox)box).addRow((TableRowBox)parse(setupBox(new TableRowBox(), box, el), el));
        } else if("td".equals(tag)) {
          TableCellBox cellBox = new TableCellBox();
          setupBox(cellBox, box, el);
          ((TableRowBox)box).addCell(cellBox);
          parse(cellBox, el);
        } else if("span".equals(tag)) {
          ContentBoxImpl spanBox = new ContentBoxImpl();
          spanBox.setInline(true);
          ((ContentBox)box).addContent(parse(setupBox(spanBox, box, el), el));
        } else if("style".equals(tag)) {
          parseCss(el);
        } else if("br".equals(tag)) {
          ((ContentBox)box).addContent(new BreakBox());
        } else if("link".equals(tag)) {
          importCss(el);
        } else if("div".equals(tag)) {
          ContentBoxImpl divBox = new ContentBoxImpl();
          divBox.setInline(false);
          ((ContentBox)box).addContent(parse(setupBox(divBox, box, el), el));
        } else {
          parse(box, el);
        }
      } else if((node instanceof Text)) {
        // TODO add CDATA support
        String text = node.getTextContent();
        if(!StringUtils.isBlank(text)) {
          if(box instanceof ContentBox) {
            ((ContentBox)box).addContent(createTextBox(node, box));
          }
        }
      }
    }
    return box;
  }

  private Box setupBox(Box box, Box parent, Element el) throws Exception {
    box.setParent(parent);
    Map<String, String> attr = XmlUtils.getAttributes(el);
    box.setId(attr.get("id"));
    box.setTag(el.getTagName());
    box.setAttributes(attr);
    String cssClasses = attr.get("class");
    if (!StringUtils.isBlank(cssClasses)) {
      for (String cssClass : StringUtils.split(cssClasses)) {
        box.addCssClass(StringUtils.strip(cssClass));
      }
    }
    String style = StringUtils.defaultString(attr.get("style"));
    String border = attr.get("border");
    if (!StringUtils.isBlank(border)) {
      style += String.format(
          "border-width:%spx;border-color:red;border-collapse:separate;",
          border);
    }
    setupCss(box, style);
    applyCss(box);
    return box;
  }

  private Set<PropertyValue> getCss(Box box) {
    Map<String, PropertyValue> cssMap = getCss(box,
        new HashMap<String, PropertyValue>());
    Set<PropertyValue> result = new HashSet<PropertyValue>();
    result.addAll(cssMap.values());
    return result;
  }

  private Map<String, PropertyValue> getCss(Box box,
      Map<String, PropertyValue> result) {
    if (box == null) {
      return result;
    }
    List<Rule> rules = box.getCssRules();
    sortRules(rules);
    for (Rule rule : rules) {
      addProperties(rule.getPropertyValues(), result);
    }
    for (String inherit : inherited) {
      PropertyValue p = getInherited(inherit, box);
      if (p != null) {
        addProperty(p, result);
      }
    }
    // TODO also get property from parent if value set to inherit
    return result;
  }

  private PropertyValue getInherited(String name, Box box) {
    if (box == null) {
      logger.debug(String.format("nothing found for css property %s", name));
      return null;
    }
    List<Rule> rules = box.getCssRules();
    sortRules(rules);
    for (Rule rule : rules) {
      for (PropertyValue p : rule.getPropertyValues()) {
        if (name.equals(p.getProperty()) && !p.getValue().equals("inherit")) {
          logger.debug(String.format("found value %s for css property %s",
              p.getValue(), p.getProperty()));
          return p;
        }
      }
    }
    return getInherited(name, box.getParent());
  }

  private void sortRules(List<Rule> rules) {
    Collections.sort(rules, Collections.reverseOrder(new Comparator<Rule>() {
      @Override
      public int compare(Rule o1, Rule o2) {
        return new CssSpecificity(o1).compareTo(new CssSpecificity(o2));
      }
    }));
  }

  private void addProperties(List<PropertyValue> properties,
      Map<String, PropertyValue> result) {
    for (PropertyValue p : properties) {
      if (!result.containsKey(p.getProperty())) {
        result.put(p.getProperty(), p);
      }
    }
  }

  private void addProperty(PropertyValue p, Map<String, PropertyValue> result) {
    PropertyValue setValue = result.get(p.getProperty());
    if (setValue == null || "inherit".equals(setValue.getValue())) {
      result.put(p.getProperty(), p);
    }
  }

  private void applyCss(Box box) throws Exception {
    Set<PropertyValue> properties = getCss(box);
    for (PropertyValue p : properties) {
      CssStyle style = cssStyleFactory.getCssStyle(p);
      if (style != null) {
        style.style(box);
      }
    }
  }

  private void setupCss(Box box, String strStyle) throws Exception {
    Rule style = CSSParser.parseStyle(strStyle);
    if (style.getPropertyValues().size() > 0) {
      box.addRule(style);
    }
    for (Rule rule : cssrules) {
      for (Selector selector : rule.getSelectors()) {
        if (match(selector, box)) {
          logger.debug(String.format("box %s matches selector %s",
              box.getTag(), selector.toString()));
          box.addRule(rule);
          break;
        }
      }
    }
  }

  private boolean match(Selector selector, Box box) {
    String s = selector.toString();
    if (s.startsWith(".")) {
      String cls = StringUtils.stripStart(s, ".");
      return box.hasCssClass(cls);
    } else {
      return StringUtils.equals(box.getTag(), s);
    }
  }

  private void parseCss(Element el) {
    String cssSource = el.getTextContent();
    try {
      cssrules.addAll(parseCss(cssSource));
    } catch (Exception e) {
      logger.warn("trouble parsing css source " + cssSource, e);
    }
  }

  private Box createTextBox(Node textNode, Box parent) throws Exception {
    String text = textNode.getTextContent();
    TextBox tbox = new TextBox(text, fontManager);
    tbox.setParent(parent);
    applyCss(tbox);
    return tbox;
  }

  private void importCss(Element elLink) throws Exception {
    Map<String, String> attr = XmlUtils.getAttributes(elLink);
    String href = attr.get("href");
    if (!StringUtils.isBlank(href) && (resolver != null)) {
      InputStream in = resolver.resolve(href);
      String css = IOUtils.toString(in);
      cssrules.addAll(parseCss(css));
    }
  }

  private List<Rule> parseCss(String source) throws Exception {
    List<Rule> rules = CSSParser.parse(source);
    for (Rule rule : rules) {
      for (Selector selector : rule.getSelectors()) {
        if (StringUtils.equals("@font-face", selector.toString())) {
          String family = rule.getPropertyValue("font-family");
          String src = rule.getPropertyValue("src");
          fontManager.registerFont(family, src);
        }
      }
    }
    return rules;
  }
}
