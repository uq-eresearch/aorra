@(
  baseYear: String,
  marineData: Option[play.api.libs.json.JsValue]
)

@import play.api.libs.json.Json
@import play.api.libs.json.JsString
@import play.api.templates.JavaScript

var baseYear = @JavaScript(Json.prettyPrint(JsString(baseYear)));
var reportYears = "2011";

var regionNames = {
  'cape-york': 'Cape York',
  'wet-tropics': 'Wet Tropics',
  'burdekin': 'Burdekin',
  'mackay-whitsunday': 'Mackay-Whitsunday',
  'fitzroy': 'Fitzroy',
  'burnett-mary': 'Burnett-Mary'
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

var marineCaptions = {
  'gbr':                '',
  'cape-york':          '',
  'wet-tropics':        '',
  'burdekin':           '',
  'mackay-whitsunday':  '',
  'fitzroy':            '',
  'burnett-mary':       ''
};

var marineData = @JavaScript(Json.prettyPrint(marineData.getOrElse(Json.obj())));

var managementData = {};

var catchmentData = {};
