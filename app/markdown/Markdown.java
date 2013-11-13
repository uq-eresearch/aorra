package markdown;

import java.io.IOException;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.tika.io.IOUtils;

public class Markdown {

    private static final String HTML_INTRO = "<!doctype html><html><head></head><body>";
    private static final String HTML_OUTRO = "</body></html>";

    public String toHtml(String markdown) {
        return HTML_INTRO + marked(markdown) + HTML_OUTRO;
    }

    private String marked(String markdown) {
        try {
            ScriptEngineManager mgr = new ScriptEngineManager();
            ScriptEngine engine = mgr.getEngineByName("JavaScript");
            engine.eval(IOUtils.toString(this.getClass().getResourceAsStream("marked.min.js")));
            Object result = engine.eval(String.format("marked('%s')",
                    StringEscapeUtils.escapeJavaScript(markdown)));
            return result!=null?result.toString():null;
        } catch(ScriptException e) {
            throw new RuntimeException(e);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

}
