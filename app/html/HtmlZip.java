package html;

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

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.io.IOUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

public class HtmlZip {
  
  private static final Pattern IMG_PATTERN =
      Pattern.compile("(<img.+?src=\")(.+?)(\".*?>)");
  private static final Pattern CSS_PATTERN =
      Pattern.compile("(<link.+?href=\")(.+?\\.css)(\".*?>)");

    public FileCleanup toHtmlZip(String name, String html, String playSession) {
        File destination = Files.createTempDir();
        toFolder(destination,
                FilenameUtils.removeExtension(name)+".html", html, playSession);
        File zipFile = zip(destination, name);
        return new FileCleanup(zipFile, zipFile.getParentFile(), destination);
    }

    public File toFolder(File destination, String name, String html, String playSession) {
        try {
            html = download(html, destination, IMG_PATTERN, "img", playSession);
            html = download(html, destination, CSS_PATTERN, "css", playSession);
            File f = new File(destination, name);
            FileUtils.write(f, html);
            return f;
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
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

    private String download(String html, File destination, Pattern p,
        String prefix, String playSession) throws IOException {
      Map<String, String> srcMap = Maps.newHashMap();
      int fc = 0;
      Matcher m = p.matcher(html);
      StringBuffer result = new StringBuffer();
      while(m.find()) {
          String href = m.group(2);
          String localPath;
          if(!srcMap.containsKey(href)) {
              localPath = String.format("files/%s%s_%s",
                      prefix, Integer.toString(fc++), getFilename(href));
              srcMap.put(href, localPath);
              download(href, localPath, destination, playSession);
          } else {
              localPath = srcMap.get(href);
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
}
