package charts.builder;

public enum ChartType {
    MARINE, COTS_OUTBREAK, ANNUAL_RAINFALL, PROGRESS_TABLE,
    TTT_CANE_AND_HORT, TTT_GRAZING, TTT_SEDIMENT, TTT_NITRO_AND_PEST;

    public static ChartType getChartType(String str) {
        return ChartType.valueOf(str.toUpperCase());
    }
}
