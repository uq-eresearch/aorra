package charts;

import org.apache.commons.lang3.StringUtils;

public enum ChartType {
    MARINE ("Marine"),
    COTS_OUTBREAK ("Crown of Thorns Outbreak"),
    ANNUAL_RAINFALL ("Annual Rainfall"),
    PROGRESS_TABLE ("Progress Table"),
    PROGRESS_TABLE_REGION ("Progress Table Region"),
    PROGRESS_TABLE_TILE ("Progress Table Tile"),
    TTT_CANE_AND_HORT ("Tracking Towards Target - Cane and Horticulture"),
    TTT_GRAZING ("Tracking Towards Target - Grazing"),
    TTT_SEDIMENT ("Tracking Towards Target - Sediment"),
    TTT_NITRO_AND_PEST ("Tracking Towards Target - Nitrogen and Pesticide"),
    GRAZING_PS ("Grazing Practice Systems"),
    HORTICULTURE_PS ("Horticulture Practice Systems"),
    SUGARCANE_PS ("Sugarcane Practice Systems"),
    GRAINS_PS("Grains Practice Systems"),
    TSA("Trends in seagrass abundance"),
    GROUNDCOVER("Groundcover"),
    GROUNDCOVER_BELOW_50("Groundcover below 50%"),
    LOADS("Loads"),
    LOADS_DIN("Loads Dissolved Inorganic nitrogen"),
    LOADS_PSII("Loads PSII pesticides"),
    LOADS_TSS("Loads Total suspended solids"),
    LOADS_PN("Loads Particulate Nitrogen"),
    LOADS_PP("Loads Particulate Phosphorus"),
    CORAL_HCC("Hard coral cover"),
    CORAL_SCC("Soft coral cover"),
    CORAL_MA("Macroalgae"),
    CORAL_JUV("Juvenile density"),
    CORAL_HCC_GBR("Hard coral cover"),
    CORAL_SCC_GBR("Soft coral cover"),
    CORAL_MA_GBR("Macroalgae"),
    CORAL_JUV_GBR("Juvenile density"),
    PSII_TRENDS("PSII Trends"),
    PSII_MAX_HEQ("Maximum PSII Herbicide Equivalent Concentrations"),
    MARINE_WQT("Water quality trend"),
    MARINE_ST("Seagrass trend"),
    MARINE_CT("Coral trend"),
    RIPARIAN_FOREST_LOSS_TOTAL("Riparian forest loss since pre-European extend"),
    RIPARIAN_FOREST_LOSS("Riparian forest loss"),
    WETLANDS_LOSS("Wetlands loss"),
    WETLANDS_REMAINING("Wetlands remaining"),
    TOTAL_SUSPENDED_SEDIMENT("Total Suspended Sediment"),
    CHLOROPHYLL_A("Chlorophyll \u03b1"),
    ;

    private final String label;

    private ChartType(String label) {
      this.label = label;
    }

    public String getLabel() {
      return label;
    }

    public boolean hasUniqueLabel() {
      for(ChartType t : ChartType.values()) {
        if(this != t && StringUtils.equals(getLabel(), t.getLabel())) {
          return false;
        }
      }
      return true;
    }

    public static ChartType getChartType(String str) {
        return ChartType.valueOf(str.toUpperCase());
    }

    public static ChartType lookup(String s) {
      for(ChartType t : ChartType.values()) {
        if(StringUtils.equalsIgnoreCase(t.getLabel(), s) || 
            StringUtils.equalsIgnoreCase(t.name(), s)) {
          return t;
        }
      }
      return null;
    }
}
