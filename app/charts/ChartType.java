package charts;

public enum ChartType {
    MARINE ("Marine"),
    COTS_OUTBREAK ("Crown of Thorns Outbreak"),
    ANNUAL_RAINFALL ("Annual Rainfall"),
    PROGRESS_TABLE ("Progress Table"),
    PROGRESS_TABLE_REGION ("Progress Table Region"),
    TTT_CANE_AND_HORT ("Tracking Towards Target - Cane and Horticulture"),
    TTT_GRAZING ("Tracking Towards Target - Grazing"),
    TTT_SEDIMENT ("Tracking Towards Target - Sediment"),
    TTT_NITRO_AND_PEST ("Tracking Towards Target - Nitrogen and Pesticide"),
    GRAZING_PS ("Grazing Practice Systems"),
    HORTICULTURE_PS ("Horticulture Practice Systems"),
    SUGARCANE_PS ("Sugarcane Practice Systems"),
    GRAINS_PS ("Grains Practice Systems"),
    TSA("Trends in seagrass abundance"),
    GROUNDCOVER("Groundcover"),
    GROUNDCOVER_BELOW_50("Groundcover below 50%"),
    LOADS("Loads"),
    CORAL_HCC("Hard coral cover"),
    CORAL_SCC("Soft coral cover"),
    CORAL_MA("Macroalgae"),
    CORAL_JUV("Juvenile density");

    private final String label;

    private ChartType(String label) {
      this.label = label;
    }

    public String getLabel() {
      return label;
    }

    public static ChartType getChartType(String str) {
        return ChartType.valueOf(str.toUpperCase());
    }
}
