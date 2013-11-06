package charts.graphics;

import java.awt.Color;

import org.apache.commons.lang3.StringUtils;

public class Colors {

    public static final Color PURPLE = fromHex("#D6117E");
    public static final Color LIGHT_BROWN = fromHex("#BAAF93");
    public static final Color VIOLET = fromHex("#2B3077");
    public static final Color PINK = fromHex("#802A7D");
    public static final Color YELLOW = fromHex("#FDF117");
    public static final Color BROWN = fromHex("#907F71");
    public static final Color ORANGE = fromHex("#EB8C28");
    public static final Color LIGHT_BLUE = fromHex("#00A4DC");
    public static final Color GREEN = fromHex("#009A4E");
    public static final Color RED = fromHex("#D92129");
    public static final Color GRAY = fromHex("#898C8B");
    public static final Color BLUE = fromHex("#206FAD");
    public static final Color DARK_RED = fromHex("#853806");
    public static final Color LIGHT_RED = fromHex("#FF7C7C");

    public static Color fromHex(String s) {
        String c = StringUtils.strip(StringUtils.strip(s), "#");
        if(c.length()>=6) {
            int r = Integer.parseInt(StringUtils.substring(c, 0, 2), 16);
            int g = Integer.parseInt(StringUtils.substring(c, 2, 4), 16);
            int b = Integer.parseInt(StringUtils.substring(c, 4, 6), 16);
            int a = 255;
            if(c.length()>=8) {
                a = Integer.parseInt(StringUtils.substring(c, 6, 8), 16);
            }
            return new Color(r,g,b,a);
        } else {
            throw new IllegalArgumentException("unknown color code "+s);
        }
    }

}
