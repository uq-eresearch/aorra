package boxrenderer;


public class Margin extends Spacing {

    public Margin() {
        super();
        setRender(false);
    }

    public Margin(int margin) {
        super(margin);
        setRender(false);
    }
    
    public Margin(int top, int left, int bottom, int right) {
        super(top, left, bottom, right);
        setRender(false);
    }
}
