package fr.sncf.osrd.railjson.parser.exceptions;

public final class MissingRollingStockField extends InvalidRollingStock {
    private static final long serialVersionUID = 5412511233874626059L;

    public final String fieldName;

    public MissingRollingStockField(String fieldName) {
        super(missingRollingStockFieldCode, String.format("missing field {%s}", fieldName));
        this.fieldName = fieldName;
    }
}
