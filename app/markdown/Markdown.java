package markdown;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
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

    private static final String CMD_WK = "/usr/bin/wkhtmltopdf";
    private static final String CMD_PANDOC = "/usr/bin/pandoc";

    private static final String MISSING = "files/img_missing.png";

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

    public File toPdf(String name, String markdown, String playSession, String converter, String copts) {
        String html = toHtml(markdown);
        File destination = Files.createTempDir();
        String filename = FilenameUtils.removeExtension(name)+".html";
        File htmlFile = toFolder(destination, filename, html, playSession);
        return pdf(htmlFile, converter, copts);
    }

    private File pdf(File in, String converter, String copts) {
        try {
            File destfolder = Files.createTempDir();
            File out = new File(destfolder, FilenameUtils.removeExtension(in.getName())+".pdf");
            ProcessBuilder pb = converter(choose(converter), copts, in, out);
            pb.directory(in.getParentFile());
            pb.redirectErrorStream(true);
            File log = new File(destfolder, "converter.log");
            pb.redirectErrorStream(true);
            pb.redirectOutput(Redirect.to(log));
            Process p = pb.start();
            // TODO do not wait forever
            int es = p.waitFor();
            if(out.exists()) {
                return out;
            } else {
                String msg = "pdf creation failed with exit status "+es;
                if(log.exists()) {
                    throw new RuntimeException(msg+"\n"+FileUtils.readFileToString(log)+
                            "\nfrom "+log.getAbsolutePath());
                } else {
                    throw new RuntimeException(msg+" "+destfolder.getAbsolutePath());
                }
            }
        } catch(Exception e) {
            throw new RuntimeException(String.format("failed to create pdf from %s",in.getName()),e);
        }
    }

    private String choose(String converter) {
        List<String> available = Lists.newArrayList();
        if(new File(CMD_WK).exists()) {
            available.add(CMD_WK);
        }
        if(new File(CMD_PANDOC).exists()) {
            available.add(CMD_PANDOC);
        }
        if(available.isEmpty()) {
            throw new RuntimeException("no converter available (install pandoc or wkhtmltopdf)");
        } else if(available.size() == 1) {
            return available.get(0);
        } else {
            for(String s : available) {
                if(StringUtils.contains(s, converter)) {
                    return s;
                }
            }
            return available.get(0);
        }
    }

    private ProcessBuilder converter(String converter, String copts, File in, File out) {
        if(StringUtils.equals(converter, CMD_WK)) {
            return new ProcessBuilder(cmd(converter,
                    options(copts), in.getName(), out.getAbsolutePath()));
        } else if(StringUtils.equals(converter,CMD_PANDOC)) {
            return new ProcessBuilder(cmd(converter,
                    options(copts), "-o", out.getAbsolutePath(), in.getName()));
        } else {
            throw new RuntimeException(String.format("unknown converter '%s'", converter));
        }
    }

    private List<String> cmd(Object... params) {
        List<String> cmd = Lists.newArrayList();
        for(Object o : params) {
            if(o instanceof String) {
                cmd.add((String)o);
            } else if(o instanceof List<?>) {
                for(Object o2 : (List<?>)o) {
                    if(o2 instanceof String) {
                        cmd.add((String)o2);
                    } else {
                        throw new RuntimeException();
                    }
                }
            } else {
                throw new RuntimeException();
            }
        }
        return cmd;
    }

    private List<String> options(String copts) {
        return split(copts);
    }

    //from http://stackoverflow.com/a/366532
    private List<String> split(String copts) {
        List<String> matchList = Lists.newArrayList();
        Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
        Matcher regexMatcher = regex.matcher(copts);
        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                // Add double-quoted string without the quotes
                matchList.add(regexMatcher.group(1));
            } else if (regexMatcher.group(2) != null) {
                // Add single-quoted string without the quotes
                matchList.add(regexMatcher.group(2));
            } else {
                // Add unquoted word
                matchList.add(regexMatcher.group());
            }
        }
        return matchList;
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

    private String downloadImages(String html, File destination, String playSession) throws IOException {
        Map<String, String> srcMap = Maps.newHashMap();
        int fc = 0;
        Pattern p = Pattern.compile("(<img.+?src=\")(.+?)(\".*?>)");
        Matcher m = p.matcher(html);
        StringBuffer result = new StringBuffer();
        while(m.find()) {
            String imgSrc = m.group(2);
            String localPath;
            if(!srcMap.containsKey(imgSrc)) {
                localPath = String.format("files/img%s_%s",
                        Integer.toString(fc), getFilename(imgSrc));
                if(download(imgSrc, localPath, destination, playSession)) {
                    fc++;
                } else {
                    File fMissing = new File(destination, MISSING);
                    if(!fMissing.exists()) {
                        InputStream in = this.getClass().getResourceAsStream("missing.png");
                        FileOutputStream out = new FileOutputStream(fMissing);
                        IOUtils.copy(in, out);
                        IOUtils.closeQuietly(out);
                    }
                    localPath = MISSING;
                }
                srcMap.put(imgSrc, localPath);
            } else {
                localPath = srcMap.get(imgSrc);
            }
            m.appendReplacement(result, String.format("$1%s$3", localPath));
        }
        m.appendTail(result);
        return result.toString();
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
