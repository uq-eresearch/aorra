package boxrenderer;

public interface ContentBox extends Box {

    public void addContent(Box content);

    public void setAlign(Align align);
}
