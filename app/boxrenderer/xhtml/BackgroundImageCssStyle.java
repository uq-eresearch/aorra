package boxrenderer.xhtml;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.IOUtils;

import boxrenderer.Box;
import boxrenderer.ImageRenderer;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGUniverse;



public class BackgroundImageCssStyle extends AbstractCssStyle implements CssStyle {

    private static class BufferedImageImageRenderer implements ImageRenderer {

        private BufferedImage img;

        public BufferedImageImageRenderer(BufferedImage img) {
            if(img == null) {
                throw new RuntimeException("image is null");
            }
            this.img = img;
        }

        @Override
        public Dimension getDimension(Graphics2D g2) throws Exception {
            return new Dimension(img.getWidth(), img.getHeight());
        }

        @Override
        public void render(Graphics2D g2) throws Exception {
            g2.drawImage(img, null, 0, 0);
        }
    }

    @Override
    public void style(Box box) throws Exception {
        String value = getProperty().getValue();
        if(StringUtils.startsWith(value, "inline")) {
            String imgData = StringUtils.substringBetween(value, "('", "')");
            byte[] buf = hexStringToByteArray(imgData);
            final BufferedImage img = ImageIO.read(new ByteArrayInputStream(buf));
            box.setBackgroundImage(new BufferedImageImageRenderer(img));
        } else if(StringUtils.startsWith(value, "url")) {
            String source = StringUtils.substringBetween(value, "('", "')");
            if(StringUtils.endsWithIgnoreCase(source, ".svg")) {
                box.setBackgroundImage(makeSvgImageRenderer(source));
            } else {
                box.setBackgroundImage(makeImageImageRenderer(source));
            }
        } else if(StringUtils.startsWith(value, "-moz-linear-gradient")) {
            // XXX does linear gradient from left to right only
            Color from = Colors.getPaint(StringUtils.split(
                    StringUtils.substringBetween(value, "(", ")"), ',')[1]);
            Color to = Colors.getPaint(StringUtils.split(
                    StringUtils.substringBetween(value, "(", ")"), ',')[2]);
            box.setLinearGradient(from, to);
        }
    }

    //http://stackoverflow.com/questions/140131/convert-a-string-representation-of-a-hex-dump-to-a-byte-array-using-java
    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private ImageRenderer makeSvgImageRenderer(String source) {
        InputStream stream = null;
        try {
            stream = getResolver().resolve(source);
            SVGUniverse svg = new SVGUniverse();
            URI uri = svg.loadSVG(stream, source);
            final SVGDiagram diagram = svg.getDiagram(uri, false);
            return new ImageRenderer() {
                @Override
                public Dimension getDimension(Graphics2D g2) throws Exception {
                    return new Dimension((int)diagram.getWidth(), (int)diagram.getHeight());
                }

                @Override
                public void render(Graphics2D g2) throws Exception {
                    Graphics2D g0 = (Graphics2D)g2.create();
                    try {
                        // center the svg shape in the diagram
//                        final Shape shape = diagram.getRoot().getShape();
//                        final double w = diagram.getWidth();
//                        final double h = diagram.getHeight();
//                        AffineTransform t = g0.getTransform();
//                        t.concatenate(AffineTransform.getTranslateInstance(
//                                -shape.getBounds2D().getX(), -shape.getBounds2D().getY()));
//                        t.concatenate(AffineTransform.getTranslateInstance(
//                                ((w-shape.getBounds2D().getWidth())/2.0),
//                                ((h-shape.getBounds2D().getHeight())/2.0)));
//                        g0.setTransform(t);
                        diagram.render(g0);
                    } finally {
                        g0.dispose();
                    }
                }};
        } catch(Exception e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    private ImageRenderer makeImageImageRenderer(String source) {
        InputStream stream = null;
        try {
            stream = getResolver().resolve(source);
            return new BufferedImageImageRenderer(ImageIO.read(stream));
        } catch(Exception e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

}
