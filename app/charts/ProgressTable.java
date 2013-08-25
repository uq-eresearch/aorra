package charts;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;

import boxrenderer.Box;
import boxrenderer.Resolver;
import boxrenderer.xhtml.Parser;

public class ProgressTable implements Drawable {

    private Box box;

    public ProgressTable() {
        try {
            Resolver resolver = new Resolver() {
                @Override
                public InputStream resolve(String source) throws Exception {
                    if(StringUtils.startsWith(source, "url('")) {
                        source = StringUtils.substringBetween(source, "('", "')");
                    }
                    InputStream stream = ProgressTable.class.getResourceAsStream(source);
                    if(stream == null) {
                        throw new Exception("failed to load resource " + source);
                    }
                    return stream;
                }};
            Parser parser = new Parser(resolver);
            box = parser.parse(resolver.resolve("progresstable.html"));
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Dimension getDimension(Graphics2D graphics) {
        try {
            return box.getDimension(graphics);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void draw(Graphics2D g2) {
        try {
            Dimension d = box.getDimension(g2);
            g2.setClip(0, 0, d.width, d.height);
            box.render(g2);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }

    }

}
