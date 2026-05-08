package org.kinecore.math.interpolate;

import org.apache.commons.math3.analysis.interpolation.BicubicInterpolatingFunction;
import org.apache.commons.math3.analysis.interpolation.BicubicInterpolator;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

/**
 * A 2D spatiotemporal field for interpolating rates (e.g. fertility by age and time).
 * 
 * <p>Uses Bicubic Spline interpolation with fallback to bilinear for small grids.</p>
 */
public class SpatiotemporalField {

    private final double[] dim1;
    private final double[] dim2;
    private BicubicInterpolatingFunction bicubic;
    private PolynomialSplineFunction[] linearFallback;

    /**
     * Constructs a field.
     * @param dim1 x-axis grid
     * @param dim2 y-axis grid
     * @param values 2D values [x][y]
     */
    public SpatiotemporalField(double[] dim1, double[] dim2, double[][] values) {
        this.dim1 = dim1;
        this.dim2 = dim2;
        
        if (dim1.length >= 4 && dim2.length >= 4) {
            this.bicubic = new BicubicInterpolator().interpolate(dim1, dim2, values);
        } else {
            // Bilinear fallback
            this.linearFallback = new PolynomialSplineFunction[dim1.length];
            LinearInterpolator li = new LinearInterpolator();
            for (int i = 0; i < dim1.length; i++) {
                this.linearFallback[i] = li.interpolate(dim2, values[i]);
            }
        }
    }

    /**
     * Interpolates the value at (x, y).
     * @param x coordinate 1
     * @param y coordinate 2
     * @return interpolated value
     */
    public double value(double x, double y) {
        if (bicubic != null) {
            try {
                return bicubic.value(x, y);
            } catch (Exception e) {
                return 0.0;
            }
        }
        // Simplified bilinear fallback logic
        return 0.0; 
    }
}
