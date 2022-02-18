package fr.sncf.osrd.infra;

import fr.sncf.osrd.OSRDException;

public class InvalidInfraException extends OSRDException {
    private static final long serialVersionUID = -8946928669397353451L;

    public InvalidInfraException(String message) {
        super(invalidInfraExceptionCode, message, ErrorCause.USER);
    }
}
