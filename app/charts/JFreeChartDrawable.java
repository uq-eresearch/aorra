package charts;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.JFreeChart;

public class JFreeChartDrawable implements Drawable {

    private JFreeChart jfreechart;

    private Dimension dimension;

    public JFreeChartDrawable(JFreeChart jfreechart, Dimension dimension) {
        this.jfreechart = jfreechart;
        this.dimension = dimension;
    }

    @Override
    public Dimension getDimension(Graphics2D graphics) {
        return dimension;
    }

    @Override
    public void draw(Graphics2D graphics) {
        jfreechart.draw(graphics,
                new Rectangle2D.Double(0,0, dimension.getWidth(), dimension.getHeight()));
    }

}
