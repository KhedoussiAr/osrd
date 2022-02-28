package fr.sncf.osrd.simulation.exceptions;

public class RouteError extends SimulationError {

    private static final long serialVersionUID = 4855294224197077488L;

    /** ID of the route that caused an error */
    public final String routeId;

    /** ID of the train that caused an error, if any (null otherwise) */
    public final String trainId;

    /** Constructor */
    public RouteError(String message, double time, String routeId, String trainId, ErrorCause cause) {
        super(routeErrorCode, message, ErrorCause.USER, time);
        this.routeId = routeId;
        this.trainId = trainId;
    }
}
