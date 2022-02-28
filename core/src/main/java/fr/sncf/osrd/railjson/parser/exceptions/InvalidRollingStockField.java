package fr.sncf.osrd.railjson.parser.exceptions;

public final class InvalidRollingStockField extends InvalidRollingStock {
    private static final long serialVersionUID = -1551144603639349811L;

    public final String fieldName;

    public InvalidRollingStockField(String fieldName, String message) {
        super(invalidRollingStockFieldCode, message);
        this.fieldName = fieldName;
    }
}
