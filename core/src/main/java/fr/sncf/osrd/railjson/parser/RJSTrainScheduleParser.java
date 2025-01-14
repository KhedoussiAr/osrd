package fr.sncf.osrd.railjson.parser;

import fr.sncf.osrd.train.RollingStock;
import fr.sncf.osrd.cbtc.CBTCNavigatePhase;
import fr.sncf.osrd.railjson.schema.common.ID;
import fr.sncf.osrd.railjson.schema.infra.RJSRoute;
import fr.sncf.osrd.railjson.schema.schedule.*;
import fr.sncf.osrd.speedcontroller.SpeedInstructions;
import fr.sncf.osrd.train.TrainSchedule;
import fr.sncf.osrd.infra.Infra;
import fr.sncf.osrd.infra.routegraph.Route;
import fr.sncf.osrd.railjson.parser.exceptions.InvalidSchedule;
import fr.sncf.osrd.railjson.parser.exceptions.UnknownRollingStock;
import fr.sncf.osrd.railjson.parser.exceptions.UnknownRoute;
import fr.sncf.osrd.speedcontroller.generators.*;
import fr.sncf.osrd.train.TrainPath;
import fr.sncf.osrd.train.TrainStop;
import fr.sncf.osrd.train.VirtualPoint;
import fr.sncf.osrd.train.decisions.KeyboardInput;
import fr.sncf.osrd.train.decisions.TrainDecisionMaker;
import fr.sncf.osrd.train.phases.NavigatePhase;
import fr.sncf.osrd.train.phases.SignalNavigatePhase;
import fr.sncf.osrd.utils.SortedDoubleMap;
import fr.sncf.osrd.utils.TrackSectionLocation;
import fr.sncf.osrd.utils.graph.EdgeDirection;
import java.util.*;
import java.util.function.Function;

public class RJSTrainScheduleParser {
    /** Parses a RailJSON train schedule */
    public static TrainSchedule parse(
            Infra infra,
            Function<String, RollingStock> rollingStockGetter,
            RJSTrainSchedule rjsTrainSchedule
    ) throws InvalidSchedule {
        return parse(infra, rollingStockGetter, rjsTrainSchedule, null);
    }


    /** Parses a RailJSON train schedule */
    public static TrainSchedule parse(
            Infra infra,
            Function<String, RollingStock> rollingStockGetter,
            RJSTrainSchedule rjsTrainSchedule,
            List<RJSVirtualPoint> rjsVirtualPoints
    ) throws InvalidSchedule {
        RollingStock rollingStock = null;
        var rollingStockID = rjsTrainSchedule.rollingStock;
        if (rjsTrainSchedule.previousTrainId == null) {
            rollingStock = rollingStockGetter.apply(rollingStockID);
            if (rollingStock == null)
                throw new UnknownRollingStock(rollingStockID);
        } else if (rollingStockID != null) {
            throw new InvalidSchedule(
                    String.format("Train %s: can't specify both a rolling stock and a previous train",
                            rjsTrainSchedule.id));
        }

        var initialLocation = RJSTrackLocationParser.parse(infra, rjsTrainSchedule.initialHeadLocation);

        var expectedPath = parsePath(infra, rjsTrainSchedule.phases, rjsTrainSchedule.routes, initialLocation);

        var stops = RJSStopsParser.parse(rjsTrainSchedule.stops, infra, expectedPath);

        var virtualPoints = parseVirtualPoints(rjsVirtualPoints, infra, expectedPath);

        var initialRouteID = rjsTrainSchedule.routes[0].id;
        var initialRoute = infra.routeGraph.routeMap.get(initialRouteID);
        if (initialRoute == null)
            throw new UnknownRoute("unknown initial route", initialRouteID);

        var initialSpeed = rjsTrainSchedule.initialSpeed;
        if (Double.isNaN(initialSpeed) || initialSpeed < 0)
            throw new InvalidSchedule("invalid initial speed");

        // parse the sequence of phases, keeping track of the location of the train between phases
        var phases = new ArrayList<NavigatePhase>();
        var beginLocation = initialLocation;
        for (var rjsPhase : rjsTrainSchedule.phases) {
            var phase = parsePhase(infra, beginLocation, rjsPhase, expectedPath, stops, virtualPoints);
            var endLocation = phase.getEndLocation();
            if (endLocation != null)
                beginLocation = endLocation;
            phases.add(phase);
        }

        // find from what direction the train arrives on the initial location
        EdgeDirection initialDirection = null;
        var tvdSectionPaths = initialRoute.tvdSectionsPaths;

        trackSectionLoop:
        for (var tvdSectionPath : tvdSectionPaths) {
            for (var trackSectionRange : tvdSectionPath.trackSections) {
                if (trackSectionRange.containsLocation(initialLocation)) {
                    initialDirection = trackSectionRange.direction;
                    break trackSectionLoop;
                }
            }
        }

        var targetSpeedGenerators = parseSpeedControllerGenerators(rjsTrainSchedule,
                expectedPath, infra);
        var refTimes = parseReferenceTimes(rjsTrainSchedule.referenceTimes);
        var speedInstructions = new SpeedInstructions(targetSpeedGenerators, refTimes);

        if (initialDirection == null)
            throw new InvalidSchedule("the initial location isn't on the initial route");

        return new TrainSchedule(
                rjsTrainSchedule.id,
                rollingStock,
                rjsTrainSchedule.departureTime,
                initialLocation,
                initialRoute,
                initialSpeed,
                phases,
                parseDecisionMaker(rjsTrainSchedule.trainControlMethod),
                expectedPath,
                speedInstructions,
                stops);
    }

