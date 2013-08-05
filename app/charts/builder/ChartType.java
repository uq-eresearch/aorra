package charts.builder;

public enum ChartType {
    MARINE;

    public static ChartType getChartType(String str) {
        return ChartType.valueOf(str.toUpperCase());
    }
}
