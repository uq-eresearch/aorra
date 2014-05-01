@(
  baseYear: play.api.libs.json.JsString,
  reportYears: play.api.libs.json.JsString,
  marineData: play.api.libs.json.JsValue,
  managementData: play.api.libs.json.JsValue,
  catchmentData: play.api.libs.json.JsValue,
  marineCaptions: play.api.libs.json.JsValue
)

@import play.api.libs.json.Json
@import play.api.templates.JavaScript

var baseYear = @JavaScript(Json.prettyPrint(baseYear));
var reportYears = @JavaScript(Json.prettyPrint(reportYears));

var regionNames = {
  'cape-york': 'Cape York',
  'wet-tropics': 'Wet Tropics',
  'burdekin': 'Burdekin',
  'mackay-whitsunday': 'Mackay Whitsunday',
  'fitzroy': 'Fitzroy',
  'burnett-mary': 'Burnett Mary'
};

var indicatorNames = {
  // Management
  "grazing": "Grazing",
  "sugarcane": "Sugarcane / Grains",
  "horticulture": "Horticulture",
  // Catchment
  "groundcover": "Groundcover",
  "nitrogen": "Nitrogen",
  "sediment": "Sediment",
  "pesticides": "Pesticides",
  // Marine
  "overall": "Overall Marine Condition",
  "coral": "Coral",
  "coral-change": "Coral change",
  "coral-cover": "Coral cover",
  "coral-juvenile": "Coral juvenile density",
  "coral-macroalgae": "Coral macroalgal cover",
  "seagrass": "Seagrass",
  "seagrass-abundance": "Seagrass abundance",
  "seagrass-reproduction": "Seagrass reproduction",
  "seagrass-nutrient": "Seagrass nutrients",
  "water": "Water Quality",
  "water-solids": "Total suspended solids",
  "water-chlorophyll": "Chlorophyll &alpha;"
};

var marineCaptions = @JavaScript(Json.prettyPrint(marineCaptions));

var marineData = @JavaScript(Json.prettyPrint(marineData));

var managementData = @JavaScript(Json.prettyPrint(managementData));

var catchmentData = @JavaScript(Json.prettyPrint(catchmentData));
