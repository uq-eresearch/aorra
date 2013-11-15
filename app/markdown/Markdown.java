package markdown;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.io.IOUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

public class Markdown {

    private static final String HTML_INTRO = "<!doctype html><html><head></head><body>";
    private static final String HTML_OUTRO = "</body></html>";

    public String toHtml(String markdown) {
        return HTML_INTRO + marked(markdown) + HTML_OUTRO;
    }

    public File toHtmlZip(String name, String markdown, String playSession) {
        String html = toHtml(markdown);
        File destination = Files.createTempDir();
        toFolder(destination,
                FilenameUtils.removeExtension(name)+".html", html, playSession);
        File zipFile = zip(destination, name);
        return zipFile;
    }

    private File zip(File destination, String name) {
        try {
            File z = new File(Files.createTempDir(), name+".zip");
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(z));
            for(String f : files(destination, "")) {
                out.putNextEntry(new ZipEntry(f));
                FileInputStream in = new FileInputStream(new File(destination, f));
                IOUtils.copy(in, out);
                in.close();
                out.closeEntry();
            }
            out.close();
            return z;
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> files(File folder, String base) {
        List<String> files = Lists.newArrayList();
        for(File f : folder.listFiles()) {
            if(f.isFile()) {
                files.add(base+f.getName());
            } else if(f.isDirectory()) {
                files.addAll(files(f, f.getName()+"/"));
            }
        }
        return files;
    }

    private File toFolder(File destination, String name, String html, String playSession) {
        try {
            html = downloadImages(html, destination, playSession);
            File f = new File(destination, name);
            FileUtils.write(f, html);
            return f;
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String downloadImages(String html, File destination, String playSession) {
        Map<String, String> srcMap = Maps.newHashMap();
        int fc = 0;
        String line = html.replace("\n", "");
        Pattern p = Pattern.compile(".*?<img.+?src=\"(.+?)\".*?>.*");
        while(true) {
            Matcher m = p.matcher(line);
            if(m.matches()) {
                String imgSrc = m.group(1);
                if(!srcMap.containsKey(imgSrc)) {
                    String localPath = String.format("files/img%s_%s",
                            Integer.toString(fc++), getFilename(imgSrc));
                    if(download(imgSrc, localPath, destination, playSession)) {
                        srcMap.put(imgSrc, localPath);
                    }
                }
                line = line.substring(line.indexOf(imgSrc)+1);
            } else {
                break;
            }
        }
        for(Map.Entry<String, String> me : srcMap.entrySet()) {
            html = StringUtils.replace(html, me.getKey(), me.getValue());
        }
        return html;
    }

    private boolean download(String src, String local, File destination, String playSession) {
        try {
            HttpClient client = new HttpClient();
            client.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
            URI u = new URI(src);
            String url;
            if(u.isAbsolute()) {
                url = u.toString();
            } else {
                url = "http://localhost:9000" + u.toString();
                HttpState state = new HttpState();
                Cookie session = new Cookie("localhost", "PLAY_SESSION",
                        playSession, "/", -1, false);
                state.addCookie(session);
                client.setState(state);
            }
            HttpMethod method = new GetMethod(url);
            method.setFollowRedirects(true);
            client.executeMethod(method);
            if(method.getStatusCode() == 200) {
                InputStream in = method.getResponseBodyAsStream();
                File f = new File(destination, local);
                f.getParentFile().mkdirs();
                FileOutputStream out = new FileOutputStream(new File(destination, local));
                IOUtils.copy(in, out);
                IOUtils.closeQuietly(out);
                method.releaseConnection();
                return true;
            } else {
                method.releaseConnection();
                return false;
            }
        } catch(Exception e) {
            return false;
        }
    }

    private String getFilename(String src) {
        try {
            URI u = new URI(src);
            String path = u.getPath();
            return FilenameUtils.getName(path);
        } catch(URISyntaxException e) {
            throw new RuntimeException(e);
        }
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
