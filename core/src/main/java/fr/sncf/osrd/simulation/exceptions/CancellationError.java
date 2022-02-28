package fr.sncf.osrd.simulation.exceptions;

public class CancellationError extends SimulationError {
    private static final long serialVersionUID = -7136817054164228209L;

    public CancellationError(String message, double time) {
        super(cancellationCode, message, ErrorCause.INTERNAL, time);
    }
}
