package boxrenderer.xhtml;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import boxrenderer.Box;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


public class FontSizeCssStyle extends AbstractCssStyle implements CssStyle {

    private static class FontSize {
        private int pt;
        private String px;
        private String em;
        private String percent;

        public FontSize(int pt, String px, String em, String percent) {
            super();
            this.pt = pt;
            this.px = px;
            this.em = em;
            this.percent = percent;
        }

        public int getPt() {
            return pt;
        }

        public String getPx() {
            return px;
        }

        public String getEm() {
            return em;
        }

        public String getPercent() {
            return percent;
        }
    }

    private static List<FontSize> sizeList = Lists.newArrayList();

    static {
        sizeList.add(new FontSize(6,"8px","0.5em","50%"));
        sizeList.add(new FontSize(7,"9px","0.55em","55%"));
        sizeList.add(new FontSize(8,"10px","0.625em","62.5%"));
        sizeList.add(new FontSize(8,"11px","0.7em","70%"));
        sizeList.add(new FontSize(9,"12px","0.75em","75%"));
        sizeList.add(new FontSize(10,"13px","0.8em","80%"));
        sizeList.add(new FontSize(11,"14px","0.875em","87.5%"));
        sizeList.add(new FontSize(11,"15px","0.95em","95%"));
        sizeList.add(new FontSize(12,"16px","1em","100%"));
        sizeList.add(new FontSize(13,"17px","1.05em","105%"));
        sizeList.add(new FontSize(14,"18px","1.125em","112.5%"));
        sizeList.add(new FontSize(14,"19px","1.2em","120%"));
        sizeList.add(new FontSize(15,"20px","1.25em","125%"));
        sizeList.add(new FontSize(15,"21px","1.3em","130%"));
        sizeList.add(new FontSize(16,"22px","1.4em","140%"));
        sizeList.add(new FontSize(17,"23px","1.45em","145%"));
        sizeList.add(new FontSize(18,"24px","1.5em","150%"));
        sizeList.add(new FontSize(20,"26px","1.6em","160%"));
        sizeList.add(new FontSize(22,"29px","1.8em","180%"));
        sizeList.add(new FontSize(24,"32px","2em","200%"));
        sizeList.add(new FontSize(26,"35px","2.2em","220%"));
        sizeList.add(new FontSize(27,"36px","2.25em","225%"));
        sizeList.add(new FontSize(28,"37px","2.3em","230%"));
        sizeList.add(new FontSize(29,"38px","2.35em","235%"));
        sizeList.add(new FontSize(30,"40px","2.45em","245%"));
        sizeList.add(new FontSize(32,"42px","2.55em","255%"));
        sizeList.add(new FontSize(34,"45px","2.75em","275%"));
        sizeList.add(new FontSize(36,"48px","3em","300%"));
    }

    private static Map<String, Integer> sizeConstants = Maps.newHashMap();
    
    static {
        sizeConstants.put("large", 14);
        sizeConstants.put("larger", 14);
        sizeConstants.put("medium", 12);
        sizeConstants.put("small", 10);
        sizeConstants.put("smaller", 10);
        sizeConstants.put("x-large", 18);
        sizeConstants.put("x-small", 8);
        sizeConstants.put("xx-large", 24);
        sizeConstants.put("xx-small", 7);
    }

    @Override
    public void style(Box box) {
        String strSize = getProperty().getValue();
        int size;
        if(strSize.endsWith("pt")) {
            size = Integer.parseInt(StringUtils.removeEnd(strSize, "pt"));
        } else {
            size = getSize(strSize);
        }
        box.setFontSize(size);
    }

    private int getSize(String strSize) {
        Integer i = sizeConstants.get(StringUtils.lowerCase(strSize));
        if(i!=null) {
            return i;
        } else {
            for(FontSize fs : sizeList) {
                if(fs.getEm().equals(strSize) || fs.getPx().equals(strSize) ||
                        fs.getPercent().equals(strSize)) {
                    return fs.getPt();
                }
            }
            throw new IllegalArgumentException(String.format(
                    "can't convert %s into point size", strSize));
        }
    }

}

