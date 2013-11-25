package html;

import org.apache.commons.lang3.StringUtils;

public class XToHtml {

    public String toHtml(String content, String mimetype) {
        if(StringUtils.containsIgnoreCase(mimetype, "markdown")) {
            return new MarkdownToHtml().toHtml(content);
        } else if(StringUtils.containsIgnoreCase(mimetype, "text/html")) {
            return content;
        }
        throw new RuntimeException("unknown mime type "+mimetype);
    }

}
