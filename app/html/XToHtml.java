package html;

import org.apache.commons.lang3.StringUtils;

public class XToHtml {

  public String toHtml(String content, String mimetype) {
    if (StringUtils.containsIgnoreCase(mimetype, "html")) {
      return content;
    } else {
      return null;
    }
  }

}
