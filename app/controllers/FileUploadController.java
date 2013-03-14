package controllers;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import play.Logger;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;

public class FileUploadController extends Controller {

    public static Result postUpload() {
        MultipartFormData body = request().body().asMultipartFormData();
        StringBuilder buf = new StringBuilder("{\"files\": [");
        List<FilePart> files = body.getFiles();
        Iterator<FilePart> iter = files.iterator();
        while(iter.hasNext()) {
            FilePart filePart = iter.next();
            String fileName = filePart.getFilename();
            File file = filePart.getFile();
            buf.append("{");
            buf.append(String.format("\"name\": \"%s\",", fileName));
            buf.append(String.format("\"size\": \"%s\"", file.length()));
            buf.append("}");
            if(iter.hasNext()) {
                buf.append(",");
            }
            Logger.info(String.format("file %s content type %s uploaded to %s", 
                    fileName, filePart.getContentType(), file.getAbsolutePath()));
        }
        buf.append("]}");
        // even though we return json set the content type to text/html to prevent IE/Opera
        // from opening a download dialog as described here:
        // https://github.com/blueimp/jQuery-File-Upload/wiki/Setup
        return ok(buf.toString()).as("text/html");
    }

    public static Result getUpload() {
        return ok(views.html.upload.render());
    }

}
