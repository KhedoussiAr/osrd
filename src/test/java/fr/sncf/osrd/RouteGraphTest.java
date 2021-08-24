package fr.sncf.osrd;

import static fr.sncf.osrd.Helpers.*;
import static fr.sncf.osrd.infra.trackgraph.TrackSection.linkEdges;
import static org.junit.jupiter.api.Assertions.assertEquals;

import fr.sncf.osrd.infra.InvalidInfraException;
import fr.sncf.osrd.infra.TVDSection;
import fr.sncf.osrd.infra.routegraph.Route;
import fr.sncf.osrd.infra.routegraph.RouteGraph;
import fr.sncf.osrd.infra.trackgraph.*;
import fr.sncf.osrd.utils.SortedArraySet;
import fr.sncf.osrd.utils.graph.EdgeDirection;
import fr.sncf.osrd.utils.graph.EdgeEndpoint;
import org.junit.jupiter.api.Test;

import java.util.*;

public class RouteGraphTest {

    private static void checkRoute(
            Route route,
            int expectedTvdSectionPath,
            double expectedLength,
            Waypoint expectedStart,
            Waypoint expectedEnd
    ) {
        assertEquals(expectedLength, route.length, 0.1);
        assertEquals(expectedTvdSectionPath, route.tvdSectionsPaths.size());

        var start = route.tvdSectionsPaths.get(0).startWaypoint;
        assertEquals(expectedStart.index, start.index);

        var lastIndex = expectedTvdSectionPath - 1;
        var end = route.tvdSectionsPaths.get(lastIndex).endWaypoint;
        assertEquals(expectedEnd.index, end.index);
    }

    private static Route makeRoute(
            RouteGraph.Builder builder,
            String id,
            ArrayList<Waypoint> waypoints,
            EdgeDirection entryDirection,
            SortedArraySet<TVDSection> tvdSections,
            HashMap<Switch, SwitchPosition> switchPositions
    ) throws InvalidInfraException {
        // Create a "flexible transit" release group
        var releaseGroups = new ArrayList<SortedArraySet<TVDSection>>();
        for (var tvdSection : tvdSections) {
            var releaseGroup = new SortedArraySet<TVDSection>();
            releaseGroup.add(tvdSection);
            releaseGroups.add(releaseGroup);
        }

        return builder.makeRoute(id, tvdSections, releaseGroups, switchPositions, waypoints.get(0),
                waypoints.get(waypoints.size() - 1), null, entryDirection);
    }

    private static Route makeRoute(
            RouteGraph.Builder builder,
            String id,
            ArrayList<Waypoint> waypoints,
            EdgeDirection entryDirection,
            SortedArraySet<TVDSection> tvdSections
    ) throws InvalidInfraException {
        return makeRoute(builder, id, waypoints, entryDirection, tvdSections, null);
    }

    /**
     * One tiv with 2 routes
     *         R1
     * A   ----------->   B
     * |---D1---D2---D3---|
     *     <-----------
     *         R2
     */
    @Test
    public void simpleRouteGraphBuild() throws InvalidInfraException {
        // Craft trackGraph
        var trackGraph = new TrackGraph();
        var nodeA = trackGraph.makePlaceholderNode("A");
        var nodeB = trackGraph.makePlaceholderNode("B");
        var trackSection = trackGraph.makeTrackSection(nodeA.index, nodeB.index, "e1", 100, null);
        var detectorBuilder = trackSection.waypoints.builder();
        var d1 = new Detector(0, "D1");
        var d2 = new Detector(1, "D2");
        var d3 = new Detector(2, "D3");
        detectorBuilder.add(40, d1);
        detectorBuilder.add(50, d2);
        detectorBuilder.add(75, d3);
        detectorBuilder.build();

        // Craft tvdSections
        var tvd12 = makeTVDSection(d1, d2);
        assignAfterTVDSection(tvd12, d1);
        assignBeforeTVDSection(tvd12, d2);
        var tvd23 = makeTVDSection(d2, d3);
        assignAfterTVDSection(tvd23, d2);
        assignBeforeTVDSection(tvd23, d3);

        var tvdSections = new SortedArraySet<TVDSection>();
        tvdSections.add(tvd12);
        tvdSections.add(tvd23);

        // Build RouteGraph
        var routeGraphBuilder = new RouteGraph.Builder(trackGraph, 3);

        var waypointsR1 = new ArrayList<Waypoint>(Arrays.asList(d1, d2, d3));
        final var route1 = makeRoute(routeGraphBuilder, "R1", waypointsR1,
                EdgeDirection.START_TO_STOP, tvdSections);

        var waypointsR2 = new ArrayList<Waypoint>(Arrays.asList(d3, d2, d1));
        final var route2 = makeRoute(routeGraphBuilder, "R2", waypointsR2,
                EdgeDirection.STOP_TO_START, tvdSections);

        var routeGraph =  routeGraphBuilder.build();

        assertEquals(2, routeGraph.getEdgeCount());

        checkRoute(route1, 2, 35, d1, d3);
        checkRoute(route2, 2, 35, d3, d1);
    }

