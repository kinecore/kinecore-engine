package org.kinecore.math.interpolate;

import org.apache.commons.math3.analysis.interpolation.BicubicInterpolator;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

/**
 * A 2D spatiotemporal field for interpolating rates (e.g. fertility by age and time).
 * 
 * <p>Implements Constant Extrapolation (Upgrade 1) to protect the engine from 
 * crashing if the simulation time or age exceeds the range of the input data.</p>
 */
public class SpatiotemporalField {
    private final double[] x;
    private final double[] y;
    private final BicubicInterpolator bicubicInterpolator;
    private org.apache.commons.math3.analysis.BivariateFunction bicubic;
    private final PolynomialSplineFunction[] linearRows;
    private final boolean useBicubic;

    public SpatiotemporalField(double[] x, double[] y, double[][] values) {
        this.x = x.clone();
        this.y = y.clone();
        if (x.length >= 4 && y.length >= 4) {
            bicubicInterpolator = new BicubicInterpolator();
            bicubic = bicubicInterpolator.interpolate(x, y, values);
            linearRows = null;
            useBicubic = true;
        } else {
            bicubicInterpolator = null;
            bicubic = null;
            linearRows = new PolynomialSplineFunction[x.length];
            LinearInterpolator li = new LinearInterpolator();
            for (int i = 0; i < x.length; i++) {
                linearRows[i] = li.interpolate(y, values[i]);
            }
            useBicubic = false;
        }
    }

    /**
     * Interpolates the value at (xVal, yVal) with constant extrapolation.
     * @param xVal coordinate 1 (typically time)
     * @param yVal coordinate 2 (typically age)
     * @return interpolated (or extrapolated) value
     */
    public double value(double xVal, double yVal) {
        // Step 1: Constant Extrapolation (The Shield)
        double safeX = clamp(xVal, x[0], x[x.length - 1]);
        double safeY = clamp(yVal, y[0], y[y.length - 1]);

        if (useBicubic) {
            return bicubic.value(safeX, safeY);
        }
        
        int ix = searchIndex(x, safeX);
        double wx = (x.length > 1) ? (safeX - x[ix]) / (x[ix + 1] - x[ix]) : 0;
        
        double v0 = linearRows[ix].value(safeY);
        if (ix + 1 < x.length) {
            double v1 = linearRows[ix + 1].value(safeY);
            return v0 * (1 - wx) + v1 * wx;
        }
        return v0;
    }

    private double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    private int searchIndex(double[] arr, double val) {
        int idx = java.util.Arrays.binarySearch(arr, val);
        if (idx < 0) idx = -idx - 2;
        if (idx < 0) idx = 0;
        if (idx >= arr.length - 1) idx = (arr.length > 1) ? arr.length - 2 : 0;
        return idx;
    }
}
