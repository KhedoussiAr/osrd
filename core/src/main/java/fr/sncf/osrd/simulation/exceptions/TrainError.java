package fr.sncf.osrd.simulation.exceptions;

public class TrainError extends SimulationError {

    private static final long serialVersionUID = 1414380145475434313L;

    /** ID of the train that caused an error */
    public final String trainId;

    /** ID of the signal that caused an error, if any (null otherwise) */
    public final String signalID;

    /** Constructor */
    public TrainError(String message, double time, String trainId, String signalID, ErrorCause cause) {
        super(trainErrorCode, message, cause, time);
        this.trainId = trainId;
        this.signalID = signalID;
    }
}
