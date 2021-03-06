package html;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.google.common.collect.Lists;
import com.google.common.io.Files;

public class HtmlToPdf {

    private static final String CMD_WEASYPRINT = "weasyprint";

    public TempFiles toPdf(String name, String html, String playSession, String copts) {
        String filename = FilenameUtils.removeExtension(name)+".html";
        File htmlFile = new HtmlZip().toFolder(Files.createTempDir(), filename, html, playSession);
        File pdfFile = pdf(htmlFile, copts);
        return new TempFiles(pdfFile, pdfFile.getParentFile(), htmlFile.getParentFile());
    }

    private File pdf(File in, String copts) {
        try {
            File destfolder = Files.createTempDir();
            File out = new File(destfolder, FilenameUtils.removeExtension(in.getName())+".pdf");
            ProcessBuilder pb = converter(copts, in, out);
            pb.directory(in.getParentFile());
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

    private ProcessBuilder converter(String copts, File in, File out) throws IOException {
        File css = new File(in.getParentFile(), "print.css");
        FileUtils.copyInputStreamToFile(this.getClass().getResourceAsStream("weasyprint.css"), css);
        return new ProcessBuilder(cmd(CMD_WEASYPRINT, "--stylesheet", css.getAbsolutePath(),
                options(copts), in.getName(), out.getAbsolutePath()));
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

}
