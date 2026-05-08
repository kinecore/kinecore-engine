package org.kinecore.math.interpolate;

import org.apache.commons.math3.analysis.interpolation.BicubicInterpolator;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

/**
 * A 2D spatiotemporal field for interpolating rates (e.g. fertility by age and time).
 * 
 * <p>Uses Bicubic Spline interpolation with fallback to bilinear for small grids.</p>
 */
public class SpatiotemporalField {
    private final double[] x;
    private final double[] y;
    private final BicubicInterpolator bicubicInterpolator;
    private org.apache.commons.math3.analysis.BivariateFunction bicubic;
    private final PolynomialSplineFunction[] linearRows;
    private final boolean useBicubic;

    /**
     * Constructs a field.
     * @param x x-axis grid
     * @param y y-axis grid
     * @param values 2D values [x][y]
     */
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
     * Interpolates the value at (xVal, yVal).
     * @param xVal coordinate 1
     * @param yVal coordinate 2
     * @return interpolated value
     */
    public double value(double xVal, double yVal) {
        if (useBicubic) {
            return bicubic.value(xVal, yVal);
        }
        // Find the two surrounding x indices
        int ix = searchIndex(x, xVal);
        int iy = searchIndex(y, yVal);
        double x0 = x[ix], x1 = x[ix + 1];
        double y0 = y[iy], y1 = y[iy + 1];
        double wx = (xVal - x0) / (x1 - x0);
        double wy = (yVal - y0) / (y1 - y0);
        
        double v00 = linearRows[ix].value(yVal);
        double v10 = linearRows[ix + 1].value(yVal);
        // Bilinear interpolation in x
        return v00 * (1 - wx) + v10 * wx;
    }

    private int searchIndex(double[] arr, double val) {
        int idx = java.util.Arrays.binarySearch(arr, val);
        if (idx < 0) idx = -idx - 2;
        if (idx < 0) idx = 0;
        if (idx >= arr.length - 1) idx = arr.length - 2;
        return idx;
    }
}
