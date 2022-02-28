package fr.sncf.osrd.simulation.exceptions;

/** A simulation error related to TVD section occupancy */
public class TVDError extends SimulationError {

    private static final long serialVersionUID = -2576312544451261656L;
    public final String tvdId;

    public TVDError(String message, String tvdId, double time) {
        super(tvdErrorCode, message, ErrorCause.USER, time);
        this.tvdId = tvdId;
    }
}