    /**
     * Complex track graph. Points A, B and D are buffer stops.
     *                R1
     *    —————————————————————,
     *   A   foo_a        D1    \               R4
     *   +—————————————————o——,  +—> <————————————————————————————————
     *                    D2   \   D3                      D4
     *   +—————————————————o————+——o————————————————————————o————————+
     *   B    foo_b             C           track                    D
     *    —————————————————————————> ————————————————————————————————>
     *              R2                             R3
     */
    @Test
    public void complexRouteGraphBuild() throws InvalidInfraException {
        // Craft trackGraph
        var trackGraph = new TrackGraph();
        var nodeA = trackGraph.makePlaceholderNode("A");
        var nodeB = trackGraph.makePlaceholderNode("B");
        var nodeC = trackGraph.makePlaceholderNode("C");
        var nodeD = trackGraph.makePlaceholderNode("D");
        var fooA = trackGraph.makeTrackSection(nodeA.index, nodeC.index, "foo_a", 75, null);
        var fooB = trackGraph.makeTrackSection(nodeB.index, nodeC.index, "foo_b", 75, null);
        var track = trackGraph.makeTrackSection(nodeC.index, nodeD.index, "track", 100, null);

        linkEdges(fooA, EdgeEndpoint.END, track, EdgeEndpoint.BEGIN);
        linkEdges(fooB, EdgeEndpoint.END, track, EdgeEndpoint.BEGIN);

        var index = 0;

        var middleSwitch = trackGraph.makeSwitchNode(nodeC.index, "switch", 0, 0);
        middleSwitch.leftTrackSection = fooA;
        middleSwitch.rightTrackSection = fooB;

        final var bsA = new BufferStop(index++, "BS_A");
        final var d1 = new Detector(index++, "D1");
        var detectorBuilder = fooA.waypoints.builder();
        detectorBuilder.add(0, bsA);
        detectorBuilder.add(40, d1);
        detectorBuilder.build();

        final var bsB = new BufferStop(index++, "BS_B");
        final var d2 = new Detector(index++, "D2");
        detectorBuilder = fooB.waypoints.builder();
        detectorBuilder.add(0, bsB);
        detectorBuilder.add(40, d2);
        detectorBuilder.build();

        final var d3 = new Detector(index++, "D3");
        final var d4 = new Detector(index++, "D4");
        final var bsD = new BufferStop(index++, "BS_D");
        detectorBuilder = track.waypoints.builder();
        detectorBuilder.add(25, d3);
        detectorBuilder.add(75, d4);
        detectorBuilder.add(100, bsD);
        detectorBuilder.build();

        // Craft tvdSections
        final var tvdSection123 = makeTVDSection(d1, d2, d3);
        assignAfterTVDSection(tvdSection123, d1, d2);
        assignBeforeTVDSection(tvdSection123, d3);
        final var tvdSection1A = makeTVDSection(d1, bsA);
        assignAfterTVDSection(tvdSection1A, bsA);
        assignBeforeTVDSection(tvdSection1A, d1);
        final var tvdSection2B = makeTVDSection(d2, bsB);
        assignAfterTVDSection(tvdSection2B, bsB);
        assignBeforeTVDSection(tvdSection2B, d2);
        final var tvdSection34 = makeTVDSection(d3, d4);
        assignAfterTVDSection(tvdSection34, d3);
        assignBeforeTVDSection(tvdSection34, d4);
        final var tvdSection4D = makeTVDSection(d4, bsD);
        assignAfterTVDSection(tvdSection4D, d4);
        assignBeforeTVDSection(tvdSection4D, bsD);

        var switchPositionLeft = new HashMap<Switch, SwitchPosition>();
        var switchPositionRight = new HashMap<Switch, SwitchPosition>();
        switchPositionLeft.put(middleSwitch, SwitchPosition.LEFT);
        switchPositionRight.put(middleSwitch, SwitchPosition.RIGHT);

        // Build RouteGraph
        var routeGraphBuilder = new RouteGraph.Builder(trackGraph, index);

        var waypointsR1 = new ArrayList<>(Arrays.asList(bsA, d1, d3));
        var tvdSectionsR1 = new SortedArraySet<TVDSection>();
        tvdSectionsR1.add(tvdSection123);
        tvdSectionsR1.add(tvdSection1A);
        final var route1 = makeRoute(routeGraphBuilder, "R1", waypointsR1,
                EdgeDirection.START_TO_STOP, tvdSectionsR1, switchPositionLeft);

        var waypointsR2 = new ArrayList<>(Arrays.asList(bsB, d2, d3));
        var tvdSectionsR2 = new SortedArraySet<TVDSection>();
        tvdSectionsR2.add(tvdSection123);
        tvdSectionsR2.add(tvdSection2B);
        final var route2 = makeRoute(routeGraphBuilder, "R2", waypointsR2,
                EdgeDirection.START_TO_STOP, tvdSectionsR2, switchPositionRight);

        var waypointsR3 = new ArrayList<>(Arrays.asList(d3, d4, bsD));
        var tvdSectionsR3 = new SortedArraySet<TVDSection>();
        tvdSectionsR3.add(tvdSection34);
        tvdSectionsR3.add(tvdSection4D);
        final var route3 = makeRoute(routeGraphBuilder, "R3", waypointsR3,
                EdgeDirection.START_TO_STOP, tvdSectionsR3);

        var waypointsR4 = new ArrayList<>(Arrays.asList(bsD, d4, d3));
        var tvdSectionsR4 = new SortedArraySet<TVDSection>();
        tvdSectionsR4.add(tvdSection34);
        tvdSectionsR4.add(tvdSection4D);
        final var route4 = makeRoute(routeGraphBuilder, "R4", waypointsR4,
                EdgeDirection.STOP_TO_START, tvdSectionsR4);

        var routeGraph =  routeGraphBuilder.build();

        assertEquals(4, routeGraph.getEdgeCount());

        checkRoute(route1, 2, 100, bsA, d3);
        checkRoute(route2, 2, 100, bsB, d3);
        checkRoute(route3, 2, 75, d3, bsD);
        checkRoute(route4, 2, 75, bsD, d3);
    }

