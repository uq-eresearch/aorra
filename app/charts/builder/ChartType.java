package charts.builder;

public enum ChartType {
    MARINE, COTS_OUTBREAK;

    public static ChartType getChartType(String str) {
        return ChartType.valueOf(str.toUpperCase());
    }
}
