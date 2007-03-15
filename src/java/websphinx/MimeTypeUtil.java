package websphinx;

public class MimeTypeUtil {
    
    public static boolean isTextual(String mimeType) {
        if (mimeType.startsWith("text") 
                || mimeType.equals("application/xml")
                || mimeType.equals("application/xhtml+xml")
                || mimeType.equals("application/x-javascript")
                || mimeType.equals("application/javascript")) {
            return true;
        } else {
            return false;
        }
    }
    
    public static boolean isHTML(String mimeType) {
        if (mimeType.equals("text/html") || mimeType.equals("application/xhtml+xml")) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Returns a default extension for a given mime type, or an 
     * empty string if the mimetype is unknown.
     * @param type
     * @return
     */
    public static String getExtension(String type) {
        if (type.equals("text/html") || type.equals("application/xhtml+xml")) return "html";
        if (type.equals("text/xml") || type.equals("application/xml")) return "xml";
        if (type.equals("text/css")) return "css";
        if (type.equals("text/plain")) return "txt";
        if (type.equals("image/jpeg")) return "jpg";
        if (type.equals("image/gif")) return "gif";
        if (type.equals("image/png")) return "png";
        if (type.equals("application/x-javascript") || type.equals("text/javascript")) return "js";
        if (type.equals("application/pdf")) return "pdf";
        if (type.equals("application/zip")) return "zip";
        // TODO: add more
        return "";
    }
    
    /**
     * Returns the mime-type according to the given file extension.
     * Default is application/octet-stream.
     * @param extension
     * @return
     */
    protected String guessMimeType(String extension) {
        String ext = extension.toLowerCase();
        if (ext.equals("html") || ext.equals("htm")) return "text/html";
        if (ext.equals("css")) return "text/css";
        if (ext.equals("txt")) return "text/plain";
        if (ext.equals("js")) return "application/x-javascript";
        if (ext.equals("jpg") || ext.equals("jpg")) return "image/jpeg";
        if (ext.equals("gif")) return "image/gif";
        if (ext.equals("png")) return "image/png";
        if (ext.equals("pdf")) return "application/pdf";
        if (ext.equals("zip")) return "application/zip";
        // TODO: add more
        return "application/octet-stream"; // default
    }

}