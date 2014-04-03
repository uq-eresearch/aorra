@(
  baseYear: String,
  marineData: Option[play.api.libs.json.JsValue],
  progressData: Option[play.api.libs.json.JsValue]
)

@import play.api.libs.json.Json
@import play.api.libs.json.JsString
@import play.api.templates.JavaScript

var baseYear = @JavaScript(Json.prettyPrint(JsString(baseYear)));

var marineData = @JavaScript(Json.prettyPrint(marineData.getOrElse(Json.obj())));

var progressData = @JavaScript(Json.prettyPrint(progressData.getOrElse(Json.obj())));