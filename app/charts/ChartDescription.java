package charts;

import static com.google.common.base.Preconditions.checkNotNull;

public class ChartDescription {

    private final ChartType type;

    private final Region region;

    public ChartDescription(ChartType type) {
      this.type = checkNotNull(type);
      this.region = Region.GBR;
    }

    public ChartDescription(ChartType type, Region region) {
      this.type = checkNotNull(type);
      this.region = checkNotNull(region);
    }

    public ChartType getType() {
      return type;
    }

    public Region getRegion() {
      return region;
    }

}
