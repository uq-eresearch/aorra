package boxrenderer;


public class Padding extends Spacing {

    public Padding() {
        super();
    }

    public Padding(int padding) {
        super(padding);
    }
    
    public Padding(int top, int left, int bottom, int right) {
        super(top, left, bottom, right);
    }
}
