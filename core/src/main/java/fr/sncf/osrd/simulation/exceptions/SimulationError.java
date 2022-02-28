package fr.sncf.osrd.simulation.exceptions;


import fr.sncf.osrd.OSRDException;

/**
 * A logic error in the simulation.
 */
public class SimulationError extends OSRDException {
    private static final long serialVersionUID = 2059504245347497991L;

    /** Time at which the error occurred */
    public final double time;

    public SimulationError(String message, double time) {
        super(message, ErrorCause.USER);
        this.time = time;
    }

    public SimulationError(String message, ErrorCause cause, double time) {
        super(message, cause);
        this.time = time;
    }
}
