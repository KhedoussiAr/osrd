package fr.sncf.osrd.simulation.exceptions;

import fr.sncf.osrd.OSRDException;

public class ChangeError extends OSRDException {
    private static final long serialVersionUID = 9023813991609794335L;

    public ChangeError(String message) {
        super(message, ErrorCause.INTERNAL);
    }
}
