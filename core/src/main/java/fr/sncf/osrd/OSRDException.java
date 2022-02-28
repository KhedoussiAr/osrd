package fr.sncf.osrd;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory;
import fr.sncf.osrd.envelope_sim.allowances.mareco_impl.MarecoConvergenceException;
import fr.sncf.osrd.infra.InvalidInfraException;
import fr.sncf.osrd.railjson.parser.exceptions.*;
import fr.sncf.osrd.simulation.exceptions.*;

public abstract class OSRDException extends RuntimeException {

    private static final long serialVersionUID = 1197516372515951853L;

    /** Detailed error message */
    public final String message;

    /** Whether this is an internal or user error, used to determine if we send a 400 or 500 code */
    public final ErrorCause cause;

    public enum ErrorCause {
        INTERNAL,
        USER
    }

    // Placing these fields in the child classes can cause class loading deadlocks
    protected static final String marecoConvergenceCode = "core:standalone_sim:mareco_convergence";

    protected static final String invalidInfraCode = "core:infra:invalid_infra";

    protected static final String invalidRollingStockCode = "core:parser:invalid_rolling_stock";
    protected static final String invalidRollingStockFieldCode = invalidRollingStockCode + ":invalid_field";
    protected static final String missingRollingStockFieldCode = invalidRollingStockCode + ":missing_field";
    protected static final String invalidScheduleCode = "core:parser:invalid_schedule";
    protected static final String unknownRollingStockCode = invalidScheduleCode + ":unknown_rolling_stock";
    protected static final String unknownRouteCode = invalidScheduleCode + ":unknown_route";
    protected static final String unknownTrackSectionCode = invalidScheduleCode + ":unknown_track_section";
    protected static final String invalidSuccessionCode = "core:parser:invalid_succession";
    protected static final String missingSuccessionTableFieldCode = invalidSuccessionCode + ":missing_table_field";

    protected static final String simulationErrorCode = "core:simulation";
    protected static final String tvdErrorCode = simulationErrorCode + ":tvd";
    protected static final String routeErrorCode = simulationErrorCode + ":route";
    protected static final String tstErrorCode = simulationErrorCode + ":tst";
    protected static final String trainErrorCode = simulationErrorCode + ":train";
    protected static final String cancellationCode = simulationErrorCode + ":cancellation";
    protected static final String changeCode = simulationErrorCode + ":change";


    protected OSRDException(String message, ErrorCause cause) {
        this.message = message;
        this.cause = cause;
    }

    public static final JsonAdapter<OSRDException> adapter = new Moshi.Builder()
            .add(PolymorphicJsonAdapterFactory.of(OSRDException.class, "type")
                    .withSubtype(MarecoConvergenceException.class, marecoConvergenceCode)
                    .withSubtype(InvalidInfraException.class, invalidInfraCode)
                    .withSubtype(InvalidRollingStock.class, invalidRollingStockCode)
                    .withSubtype(InvalidRollingStockField.class, invalidRollingStockFieldCode)
                    .withSubtype(MissingRollingStockField.class, missingRollingStockFieldCode)
                    .withSubtype(InvalidSchedule.class, invalidScheduleCode)
                    .withSubtype(UnknownRollingStock.class, unknownRollingStockCode)
                    .withSubtype(UnknownRoute.class, unknownRouteCode)
                    .withSubtype(UnknownTrackSection.class, unknownTrackSectionCode)
                    .withSubtype(InvalidSuccession.class, invalidSuccessionCode)
                    .withSubtype(MissingSuccessionTableField.class, missingSuccessionTableFieldCode)
                    .withSubtype(SimulationError.class, simulationErrorCode)
                    .withSubtype(TVDError.class, tvdErrorCode)
                    .withSubtype(RouteError.class, routeErrorCode)
                    .withSubtype(TSTError.class, tstErrorCode)
                    .withSubtype(TrainError.class, trainErrorCode)
                    .withSubtype(CancellationError.class, cancellationCode)
                    .withSubtype(ChangeError.class, changeCode)
            )
            .build()
            .adapter(OSRDException.class);
}
