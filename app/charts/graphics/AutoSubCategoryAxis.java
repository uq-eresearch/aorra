package charts.graphics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.AxisState;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.text.TextBlock;
import org.jfree.text.TextBlockAnchor;
import org.jfree.text.TextLine;
import org.jfree.ui.RectangleEdge;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class AutoSubCategoryAxis extends CategoryAxis {

    public static final String DEFAULT_SEPARATOR = "_||_";

    private static final Double DEFAULT_CATEGORY_MARGIN = 0.0;

    public static enum Border {
        NONE, BETWEEN, ALL
    }

    public static class CategoryLabelConfig {
        final private CategoryLabelPositions position;
        final private Font font;
        final private Paint fontPaint;
        final private double marginTop;
        final private double marginBottom;
        final private Border border;
        final private Paint borderPaint; 
        final private Stroke borderStroke;
        final private Map<String, String> abbrDict;
        final private boolean wrap;

        public CategoryLabelConfig(CategoryLabelPositions position, Font font, Paint fontPaint,
                double marginTop, double marginBottom, Border border, Paint borderPaint,
                Stroke borderStroke, Map<String, String> abbrDict, boolean wrap) {
            super();
            this.position = position;
            this.font = font;
            this.fontPaint = fontPaint;
            this.marginTop = marginTop;
            this.marginBottom = marginBottom;
            this.border = border;
            this.borderPaint = borderPaint;
            this.borderStroke = borderStroke;
            this.abbrDict = abbrDict;
            this.wrap = wrap;
        }

        public CategoryLabelConfig(CategoryLabelPositions position, Font font, Paint fontPaint,
                double marginTop, double marginBottom) {
            this(position, font, fontPaint, marginTop, marginBottom,
                    Border.NONE, null, null, null, false);
        }

        public CategoryLabelConfig(Font font, double marginTop, double marginBottom) {
            this(CategoryLabelPositions.STANDARD, font, Color.black, marginTop, marginBottom,
                    Border.NONE, null, null, null, false);
        }

        public CategoryLabelConfig(Font font, double marginTop, double marginBottom,
                Border border, Paint borderPaint) {
            this(CategoryLabelPositions.STANDARD, font, Color.black, marginTop, marginBottom,
                    border, borderPaint, new BasicStroke(1), null, false);
        }

        public CategoryLabelConfig(Font font, double marginTop, double marginBottom,
                Border border, Paint borderPaint, Map<String, String> abbrDict, boolean wrap) {
            this(CategoryLabelPositions.STANDARD, font, Color.black, marginTop, marginBottom,
                    border, borderPaint, new BasicStroke(1), abbrDict, wrap);
        }

        public CategoryLabelPositions getPosition() {
            return position;
        }

        public Font getFont() {
            return font;
        }

        public double getMarginTop() {
            return marginTop;
        }

        public double getMarginBottom() {
            return marginBottom;
        }

        public Paint getFontPaint() {
            return fontPaint;
        }

        public Border getBorder() {
            return border;
        }

        public Paint getBorderPaint() {
            return borderPaint;
        }

        public Stroke getBorderStroke() {
            return borderStroke;
        }

        public boolean wrap() {
        	return wrap;
        }

    }

    private class SubCategory {
        private String name;
        private SubCategory parent;
        private List<SubCategory> subcategories = Lists.newArrayList();

        public SubCategory() {}
        
        public SubCategory(String name, SubCategory parent) {
            this.name = name;
            this.parent = parent;
        }

        public void addSubCategory(String path) {
            String[] split = StringUtils.split(path, AutoSubCategoryAxis.this.separator);
            String name = split[0];
            SubCategory sub = getSubCategory(name);
            if(sub == null) {
                sub = new SubCategory(name, this);
                subcategories.add(sub);
            }
            if(split.length > 1) {
                sub.addSubCategory(StringUtils.join(split, AutoSubCategoryAxis.this.separator, 1, split.length));
            }
        }

        public SubCategory getSubCategory(String name) {
            for(SubCategory sub : subcategories) {
                if(sub.name.equals(name)) {
                    return sub;
                }
            }
            return null;
        }

        public String getName() {
            return this.name;
        }

        public List<SubCategory> getLeafs() {
            return getLeafs(new ArrayList<SubCategory>());
        }

        private List<SubCategory> getLeafs(List<SubCategory> leafs) {
            if(subcategories.isEmpty()) {
                leafs.add(this);
            } else {
                for(SubCategory c : subcategories) {
                    c.getLeafs(leafs);
                }
            }
            return leafs;
        }

        public int getDepth() {
            return getDepth(0);
        }

        private int getDepth(int d) {
            if(parent == null) {
                return d;
            } else {
                return parent.getDepth(d+1);
            }
        }

        public boolean isSibling(SubCategory other) {
            if(parent == null) {
                return false;
            }
            for(SubCategory s : parent.subcategories) {
                if(s == other) {
                    return true;
                }
            }
            return false;
        }

        public int findSiblingDepth(SubCategory other) {
            if(isSibling(other)) {
                return getDepth();
            } else if(parent == null) {
                throw new RuntimeException("root");
            } else {
                return parent.findSiblingDepth(other.parent);
            }
        }

        public int depth() {
            return depth(0);
        }

        private int depth(int depth) {
            if(subcategories.isEmpty()) {
                return depth;
            } else {
                int d = depth;
                for(SubCategory c : subcategories) {
                    d = Math.max(d, c.depth(depth+1));
                }
                return d;
            }
        }

        public List<SubCategory> getAllOfDepth(int depth) {
            return getAllOfDepth(new ArrayList<SubCategory>(), depth, 0);
        }

        private List<SubCategory> getAllOfDepth(List<SubCategory> l, int depth, int current) {
            if(depth == current) {
                l.add(this);
            } else {
                for(SubCategory c : subcategories) {
                    c.getAllOfDepth(l, depth, current+1);
                }
            }
            return l;
        }

        public String getPath() {
            List<String> path = Lists.newArrayList();
            SubCategory current = this;
            while(current.name != null) {
                path.add(current.name);
                current = current.parent;
            }
            return StringUtils.join(Lists.reverse(path), AutoSubCategoryAxis.this.separator);
        }

        public String toString(String indent) {
            String s = indent + (name!=null?name:"root") + "\n";
            for(SubCategory sub : subcategories) {
                s += sub.toString(indent+"  ");
            }
            return s;
        }

        @Override 
        public String toString() {
            return toString("");
        }
    }

    private CategoryDataset dataset;

    private SubCategory root = new SubCategory();

    private Map<Integer, Double> categoryMargins = Maps.newHashMap();

    private Map<Integer, CategoryLabelConfig> labelConfigs = Maps.newHashMap();

    private String separator = DEFAULT_SEPARATOR;

    public AutoSubCategoryAxis(CategoryDataset dataset) {
        this.dataset = dataset;
        for(int col=0;col<dataset.getColumnCount();col++) {
            root.addSubCategory(dataset.getColumnKey(col).toString());
        }
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public void setItemMargin(int depth, double margin) {
        categoryMargins.put(depth, margin);
    }

    public double getItemMargin(int depth) {
        Double m = categoryMargins.get(depth);
        if(m == null) {
            return DEFAULT_CATEGORY_MARGIN;
        } else {
            return m;
        }
    }

    public void setCategoryLabelConfig(int depth, CategoryLabelConfig config) {
        labelConfigs.put(depth, config);
    }

    public CategoryLabelConfig getCategoryLabelConfig(int depth) {
        CategoryLabelConfig config = labelConfigs.get(depth);
        if(config == null) {
            config = new CategoryLabelConfig(new Font(Font.DIALOG, Font.PLAIN, 10), 0,0);
        }
        return config;
    }

    @Override
    public double getCategoryMargin() {
        return 0.0;
    }

    /**
     * Returns the starting coordinate for the specified category.
     *
     * @param category  the category.
     * @param categoryCount  the number of categories.
     * @param area  the data area.
     * @param edge  the axis location.
     *
     * @return The coordinate.
     *
     * @see #getCategoryMiddle(int, int, Rectangle2D, RectangleEdge)
     * @see #getCategoryEnd(int, int, Rectangle2D, RectangleEdge)
     */
    @Override
    public double getCategoryStart(int category, int categoryCount,
                                   Rectangle2D area,
                                   RectangleEdge edge) {
        double result = 0.0;
        if ((edge == RectangleEdge.TOP) || (edge == RectangleEdge.BOTTOM)) {
            result = area.getX() + area.getWidth() * getLowerMargin();
        }
        else if ((edge == RectangleEdge.LEFT)
                || (edge == RectangleEdge.RIGHT)) {
            result = area.getMinY() + area.getHeight() * getLowerMargin();
        }
        double categorySize = calculateCategorySize(categoryCount, area, edge);
        double margin = 0.0;
        for(int i = 0; i < category;i++) {
            margin += categorySize * getMarginAsPercentage(i);
        }
        return result += category * categorySize + margin;
    }

    public double calculateCategorySize(Rectangle2D area) {
        return calculateCategorySize(dataset.getColumnCount(), area, RectangleEdge.BOTTOM);
    }

    @Override
    protected double calculateCategorySize(int categoryCount, Rectangle2D area,
            RectangleEdge edge) {
        double result = 0.0;
        double available = 0.0;
        if ((edge == RectangleEdge.TOP) || (edge == RectangleEdge.BOTTOM)) {
            available = area.getWidth();
        }
        else if ((edge == RectangleEdge.LEFT)
                || (edge == RectangleEdge.RIGHT)) {
            available = area.getHeight();
        }
        List<SubCategory> leafs = root.getLeafs();
        if(leafs.size() != categoryCount) {
            throw new RuntimeException(String.format("leaf size %s does not match category count %s",
                    leafs.size(), categoryCount));
        }
        double margin = 0.0; 
        for(int i = 0;i<leafs.size()-1;i++) {
            margin += getMarginAsPercentage(i);
        }
        
        result = available * (1 - getLowerMargin() - getUpperMargin()) / (leafs.size() + margin);
        return result;
    }

    /**
     * Returns the Margin as a percentage of the category size between category1 and category1+1
     */
    private double getMarginAsPercentage(int category1) {
        List<SubCategory> leafs = root.getLeafs();
        SubCategory c1 = leafs.get(category1);
        SubCategory c2 = leafs.get(category1+1);
        return getItemMargin(c1.findSiblingDepth(c2)-1);
    }

    @Override
    protected AxisState drawCategoryLabels(Graphics2D g2, Rectangle2D plotArea,
            Rectangle2D dataArea, RectangleEdge edge, AxisState state,
            PlotRenderingInfo plotState) {
        return drawCategoryLabels(g2, plotArea, dataArea, edge, state, plotState, true);
    }

    private String[] labelRows(String name, CategoryLabelConfig config) {
//    TODO make number of rows configurable
      return config.wrap?StringUtils.split(name, " ", 2):new String[] {name};
    }

    private TextBlock makeLabelBlock(String[] rows, CategoryLabelConfig config) {
      Font f = config.getFont();
      Paint p = config.getFontPaint();
      TextBlock block = new TextBlock();
      for(String s : rows) {
        if(StringUtils.isNotBlank(s)) {
          TextLine line = new TextLine(s, f, p);
          block.addLine(line);
        }
      }
      return block;
    }

    private String[] shortenLabel(String[] rows, CategoryLabelConfig config) {
      if(config.abbrDict == null) {
        return rows;
      }
      String[] result = new String[rows.length];
      Map<String, String> abbrDict = config.abbrDict;
      for(int i=0;i<rows.length;i++) {
        if(StringUtils.isNotBlank(rows[i])) {
          result[i] = rows[i];
          for(Map.Entry<String, String> me : abbrDict.entrySet()) {
            result[i] = StringUtils.replace(result[i], me.getKey(), me.getValue()); 
          }
        }
      }
      return result;
    }

    private String[] shortestLables(String[] rows) {
      String[] result = new String[rows.length];
      for(int i=0;i<rows.length;i++) {
        if(StringUtils.isNotBlank(rows[i])) {
          result[i] = rows[i].substring(0, 1);
        }
      }
      return result;
    }

    private List<TextBlock> labelAlternatives(String name, CategoryLabelConfig config) {
      if(config.wrap) {
        String[] rows = labelRows(name, config);
        TextBlock b0 = makeLabelBlock(rows, config);
        TextBlock b1 = makeLabelBlock(shortenLabel(rows, config), config);
        TextBlock b2 = makeLabelBlock(shortestLables(rows), config);
        return Lists.newArrayList(b0, b1, b2);
      } else {
        return Collections.singletonList(makeLabelBlock(labelRows(name, config), config));
      }
    }

    private AxisState drawCategoryLabels(Graphics2D g2, Rectangle2D plotArea,
            Rectangle2D dataArea, RectangleEdge edge, AxisState state,
            PlotRenderingInfo plotState, boolean render) {
        if(!isTickLabelsVisible()) {
            return state;
        }
        double y0 = state.getCursor();
        for(int depth = root.depth();depth>0;depth--) {
            List<SubCategory> l = root.getAllOfDepth(depth);
            CategoryLabelConfig config = getCategoryLabelConfig(depth-1);
            double yb0 = state.getCursor();
            state.cursorDown(config.getMarginTop());
            float maxHeight = 0.0f;
            for(SubCategory c : l) {
                double angle = 0;
                if(config.position.equals(CategoryLabelPositions.UP_90)) {
                    angle = -Math.PI/2;
                }
                int categoryStartIndex = getCategoryStartIndex(c);
                int categoryEndIndex = getCategoryEndIndex(c);
                double start = getCategoryStart(categoryStartIndex, dataset.getColumnCount(), dataArea, edge);
                double end = getCategoryEnd(categoryEndIndex, dataset.getColumnCount(), dataArea, edge);
                double middle = start + (end-start)/2;
                double maxWidth = end-start;
                g2.setFont(config.getFont());
                g2.setPaint(config.getFontPaint());
                List<TextBlock> blocks = labelAlternatives(c.getName(), config);
                Iterator<TextBlock> iter = blocks.iterator();
                while(iter.hasNext()) {
                  TextBlock block = iter.next();
                  Shape b = block.calculateBounds(g2, (float)middle, (float)state.getCursor(),
                      TextBlockAnchor.CENTER, (float)middle, (float)state.getCursor(), angle);
                  Rectangle2D bounds = b.getBounds2D();
                  if(bounds.getWidth() > maxWidth && config.wrap() && iter.hasNext()) {
                    continue;
                  }
                  float height = (float)(bounds.getMaxY() - bounds.getMinY());
                  maxHeight = Math.max(height, maxHeight);
                  if(render) {
                    block.draw(g2, (float)middle, (float)state.getCursor()+height/2,
                        TextBlockAnchor.CENTER, (float)middle,
                        (float)state.getCursor()+height/2, angle);
                  }
                  break;
                }
            }
            state.cursorDown(maxHeight);
            state.cursorDown(config.getMarginBottom());
            if(render) {
                for(SubCategory c : l) {
                    drawBorder(g2, y0, yb0, state.getCursor(), config, dataArea, c);
                }
            }
        }
        return state;
    }

    private void drawBorder(Graphics2D g2, double y0, double yb0, double y1,
            CategoryLabelConfig config, Rectangle2D area, SubCategory c) {
        if(config.getBorder() == Border.NONE) {
            return;
        }
        int start = getCategoryStartIndex(c);
        int end = getCategoryEndIndex(c);
        g2.setStroke(config.getBorderStroke());
        g2.setPaint(config.getBorderPaint());
        if(config.getBorder() == Border.BETWEEN) {
            if(end+1 >= dataset.getColumnCount()) {
                return;
            }
            double m1 = getCategoryMiddle(end, dataset.getColumnCount(), area, RectangleEdge.BOTTOM);
            double m2 = getCategoryMiddle(end+1, dataset.getColumnCount(), area, RectangleEdge.BOTTOM);
            double x = (m1+m2)/2;
            line(g2,x,y0,x,y1);
        } else if(config.getBorder() == Border.ALL) {
            double x0;
            double x1;
            if(start == end) {
                x0 = getCategoryStart(start, dataset.getColumnCount(), area, RectangleEdge.BOTTOM);
                x1 = getCategoryEnd(start, dataset.getColumnCount(), area, RectangleEdge.BOTTOM);
            } else {
                x0 = getMiddleX(area, start-1, start);
                x1 = getMiddleX(area, end, end+1);
            }
            line(g2, x0, yb0, x1, yb0);
            line(g2, x0, y1, x1, y1);
            line(g2, x0, y0, x0, y1);
            line(g2, x1, y0, x1, y1);
        }
    }

    private void line(Graphics2D g2, double x1, double y1, double x2, double y2) {
        g2.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    private double getMiddleX(Rectangle2D area, int index1, int index2) {
        double x;
        if(index1 <= 0) {
            x = area.getMinX();
        } else if(index2 >= dataset.getColumnCount()){
            x = area.getMaxX();
        } else {
            double m1 = getCategoryMiddle(index1, dataset.getColumnCount(), area, RectangleEdge.BOTTOM);
            double m2 = getCategoryMiddle(index2, dataset.getColumnCount(), area, RectangleEdge.BOTTOM);
            x = (m1+m2)/2;
        }
        return x;
    }

    private int getCategoryStartIndex(SubCategory c) {
        List<SubCategory> leafs = root.getLeafs();
        SubCategory l1 = c.getLeafs().get(0);
        return leafs.indexOf(l1);
    }

    private int getCategoryEndIndex(SubCategory c) {
        List<SubCategory> leafs = root.getLeafs();
        List<SubCategory> tmp = c.getLeafs();
        SubCategory lx = tmp.get(tmp.size()-1);
        return leafs.indexOf(lx);
    }

    @Override
    public AxisSpace reserveSpace(Graphics2D g2, Plot plot,
            Rectangle2D plotArea, RectangleEdge edge, AxisSpace space) {
        Rectangle2D labelEnclosure = getLabelEnclosure(g2, edge);
        space.add(labelEnclosure.getHeight() + this.getCategoryLabelPositionOffset(), edge);
        AxisState axisState = drawCategoryLabels(g2, plotArea,
            space.shrink(plotArea, null), edge, new AxisState(), null, false);
        space.add(axisState.getCursor(), RectangleEdge.BOTTOM);
        return space;
    }

    // rearrange dataset so that the order of leafs
    // matches the order of columns
    public CategoryDataset getFixedDataset() {
        DefaultCategoryDataset fixed = new DefaultCategoryDataset();
        List<SubCategory> leafs = root.getLeafs();
        for(SubCategory leaf : leafs) {
            String path = leaf.getPath();
            int col = dataset.getColumnIndex(path);
            if(col == -1) {
                throw new RuntimeException(String.format("column '%s' not found in dataset", path));
            }
            for(int row=0;row<dataset.getRowCount();row++) {
                Comparable<?> rc = dataset.getRowKey(row);
                if(rc != null) {
                    fixed.addValue(dataset.getValue(row, col), rc, path);
                }
            }
        }
        return fixed;
    }

}
