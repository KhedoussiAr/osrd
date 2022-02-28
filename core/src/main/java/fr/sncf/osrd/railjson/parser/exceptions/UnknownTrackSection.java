package fr.sncf.osrd.railjson.parser.exceptions;

public class UnknownTrackSection extends InvalidSchedule {
    private static final long serialVersionUID = -7305193783954300262L;

    public final String trackSectionID;

    public UnknownTrackSection(String message, String trackSectionID) {
        super(unknownTrackSectionCode, message);
        this.trackSectionID = trackSectionID;
    }
}
