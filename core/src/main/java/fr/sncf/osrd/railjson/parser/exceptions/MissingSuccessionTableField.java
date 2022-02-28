package fr.sncf.osrd.railjson.parser.exceptions;

public final class MissingSuccessionTableField extends InvalidSuccession {
    private static final long serialVersionUID = -449949414344839739L;

    public final String fieldName;

    public MissingSuccessionTableField(String fieldName) {
        super(missingSuccessionTableFieldCode, String.format("missing field {%s}", fieldName));
        this.fieldName = fieldName;
    }
}