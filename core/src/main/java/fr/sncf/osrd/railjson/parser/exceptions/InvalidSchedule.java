package fr.sncf.osrd.railjson.parser.exceptions;

import fr.sncf.osrd.OSRDException;

public class InvalidSchedule extends OSRDException {
    private static final long serialVersionUID = 5681057974753003734L;

    public InvalidSchedule(String message) {
        super(message, ErrorCause.USER);
    }

    protected InvalidSchedule(String code, String message) {
        super(message, ErrorCause.USER);
    }
}
