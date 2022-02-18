package fr.sncf.osrd.api;

import fr.sncf.osrd.OSRDException;
import org.takes.Response;
import org.takes.rs.RsJson;
import org.takes.rs.RsWithBody;
import org.takes.rs.RsWithStatus;

public class ExceptionHandler {

    /** Formats the exception as an HTTP response */
    public static Response convertToResponse(Throwable ex) {
        if (ex instanceof OSRDException)
            return convertOSRDErrorToResponse((OSRDException) ex);
        else {
            return new RsWithStatus(
                    new RsWithBody(ex.toString()),
                    500
            );
        }
    }

    private static Response convertOSRDErrorToResponse(OSRDException ex) {
        int code = ex.cause == OSRDException.ErrorCause.USER ? 400 : 500;
        return new RsWithStatus(
                new RsJson(
                        new RsWithBody(OSRDException.adapter.toJson(ex))
                ),
                code
        );
    }
}