    /** Resolves references between schedules, in particular train successions */
    public static void resolveScheduleDependencies(Collection<RJSTrainSchedule> rjsSchedules,
                                                   List<TrainSchedule> schedules)
            throws InvalidSchedule {
        var schedulesById = new HashMap<String, TrainSchedule>();
        for (var schedule : schedules)
            schedulesById.put(schedule.trainID, schedule);

        for (var rjsSchedule : rjsSchedules) {
            if (rjsSchedule.previousTrainId == null)
                continue;
            if (!schedulesById.containsKey(rjsSchedule.previousTrainId))
                throw new InvalidSchedule(String.format("Succession error: train %s depends on non-existent train %s",
                        rjsSchedule.id, rjsSchedule.previousTrainId));
            double delay = 0;
            if (!Double.isNaN(rjsSchedule.trainTransitionDelay))
                delay = rjsSchedule.trainTransitionDelay;
            var schedule = schedulesById.get(rjsSchedule.id);
            var previousTrainSchedule = schedulesById.get(rjsSchedule.previousTrainId);

            if (previousTrainSchedule.trainSuccession != null)
                throw new InvalidSchedule(String.format("Train %s can't be linked to several next trains (%s and %s)",
                        previousTrainSchedule.trainID, previousTrainSchedule.trainSuccession.nextTrain.trainID,
                        rjsSchedule.id));

            previousTrainSchedule.trainSuccession = new TrainSchedule.TrainSuccession(schedule, delay);
            schedule.departureTime = -1;
            schedule.rollingStock = previousTrainSchedule.rollingStock;
        }
    }

    /** Parses the expected times at each position, if specified */
    private static SortedDoubleMap parseReferenceTimes(List<RJSTrainSchedule.RJSTimePoint> points) {
        if (points == null)
            return null;
        var res = new SortedDoubleMap();
        for (var p : points)
            res.put(p.position, p.time);
        return res;
    }

    private static double[] parseAllowanceBeginEnd(RJSLegacyAllowance allowance,
                                                   TrainPath path,
                                                   Infra infra) throws InvalidSchedule {
        if (allowance.beginLocation != null && allowance.beginPosition != null)
            throw new InvalidSchedule("Can't set both begin_location and begin_position for an allowance");
        if (allowance.endLocation != null && allowance.endPosition != null)
            throw new InvalidSchedule("Can't set both end_location and end_position for an allowance");

        double begin = 0;
        double end = Double.POSITIVE_INFINITY;

        if (allowance.beginLocation != null)
            begin = path.convertTrackLocation(RJSTrackLocationParser.parse(infra, allowance.beginLocation));
        else if (allowance.beginPosition != null)
            begin = allowance.beginPosition;

        if (allowance.endLocation != null)
            end = path.convertTrackLocation(RJSTrackLocationParser.parse(infra, allowance.endLocation));
        else if (allowance.endPosition != null)
            end = allowance.endPosition;

        return new double[]{begin, end};
    }

