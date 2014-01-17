package html

import org.apache.commons.lang3.StringUtils.containsIgnoreCase

class XToHtml {

  def toHtml(content: String, mimetype: String) = content match {
    case _ if containsIgnoreCase(mimetype, "html") =>
      wrapHtml(content)
    case _ => null
  }

  private def wrapHtml(content: String) =
    s"""
    <html>
      <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
      </head>
      <body>
        ${content}
      </body>
    </html>
    """

}
