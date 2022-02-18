package fr.sncf.osrd.envelope_sim.allowances.mareco_impl;

import fr.sncf.osrd.envelope_sim.allowances.MarecoAllowance;
import fr.sncf.osrd.OSRDException;

public class MarecoConvergenceException extends OSRDException {

    private static final long serialVersionUID = -4300915709988986248L;
    public final String marecoErrorType;
    public final int allowanceIndex;
    public final double begin;
    public final double end;

    private MarecoConvergenceException(
            String message,
            ErrorCause cause,
            MarecoAllowance allowance,
            String marecoErrorType
    ) {
        super(marecoConvergenceExceptionCode, message, cause);
        this.allowanceIndex = allowance.allowanceIndex;
        this.marecoErrorType = marecoErrorType;
        this.begin = allowance.sectionBegin;
        this.end = allowance.sectionEnd;
    }

    /** Generates an error from a discontinuity in mareco search */
    public static MarecoConvergenceException discontinuity(MarecoAllowance allowance) {
        return new MarecoConvergenceException(
                "Mareco failed to converge when computing allowances because of a discontinuity in the search space",
                ErrorCause.INTERNAL,
                allowance,
                "discontinuity"
        );
    }

    /** Generates an error from setting were we can't go slow enough */
    public static MarecoConvergenceException tooMuchTime(MarecoAllowance allowance) {
        return new MarecoConvergenceException(
                "We could not go slow enough in this setting to match the given allowance time",
                ErrorCause.USER,
                allowance,
                "too_much_time"
        );
    }

    /** Generates an error from setting were we can't go fast enough */
    public static MarecoConvergenceException notEnoughTime(MarecoAllowance allowance) {
        return new MarecoConvergenceException(
                "We could not go fast enough in this setting to match the given allowance time",
                ErrorCause.INTERNAL,
                allowance,
                "not_enough_time"
        );
    }
}
