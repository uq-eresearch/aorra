package boxrenderer;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class TableCellBox extends AbstractBox implements ContentBox {

    public static enum VAlign {
        TOP, BOTTOM, CENTER;
    }

    private ContentBox content = new ContentBoxImpl();

    private int colspan = 1;

    private int rowspan = 1;

    private Align align = Align.LEFT;

    private VAlign valign = VAlign.CENTER;

    public TableCellBox() {
    }

    public TableCellBox(Box content) {
        addContent(content);
    }

    public TableCellBox(Box content, int colspan, int rowspan) {
        this(content);
        setColspan(colspan);
        setRowspan(rowspan);
    }

    public TableCellBox(int colspan, int rowspan) {
        this.colspan = colspan;
        this.rowspan = rowspan;
    }

    @Override
    public Dimension getContentDimension(Graphics2D g2) throws Exception {
        return content.getDimension(g2);
    }

    @Override
    public void renderContent(Graphics2D g2) throws Exception {
        int x = 0;
        int y = 0;
        Rectangle bounds = g2.getClipBounds();
        Dimension d = content.getDimension(g2);
        if(bounds.width > d.width) {
            if(align == Align.CENTER) {
                x = bounds.width / 2 - d.width / 2;
            } else if(align == Align.RIGHT) {
                x = bounds.width - d.width;
            }
        }
        if(bounds.height > d.height) {
            if(valign == VAlign.CENTER) {
                y = bounds.height / 2 - d.height / 2;
            } else if(valign == VAlign.BOTTOM) {
                y = bounds.height - d.height;
            }
        }
        Graphics2D g0 = (Graphics2D)g2.create(x, y,d.width, d.height);
        content.render(g0);
        g0.dispose();
    }

    public int getColspan() {
        return colspan;
    }

    public void setColspan(int colspan) {
        this.colspan = colspan;
    }

    public int getRowspan() {
        return rowspan;
    }

    public void setRowspan(int rowspan) {
        this.rowspan = rowspan;
    }

    public Align getAlign() {
        return align;
    }

    public void setAlign(Align align) {
        if(align != null) {
            this.align = align;
            content.setAlign(align);
        }
    }

    public VAlign getValign() {
        return valign;
    }

    public void setValign(VAlign valign) {
        if(valign != null) {
            this.valign = valign;
        }
    }

    @Override
    public void addContent(Box content) {
        this.content.addContent(content);
    }

    @Override
    public void setAttributes(Map<String, String> attributes) {
        super.setAttributes(attributes);
        int colspan = 1;
        int rowspan = 1;
        try {
            colspan = Integer.parseInt(attributes.get("colspan"));
        } catch(Exception e) {}
        try {
            rowspan = Integer.parseInt(attributes.get("rowspan"));
        } catch(Exception e) {}
        setColspan(colspan);
        setRowspan(rowspan);
        if(attributes.get("align")!=null) {
            setAlign(Align.valueOf(StringUtils.upperCase(attributes.get("align"))));
        }
    }

}
