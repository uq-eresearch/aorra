package boxrenderer;

import java.awt.Font;
import java.io.InputStream;
import java.util.Map;

import org.apache.poi.util.IOUtils;

import com.google.common.collect.Maps;

public class FontManager {

    private Resolver resolver;

    private Map<String, Font> fonts = Maps.newHashMap();

    public FontManager(Resolver resolver) {
        this.resolver = resolver;
    }

    public void registerFont(String family, String src) {
        try {
            InputStream stream = resolver.resolve(src);
            if(stream == null) {
                throw new Exception("failed to resolve "+src);
            }
            Font font = Font.createFont(Font.TRUETYPE_FONT, stream);
            fonts.put(family, font);
            IOUtils.closeQuietly(stream);
        } catch(Exception e) {
            throw new RuntimeException(String.format(
                    "failed to register font %s with source %s", family, src), e);
        }
    }

    public Font getFont(String family, int style, int size) {
        Font font = fonts.get(family);
        if(font != null) {
            return font.deriveFont(style, size);
        } else {
            return new Font(family, style, size);
        }
    }

}
