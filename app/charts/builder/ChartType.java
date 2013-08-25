package charts.builder;

public enum ChartType {
    MARINE, COTS_OUTBREAK, ANNUAL_RAINFALL, PROGRESS_TABLE;

    public static ChartType getChartType(String str) {
        return ChartType.valueOf(str.toUpperCase());
    }
}
