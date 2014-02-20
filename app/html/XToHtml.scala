package html

import org.apache.commons.lang3.StringUtils.containsIgnoreCase

class XToHtml {

  def toHtml(content: String, mimetype: String) = content match {
    case _ if containsIgnoreCase(mimetype, "html") =>
      wrapHtml(content)
    case _ => null
  }

  private def wrapHtml(content: String) =
    s"""<!DOCTYPE html>
    <html>
      <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
        <link rel="stylesheet" media="screen" href="/assets/stylesheets/main.min.css"/>
      </head>
      <body>
        <div id="html-tab">
          <div class="html-pane">
            ${content}
          </div>
        </div>
      </body>
    </html>
    """

}
