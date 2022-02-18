package fr.sncf.osrd;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory;
import fr.sncf.osrd.envelope_sim.allowances.mareco_impl.MarecoConvergenceException;

public abstract class OSRDException extends RuntimeException {

    private static final long serialVersionUID = -9203463704974493127L;

    /** Error code identifier, format `module:submodule:type` */
    public final String type;

    /** Detailed error message */
    public final String message;

    /** Whether this is an internal or user error, used to determine if we send a 400 or 500 code */
    public final ErrorCause cause;

    public enum ErrorCause {
        INTERNAL,
        USER
    }

    // Placing these fields in the child classes can cause class loading deadlocks
    protected static final String marecoConvergenceExceptionCode = "core:standalone_sim:mareco_convergence";
    protected static final String invalidInfraExceptionCode = "core:infra:invalid_infra";


    protected OSRDException(String type, String message, ErrorCause cause) {
        this.type = type;
        this.message = message;
        this.cause = cause;
    }

    public static final JsonAdapter<OSRDException> adapter = new Moshi.Builder()
            .add(PolymorphicJsonAdapterFactory.of(OSRDException.class, "type")
                    .withSubtype(MarecoConvergenceException.class, marecoConvergenceExceptionCode)
            )
            .build()
            .adapter(OSRDException.class);
}
