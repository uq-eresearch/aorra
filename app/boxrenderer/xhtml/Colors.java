package boxrenderer.xhtml;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class Colors {

    private static Map<String, String> colorNames = new HashMap<String, String>() {
        @Override
        public String put(String key, String value) {
            return super.put(key.toLowerCase(), value);
        }
    };
    static {
        colorNames.put("AliceBlue", "#F0F8FF");
        colorNames.put("AntiqueWhite", "#FAEBD7");
        colorNames.put("Aqua", "#00FFFF");
        colorNames.put("Aquamarine", "#7FFFD4");
        colorNames.put("Azure", "#F0FFFF");
        colorNames.put("Beige", "#F5F5DC");
        colorNames.put("Bisque", "#FFE4C4");
        colorNames.put("Black", "#000000");
        colorNames.put("BlanchedAlmond", "#FFEBCD");
        colorNames.put("Blue", "#0000FF");
        colorNames.put("BlueViolet", "#8A2BE2");
        colorNames.put("Brown", "#A52A2A");
        colorNames.put("BurlyWood", "#DEB887");
        colorNames.put("CadetBlue", "#5F9EA0");
        colorNames.put("Chartreuse", "#7FFF00");
        colorNames.put("Chocolate", "#D2691E");
        colorNames.put("Coral", "#FF7F50");
        colorNames.put("CornflowerBlue", "#6495ED");
        colorNames.put("Cornsilk", "#FFF8DC");
        colorNames.put("Crimson", "#DC143C");
        colorNames.put("Cyan", "#00FFFF");
        colorNames.put("DarkBlue", "#00008B");
        colorNames.put("DarkCyan", "#008B8B");
        colorNames.put("DarkGoldenRod", "#B8860B");
        colorNames.put("DarkGray", "#A9A9A9");
        colorNames.put("DarkGrey", "#A9A9A9");
        colorNames.put("DarkGreen", "#006400");
        colorNames.put("DarkKhaki", "#BDB76B");
        colorNames.put("DarkMagenta", "#8B008B");
        colorNames.put("DarkOliveGreen", "#556B2F");
        colorNames.put("Darkorange", "#FF8C00");
        colorNames.put("DarkOrchid", "#9932CC");
        colorNames.put("DarkRed", "#8B0000");
        colorNames.put("DarkSalmon", "#E9967A");
        colorNames.put("DarkSeaGreen", "#8FBC8F");
        colorNames.put("DarkSlateBlue", "#483D8B");
        colorNames.put("DarkSlateGray", "#2F4F4F");
        colorNames.put("DarkSlateGrey", "#2F4F4F");
        colorNames.put("DarkTurquoise", "#00CED1");
        colorNames.put("DarkViolet", "#9400D3");
        colorNames.put("DeepPink", "#FF1493");
        colorNames.put("DeepSkyBlue", "#00BFFF");
        colorNames.put("DimGray", "#696969");
        colorNames.put("DimGrey", "#696969");
        colorNames.put("DodgerBlue", "#1E90FF");
        colorNames.put("FireBrick", "#B22222");
        colorNames.put("FloralWhite", "#FFFAF0");
        colorNames.put("ForestGreen", "#228B22");
        colorNames.put("Fuchsia", "#FF00FF");
        colorNames.put("Gainsboro", "#DCDCDC");
        colorNames.put("GhostWhite", "#F8F8FF");
        colorNames.put("Gold", "#FFD700");
        colorNames.put("GoldenRod", "#DAA520");
        colorNames.put("Gray", "#808080");
        colorNames.put("Grey", "#808080");
        colorNames.put("Green", "#008000");
        colorNames.put("GreenYellow", "#ADFF2F");
        colorNames.put("HoneyDew", "#F0FFF0");
        colorNames.put("HotPink", "#FF69B4");
        colorNames.put("IndianRed", "#CD5C5C");
        colorNames.put("Indigo", "#4B0082");
        colorNames.put("Ivory", "#FFFFF0");
        colorNames.put("Khaki", "#F0E68C");
        colorNames.put("Lavender", "#E6E6FA");
        colorNames.put("LavenderBlush", "#FFF0F5");
        colorNames.put("LawnGreen", "#7CFC00");
        colorNames.put("LemonChiffon", "#FFFACD");
        colorNames.put("LightBlue", "#ADD8E6");
        colorNames.put("LightCoral", "#F08080");
        colorNames.put("LightCyan", "#E0FFFF");
        colorNames.put("LightGoldenRodYellow", "#FAFAD2");
        colorNames.put("LightGray", "#D3D3D3");
        colorNames.put("LightGrey", "#D3D3D3");
        colorNames.put("LightGreen", "#90EE90");
        colorNames.put("LightPink", "#FFB6C1");
        colorNames.put("LightSalmon", "#FFA07A");
        colorNames.put("LightSeaGreen", "#20B2AA");
        colorNames.put("LightSkyBlue", "#87CEFA");
        colorNames.put("LightSlateGray", "#778899");
        colorNames.put("LightSlateGrey", "#778899");
        colorNames.put("LightSteelBlue", "#B0C4DE");
        colorNames.put("LightYellow", "#FFFFE0");
        colorNames.put("Lime", "#00FF00");
        colorNames.put("LimeGreen", "#32CD32");
        colorNames.put("Linen", "#FAF0E6");
        colorNames.put("Magenta", "#FF00FF");
        colorNames.put("Maroon", "#800000");
        colorNames.put("MediumAquaMarine", "#66CDAA");
        colorNames.put("MediumBlue", "#0000CD");
        colorNames.put("MediumOrchid", "#BA55D3");
        colorNames.put("MediumPurple", "#9370D8");
        colorNames.put("MediumSeaGreen", "#3CB371");
        colorNames.put("MediumSlateBlue", "#7B68EE");
        colorNames.put("MediumSpringGreen", "#00FA9A");
        colorNames.put("MediumTurquoise", "#48D1CC");
        colorNames.put("MediumVioletRed", "#C71585");
        colorNames.put("MidnightBlue", "#191970");
        colorNames.put("MintCream", "#F5FFFA");
        colorNames.put("MistyRose", "#FFE4E1");
        colorNames.put("Moccasin", "#FFE4B5");
        colorNames.put("NavajoWhite", "#FFDEAD");
        colorNames.put("Navy", "#000080");
        colorNames.put("OldLace", "#FDF5E6");
        colorNames.put("Olive", "#808000");
        colorNames.put("OliveDrab", "#6B8E23");
        colorNames.put("Orange", "#FFA500");
        colorNames.put("OrangeRed", "#FF4500");
        colorNames.put("Orchid", "#DA70D6");
        colorNames.put("PaleGoldenRod", "#EEE8AA");
        colorNames.put("PaleGreen", "#98FB98");
        colorNames.put("PaleTurquoise", "#AFEEEE");
        colorNames.put("PaleVioletRed", "#D87093");
        colorNames.put("PapayaWhip", "#FFEFD5");
        colorNames.put("PeachPuff", "#FFDAB9");
        colorNames.put("Peru", "#CD853F");
        colorNames.put("Pink", "#FFC0CB");
        colorNames.put("Plum", "#DDA0DD");
        colorNames.put("PowderBlue", "#B0E0E6");
        colorNames.put("Purple", "#800080");
        colorNames.put("Red", "#FF0000");
        colorNames.put("RosyBrown", "#BC8F8F");
        colorNames.put("RoyalBlue", "#4169E1");
        colorNames.put("SaddleBrown", "#8B4513");
        colorNames.put("Salmon", "#FA8072");
        colorNames.put("SandyBrown", "#F4A460");
        colorNames.put("SeaGreen", "#2E8B57");
        colorNames.put("SeaShell", "#FFF5EE");
        colorNames.put("Sienna", "#A0522D");
        colorNames.put("Silver", "#C0C0C0");
        colorNames.put("SkyBlue", "#87CEEB");
        colorNames.put("SlateBlue", "#6A5ACD");
        colorNames.put("SlateGray", "#708090");
        colorNames.put("SlateGrey", "#708090");
        colorNames.put("Snow", "#FFFAFA");
        colorNames.put("SpringGreen", "#00FF7F");
        colorNames.put("SteelBlue", "#4682B4");
        colorNames.put("Tan", "#D2B48C");
        colorNames.put("Teal", "#008080");
        colorNames.put("Thistle", "#D8BFD8");
        colorNames.put("Tomato", "#FF6347");
        colorNames.put("Turquoise", "#40E0D0");
        colorNames.put("Violet", "#EE82EE");
        colorNames.put("Wheat", "#F5DEB3");
        colorNames.put("White", "#FFFFFF");
        colorNames.put("WhiteSmoke", "#F5F5F5");
        colorNames.put("Yellow", "#FFFF00");
        colorNames.put("YellowGreen", "#9ACD32");
    }

    public static Color getPaint(String definition) {
        String colorCode = colorNames.get(definition.toLowerCase());
        if(colorCode == null) {
            colorCode = definition;
        }
        if(StringUtils.startsWith(StringUtils.lowerCase(definition), "rgb")) {
            return getPaintFromDecRGB(colorCode);
        } else {
            return getPaintFromHexRGB(colorCode);
        }
    }

    private static Color getPaintFromDecRGB(String code) {
        String colors = StringUtils.substringBetween(code, "(", ")");
        int r = Integer.parseInt(StringUtils.split(colors, ",")[0]);
        int g = Integer.parseInt(StringUtils.split(colors, ",")[1]);
        int b = Integer.parseInt(StringUtils.split(colors, ",")[2]);
        return new Color(r,g,b);
    }

    private static Color getPaintFromHexRGB(String code) {
        String c = StringUtils.strip(StringUtils.strip(code), "#");
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
            throw new IllegalArgumentException("unknown color code "+code);
        }
    }

}
