package org.kinecore.util;

/**
 * Global numerical constants for the KineCore engine.
 */
public class MathUtils {

    /** Default numerical grain for boundary logic (soft stop) */
    public static final double NEAR_ZERO = 1e-6;

    /** High precision epsilon for hard clamps and equality checks */
    public static final double PRECISION = 1e-12;
    
    /** Default stiffness for soft boundary damping */
    public static final double DAMPING_EPSILON = 0.05;

    private MathUtils() {}
}
