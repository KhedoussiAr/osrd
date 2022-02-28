package fr.sncf.osrd.railjson.parser.exceptions;

import fr.sncf.osrd.OSRDException;

public class InvalidRollingStock extends OSRDException {
    private static final long serialVersionUID = -8380552148316200567L;

    public InvalidRollingStock(String message) {
        super(message, ErrorCause.USER);
    }

    public InvalidRollingStock() {
        this("Invalid rolling stock");
    }

    protected InvalidRollingStock(String code, String message) {
        super(message, ErrorCause.USER);
    }
}
