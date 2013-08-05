package charts.builder;

public class ChartDescription {

    private ChartType type;

    private Region region;

    public ChartDescription(ChartType type) {
        super();
        this.type = type;
    }

    public ChartDescription(ChartType type, Region region) {
        this(type);
        this.region = region;
    }

    public ChartType getType() {
        return type;
    }

    public Region getRegion() {
        return region;
    }

}
