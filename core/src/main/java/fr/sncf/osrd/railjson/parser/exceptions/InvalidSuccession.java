package fr.sncf.osrd.railjson.parser.exceptions;

import fr.sncf.osrd.OSRDException;

public class InvalidSuccession extends OSRDException {
    private static final long serialVersionUID = -4629780236669835265L;

    public InvalidSuccession(String message) {
        super(invalidSuccessionCode, message, ErrorCause.USER);
    }

    public InvalidSuccession() {
        this("invalid succession");
    }

    protected InvalidSuccession(String code, String message) {
        super(code, message, ErrorCause.USER);
    }
}
