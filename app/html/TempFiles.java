package html;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.Lists;

public class TempFiles implements AutoCloseable {

    private List<File> cleanup = Lists.newArrayList();

    TempFiles(File... cleanup) {
        this.cleanup.addAll(Arrays.asList(cleanup));
    }

    public File result() {
        return cleanup.get(0);
    }

    public void cleanup() {
        for(File f : cleanup) {
            FileUtils.deleteQuietly(f);
        }
    }

    @Override
    public void close() throws Exception {
      cleanup();
    }

}