    private static SpeedControllerGenerator parseSpeedControllerGenerator(RJSLegacyAllowance allowance,
                                                                          TrainPath path,
                                                                          Infra infra)
            throws InvalidSchedule {
        if (allowance == null)
            return new MaxSpeedGenerator();

        var beginAndEnd = parseAllowanceBeginEnd(allowance, path, infra);
        var begin = beginAndEnd[0];
        var end = beginAndEnd[1];

        if (allowance instanceof RJSLegacyAllowance.Linear) {
            var linearAllowance = (RJSLegacyAllowance.Linear) allowance;
            return new LinearAllowanceGenerator(begin, end,
                    linearAllowance.allowanceValue, linearAllowance.allowanceType);
        } else if (allowance instanceof RJSLegacyAllowance.Construction) {
            var constructionAllowance = (RJSLegacyAllowance.Construction) allowance;
            return new ConstructionAllowanceGenerator(begin, end,
                    constructionAllowance.allowanceValue);
        } else if (allowance instanceof RJSLegacyAllowance.Mareco) {
            var marecoAllowance = (RJSLegacyAllowance.Mareco) allowance;
            return new MarecoAllowanceGenerator(begin, end,
                    marecoAllowance.allowanceValue, marecoAllowance.allowanceType);
        } else {
            throw new InvalidSchedule("Unimplemented allowance type");
        }
    }

    private static TrainDecisionMaker parseDecisionMaker(String decisionMakerType) throws InvalidSchedule {
        if (decisionMakerType == null || decisionMakerType.equals("default")) {
            return new TrainDecisionMaker.DefaultTrainDecisionMaker();
        } else if (decisionMakerType.equals("keyboard")) {
            return new KeyboardInput(2);
        } else {
            throw new InvalidSchedule(String.format("Unknown decision maker type: %s", decisionMakerType));
        }
    }

    private static List<Set<SpeedControllerGenerator>> parseSpeedControllerGenerators(RJSTrainSchedule phase,
                                                                                      TrainPath path,
                                                                                      Infra infra)
            throws InvalidSchedule {
        List<Set<SpeedControllerGenerator>> list = new ArrayList<>();
        if (phase.allowances == null)
            return list;
        for (var allowanceSet : phase.allowances) {
            var set = new HashSet<SpeedControllerGenerator>();
            for (var allowance : allowanceSet)
                set.add(parseSpeedControllerGenerator(allowance, path, infra));
            list.add(set);
        }
        return list;
    }

    private static NavigatePhase parsePhase(
            Infra infra,
            TrackSectionLocation startLocation,
            RJSTrainPhase rjsPhase,
            TrainPath expectedPath,
            List<TrainStop> stops,
            List<VirtualPoint> virtualPoints
    ) throws InvalidSchedule {
        var endLocation = RJSTrackLocationParser.parse(infra, rjsPhase.endLocation);
        var driverSightDistance = rjsPhase.driverSightDistance;

        if (Double.isNaN(driverSightDistance) || driverSightDistance < 0)
            throw new InvalidSchedule("invalid driver sight distance");

        if (rjsPhase.getClass() == RJSTrainPhase.Navigate.class) {
            return SignalNavigatePhase.from(driverSightDistance, startLocation, endLocation,
                    expectedPath, stops, virtualPoints);
        } else if (rjsPhase.getClass() == RJSTrainPhase.CBTC.class) {
            return CBTCNavigatePhase.from(driverSightDistance, startLocation, endLocation,
                    expectedPath, stops, virtualPoints);
        }
        throw new RuntimeException("unknown train phase");
    }

    private static TrainPath parsePath(Infra infra,
                                       RJSTrainPhase[] phases,
                                       ID<RJSRoute>[] rjsRoutes,
                                       TrackSectionLocation start) throws InvalidSchedule {
        var routes = new ArrayList<Route>();
        for (var rjsRoute : rjsRoutes) {
            var route = infra.routeGraph.routeMap.get(rjsRoute.id);
            if (route == null)
                throw new UnknownRoute("unknown route in train path", rjsRoute.id);
            routes.add(route);
        }

        var rjsEndLocation = phases[phases.length - 1].endLocation;
        return TrainPath.from(routes, start, RJSTrackLocationParser.parse(infra, rjsEndLocation));
    }

    private static List<VirtualPoint> parseVirtualPoints(
            List<RJSVirtualPoint> rjsVirtualPoints,
            Infra infra,
            TrainPath path
    ) throws InvalidSchedule {
        var res = new ArrayList<VirtualPoint>();
        if (rjsVirtualPoints == null)
            return res;

        for (var rjsVirtualPoint : rjsVirtualPoints) {
            var location = RJSTrackLocationParser.parse(infra, rjsVirtualPoint.location);
            // Skip the virtual point if not part of the path
            if (!path.containsTrackLocation(location))
                continue;
            var position = path.convertTrackLocation(location);
            res.add(new VirtualPoint(rjsVirtualPoint.name, position));
        }
        res.sort(Comparator.comparingDouble(stop -> stop.position));
        return res;
    }
}
