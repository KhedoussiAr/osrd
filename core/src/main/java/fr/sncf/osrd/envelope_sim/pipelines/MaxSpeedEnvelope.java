package fr.sncf.osrd.envelope_sim.pipelines;

import fr.sncf.osrd.envelope.*;
import fr.sncf.osrd.envelope.constraint.ConstrainedEnvelopePartBuilder;
import fr.sncf.osrd.envelope.constraint.EnvelopeCeiling;
import fr.sncf.osrd.envelope.constraint.SpeedFloor;
import fr.sncf.osrd.envelope_sim.EnvelopeProfile;
import fr.sncf.osrd.envelope_sim.EnvelopeSimContext;
import fr.sncf.osrd.envelope_sim.StopMeta;
import fr.sncf.osrd.envelope_sim.overlays.EnvelopeDeceleration;

/** Max speed envelope = MRSP + braking curves
 * It is the max speed allowed at any given point, ignoring allowances
 */
public class MaxSpeedEnvelope {
    static boolean increase(double prevPos, double prevSpeed, double nextPos, double nextSpeed) {
        // Works for both accelerations (forwards) and decelerations (backwards)
        return prevSpeed < nextSpeed;
    }

    /** Generate braking curves overlay everywhere the mrsp decrease (increase backwards) with a discontinuity */
    private static Envelope addBrakingCurves(EnvelopeSimContext context, Envelope mrsp) {
        var builder = OverlayEnvelopeBuilder.backward(mrsp);
        var cursor = EnvelopeCursor.backward(mrsp);
        while (cursor.findPartTransition(MaxSpeedEnvelope::increase)) {
            var partBuilder = new EnvelopePartBuilder();
            partBuilder.setAttr(EnvelopeProfile.BRAKING);
            var overlayBuilder = new ConstrainedEnvelopePartBuilder(
                    partBuilder,
                    new SpeedFloor(0),
                    new EnvelopeCeiling(mrsp)
            );
            var startSpeed = cursor.getSpeed();
            var startPosition = cursor.getPosition();
            // TODO: link directionSign to cursor boolean reverse
            EnvelopeDeceleration.decelerate(context, startPosition, startSpeed, overlayBuilder, -1);
            builder.addPart(partBuilder.build());
            cursor.nextPart();
        }
        return builder.build();
    }

    /** Generate braking curves overlay at every stop position */
    private static Envelope addStopBrakingCurves(
            EnvelopeSimContext context,
            double[] stopPositions,
            Envelope curveWithDecelerations
    ) {
        for (int i = 0; i < stopPositions.length; i++) {
            var stopPosition = stopPositions[i];
            // if the stopPosition is zero, no need to build a deceleration curve
            if (stopPosition == 0.0)
                continue;
            if (stopPosition > context.path.getLength())
                throw new RuntimeException(String.format(
                        "Stop at index %d is out of bounds (position = %f, path length = %f)",
                        i, stopPosition, context.path.getLength()
                ));
            var partBuilder = new EnvelopePartBuilder();
            partBuilder.setAttr(EnvelopeProfile.BRAKING);
            partBuilder.setAttr(new StopMeta(i));
            var overlayBuilder = new ConstrainedEnvelopePartBuilder(
                    partBuilder,
                    new SpeedFloor(0),
                    new EnvelopeCeiling(curveWithDecelerations)
            );
            EnvelopeDeceleration.decelerate(context, stopPosition, 0, overlayBuilder, -1);

            var builder = OverlayEnvelopeBuilder.backward(curveWithDecelerations);
            builder.addPart(partBuilder.build());
            curveWithDecelerations = builder.build();
        }
        return curveWithDecelerations;
    }

    /** Generate a max speed envelope given a mrsp */
    public static Envelope from(EnvelopeSimContext context, double[] stopPositions, Envelope mrsp) {
        var maxSpeedEnvelope = addBrakingCurves(context, mrsp);
        maxSpeedEnvelope = addStopBrakingCurves(context, stopPositions, maxSpeedEnvelope);
        return maxSpeedEnvelope;
    }
}
