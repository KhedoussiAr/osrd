package fr.sncf.osrd.railjson.parser.exceptions;

import fr.sncf.osrd.OSRDException;

public class InvalidRollingStock extends OSRDException {
    private static final long serialVersionUID = -7902941815959834009L;

    public InvalidRollingStock(String message) {
        super(invalidRollingStockCode, message, ErrorCause.USER);
    }

    public InvalidRollingStock() {
        this("Invalid rolling stock");
    }

    protected InvalidRollingStock(String code, String message) {
        super(code, message, ErrorCause.USER);
    }
}
