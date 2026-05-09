package org.kinecore.engine.signal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.kinecore.engine.ExogenousSignal;
import org.kinecore.math.interpolate.SpatiotemporalField;

/**
 * A serializable implementation of ExogenousSignal that uses a 2D spline
 * to interpolate values over time and age (Step 1).
 * 
 * <p>This allows the Next.js dashboard to upload CSV data which is converted
 * into a spatiotemporal grid and injected directly into the ODE feedback loops.</p>
 */
public class SplineSignal implements ExogenousSignal {

    private final double[] timeGrid;
    private final double[] ageGrid;
    private final double[][] values;
    private final transient SpatiotemporalField field;

    @JsonCreator
    public SplineSignal(@JsonProperty("timeGrid") double[] timeGrid,
                        @JsonProperty("ageGrid")  double[] ageGrid,
                        @JsonProperty("values")   double[][] values) {
        this.timeGrid = timeGrid;
        this.ageGrid = ageGrid;
        this.values = values;
        this.field = new SpatiotemporalField(timeGrid, ageGrid, values);
    }

    @Override
    public double getValueAt(double t, int ageIdx) {
        return field.value(t, (double) ageIdx);
    }

    @JsonProperty("timeGrid")
    public double[] getTimeGrid() { return timeGrid; }

    @JsonProperty("ageGrid")
    public double[] getAgeGrid() { return ageGrid; }

    @JsonProperty("values")
    public double[][] getValues() { return values; }
}