    /**
     *                      +  outerA
     *                      |
     *                   DA o  |
     *                      |  v
     *                      |
     *               innerA +
     *                     / \
     *                 ^  /   \  |
     *                 | /     \ v
     *           ->     /       \     <-
     * outerC +-o------+---------+---------o-+ outerB
     *          DC   innerC <- innerB      DB
     */
    @Test
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void triangularTVDSection() throws InvalidInfraException {
        // Craft trackGraph
        var trackGraph = new TrackGraph();
        var nodeInnerA = trackGraph.makePlaceholderNode("innerA");
        var nodeInnerB = trackGraph.makePlaceholderNode("innerB");
        var nodeInnerC = trackGraph.makePlaceholderNode("innerC");
        var nodeOuterA = trackGraph.makePlaceholderNode("outerA");
        var nodeOuterB = trackGraph.makePlaceholderNode("outerB");
        var nodeOuterC = trackGraph.makePlaceholderNode("outerC");
        var trackSectionA = trackGraph.makeTrackSection(nodeOuterA.index, nodeInnerA.index, "eA", 100, null);
        var trackSectionB = trackGraph.makeTrackSection(nodeOuterB.index, nodeInnerB.index, "eB", 100, null);
        var trackSectionC = trackGraph.makeTrackSection(nodeOuterC.index, nodeInnerC.index, "eC", 100, null);
        var trackSectionAB = trackGraph.makeTrackSection(nodeInnerA.index, nodeInnerB.index, "AB", 100, null);
        var trackSectionBC = trackGraph.makeTrackSection(nodeInnerB.index, nodeInnerC.index, "BC", 100, null);
        var trackSectionAC = trackGraph.makeTrackSection(nodeInnerC.index, nodeInnerA.index, "CA", 100, null);

        var detectorBuilderA = trackSectionA.waypoints.builder();
        var da = new Detector(0, "DA");
        detectorBuilderA.add(50, da);
        detectorBuilderA.build();

        var detectorBuilderB = trackSectionB.waypoints.builder();
        var db = new Detector(1, "DB");
        detectorBuilderB.add(50, db);
        detectorBuilderB.build();

        var detectorBuilderC = trackSectionC.waypoints.builder();
        var dc = new Detector(2, "DC");
        detectorBuilderC.add(50, dc);
        detectorBuilderC.build();

        linkEdges(trackSectionA, EdgeEndpoint.END, trackSectionAB, EdgeEndpoint.BEGIN);
        linkEdges(trackSectionA, EdgeEndpoint.END, trackSectionAC, EdgeEndpoint.END);
        linkEdges(trackSectionB, EdgeEndpoint.END, trackSectionBC, EdgeEndpoint.BEGIN);
        linkEdges(trackSectionB, EdgeEndpoint.END, trackSectionAB, EdgeEndpoint.END);
        linkEdges(trackSectionC, EdgeEndpoint.END, trackSectionAC, EdgeEndpoint.BEGIN);
        linkEdges(trackSectionC, EdgeEndpoint.END, trackSectionBC, EdgeEndpoint.END);

        var switchA = trackGraph.makeSwitchNode(nodeInnerA.index, "switchA", 0, 0);
        var switchB = trackGraph.makeSwitchNode(nodeInnerB.index, "switchB", 1, 0);
        var switchC = trackGraph.makeSwitchNode(nodeInnerC.index, "switchC", 2, 0);
        switchA.leftTrackSection = trackSectionAB;
        switchA.rightTrackSection = trackSectionAC;
        switchB.leftTrackSection = trackSectionBC;
        switchB.rightTrackSection = trackSectionAB;
        switchC.leftTrackSection = trackSectionAC;
        switchC.rightTrackSection = trackSectionBC;

        // Craft tvdSections
        var tvdSectionsSet = new SortedArraySet<TVDSection>();
        var tvdSection123 = makeTVDSection(da, db, dc);
        tvdSectionsSet.add(tvdSection123);
        assignAfterTVDSection(tvdSection123, da, db, dc);
        var releaseGroups = Collections.singletonList(tvdSectionsSet);

        // Build RouteGraph
        var routeGraphBuilder = new RouteGraph.Builder(trackGraph, 3);

        var switchPositionsR1 = new HashMap<Switch, SwitchPosition>();
        switchPositionsR1.put(switchA, SwitchPosition.LEFT);
        switchPositionsR1.put(switchB, SwitchPosition.RIGHT);
        routeGraphBuilder.makeRoute("R1", tvdSectionsSet, releaseGroups, switchPositionsR1, da, db,
                null, EdgeDirection.START_TO_STOP);

        var switchPositionsR2 = new HashMap<Switch, SwitchPosition>();
        switchPositionsR2.put(switchB, SwitchPosition.LEFT);
        switchPositionsR2.put(switchC, SwitchPosition.RIGHT);
        routeGraphBuilder.makeRoute("R2", tvdSectionsSet, releaseGroups, switchPositionsR2, db, dc,
                null, EdgeDirection.START_TO_STOP);

        var switchPositionsR3 = new HashMap<Switch, SwitchPosition>();
        switchPositionsR3.put(switchC, SwitchPosition.LEFT);
        switchPositionsR3.put(switchA, SwitchPosition.RIGHT);
        routeGraphBuilder.makeRoute("R3", tvdSectionsSet, releaseGroups, switchPositionsR3, dc, da,
                null, EdgeDirection.START_TO_STOP);
    }
}
