package charts.builder;


public enum Region {

    GBR("GBR", "Great Barrier Reef"),
    CAPE_YORK("Cape York"),
    WET_TROPICS("Wet Tropics"),
    BURDEKIN("Burdekin"),
    MACKAY_WHITSUNDAY("Mackay Whitsunday"),
    FITZROY("Fitzroy"),
    BURNETT_MARY("Burnett Mary");

    private String properName;

    private String name;

    private Region(String name) {
        this.name = name;
    }

    private Region(String name, String properName) {
        this(name);
        this.properName= properName;
    }

    public String getName() {
        return name;
    }

    public String getProperName() {
        return properName!=null?properName:name;
    }

    public static Region getRegion(String name) {
        for(Region region : Region.values()) {
            if(region.name.equals(name)) {
                return region;
            }
        }
        return null;
    }

}
