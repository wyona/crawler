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
}