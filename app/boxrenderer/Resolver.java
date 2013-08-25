package boxrenderer;

import java.io.InputStream;

public interface Resolver {

    public InputStream resolve(String source) throws Exception;

}
