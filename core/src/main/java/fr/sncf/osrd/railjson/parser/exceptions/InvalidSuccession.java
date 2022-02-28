package fr.sncf.osrd.railjson.parser.exceptions;

import fr.sncf.osrd.OSRDException;

public class InvalidSuccession extends OSRDException {
    private static final long serialVersionUID = -3695562224328844732L;

    public InvalidSuccession(String message) {
        super(message, ErrorCause.USER);
    }

    public InvalidSuccession() {
        this("invalid succession");
    }

    protected InvalidSuccession(String code, String message) {
        super(message, ErrorCause.USER);
    }
}
