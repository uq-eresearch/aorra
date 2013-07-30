package charts;


public enum Region {

    CAPE_YORK("Cape York"),
    WET_TROPICS("Wet Tropics"),
    BURDEKIN("Burdekin"),
    MACKAY_WHITSUNDAYS("Mackay Whitsundays"),
    FITZROY("Fitzroy"),
    BURNETT_MARY("Burnett Mary"),
    GBR("GBR");

    private String name;

    private Region(String name) {
        this.name= name;
    }

    public String getName() {
        return name;
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
