package boxrenderer;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

import com.google.common.collect.Lists;

public class ContentBoxImpl extends AbstractBox implements ContentBox {

    private static class LayoutInfo {
        Rectangle position;
        Box box;

        public LayoutInfo(Rectangle position, Box box) {
            this.position = position;
            this.box = box;
        }
    }

    private List<Box> content = Lists.newArrayList();

    private Align align = Align.LEFT;

    @Override
    public Dimension getContentDimension(Graphics2D g2) throws Exception {
        return getSize(layout(g2));
    }

    @Override
    public void renderContent(Graphics2D g2) throws Exception {
        List<LayoutInfo> layout = layout(g2);
        for(LayoutInfo li : layout) {
            Rectangle pos = li.position;
            Graphics2D g0 = (Graphics2D)g2.create(pos.x, pos.y, pos.width, pos.height);
            try {
                li.box.render(g0);
            } finally {
                g0.dispose();
            }
        }
    }

    public void addContent(Box content) {
        this.content.add(content);
        
    }

    private List<LayoutInfo> layout(Graphics2D g2) throws Exception {
        List<LayoutInfo> layout = Lists.newArrayList();
        int x = 0;
        int y = 0;
        int lineHeight = 0;
        int maxWidth = 0;
        for(Box box : content) {
            if(!box.isInline()) {
                Dimension d = box.getDimension(g2);
                maxWidth = Math.max(maxWidth, d.width);
            }
        }
        for(Box box : content) {
            Dimension d = box.getDimension(g2);
            if(box.isInline()) {
                Rectangle pos = new Rectangle(x,y,d.width,d.height);
                lineHeight = Math.max(lineHeight, d.height);
                x+=d.width;
                layout.add(new LayoutInfo(pos, box));
            } else {
                if(align == Align.LEFT) {
                    x=0;
                } else if(align == Align.CENTER) {
                    x=maxWidth / 2 - d.width / 2;
                } else if(align == Align.RIGHT) {
                    throw new RuntimeException("Align right not implemented");
                }
                y+=lineHeight;
                lineHeight = 0;
                Rectangle pos = new Rectangle(x,y,d.width,d.height);
                y+=d.height;
                layout.add(new LayoutInfo(pos, box));
            }
        }
        return layout;
    }

    private Dimension getSize(List<LayoutInfo> layout) {
        Dimension result = new Dimension();
        for(LayoutInfo li : layout) {
            result.width = Math.max(result.width, li.position.x+li.position.width);
            result.height= Math.max(result.height, li.position.y+li.position.height);
        }
        return result;
    }

    @Override
    public void setAlign(Align align) {
        this.align = align;
    }
}
