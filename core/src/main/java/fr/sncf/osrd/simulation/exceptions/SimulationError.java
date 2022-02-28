package fr.sncf.osrd.simulation.exceptions;


import fr.sncf.osrd.OSRDException;

/**
 * A logic error in the simulation.
 */
public class SimulationError extends OSRDException {
    private static final long serialVersionUID = 2709122331146103500L;

    /** Time at which the error occurred */
    public final double time;

    public SimulationError(String message, double time) {
        super(simulationErrorCode, message, ErrorCause.USER);
        this.time = time;
    }

    public SimulationError(String code, String message, ErrorCause cause, double time) {
        super(code, message, cause);
        this.time = time;
    }
}
