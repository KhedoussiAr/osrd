package fr.sncf.osrd.railjson.parser.exceptions;

import fr.sncf.osrd.OSRDException;

public class InvalidSchedule extends OSRDException {
    private static final long serialVersionUID = 1166970317776716683L;

    public InvalidSchedule(String message) {
        super(invalidScheduleCode, message, ErrorCause.USER);
    }

    protected InvalidSchedule(String code, String message) {
        super(code, message, ErrorCause.USER);
    }
}
