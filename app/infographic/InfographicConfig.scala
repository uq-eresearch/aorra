package infographic

case class InfographicConfig(
  val baseYear: String,
  val reportYears: String, // May just be one year
  val marineChartFileId: String,
  val progressTableFileId: String,
  val marineCaptionsFileId: String
)
