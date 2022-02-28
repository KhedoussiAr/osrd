package fr.sncf.osrd.simulation.exceptions;

public class TSTError extends SimulationError {

    private static final long serialVersionUID = -6040356036097814149L;
    public final String trainID;

    public TSTError(String message, double time, String trainID) {
        super(tstErrorCode, message, ErrorCause.INTERNAL, time);
        this.trainID = trainID;
    }
}
