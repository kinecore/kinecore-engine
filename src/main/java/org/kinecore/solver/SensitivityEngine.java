package org.kinecore.solver;

import java.util.*;

/**
 * Calculates sensitivity of simulation outcomes to input parameters (Task 2).
 * 
 * <p>Uses Pearson Correlation to determine which parameters have the strongest influence
 * on the final value of a given output metric.</p>
 */
public class SensitivityEngine {

    /**
     * Private constructor for utility class.
     */
    private SensitivityEngine() {}

    /**
     * Analyzes the sensitivity of a target output variable to all sampled parameters.
     * 
     * @param result the simulation result containing iteration-level data
     * @param targetOutputName the name of the output variable to analyze
     * @return a map of parameter names to their Pearson Correlation coefficient with the target
     */
    public static Map<String, Double> analyze(MonteCarloEnsemble.SimulationResult result, String targetOutputName) {
        List<Map<String, Double>> paramsList = result.getSampledParameters();
        List<Map<String, Double>> outcomesList = result.getFinalStateSummaries();
        
        if (paramsList.isEmpty()) return Collections.emptyMap();
        
        Set<String> paramNames = paramsList.get(0).keySet();
        Map<String, Double> correlations = new HashMap<>();
        
        double[] outcomes = outcomesList.stream()
                .mapToDouble(m -> m.getOrDefault(targetOutputName, 0.0))
                .toArray();
        
        for (String paramName : paramNames) {
            double[] paramValues = paramsList.stream()
                    .mapToDouble(m -> m.getOrDefault(paramName, 0.0))
                    .toArray();
            
            correlations.put(paramName, calculatePearsonCorrelation(paramValues, outcomes));
        }
        
        return correlations;
    }

    private static double calculatePearsonCorrelation(double[] x, double[] y) {
        if (x.length != y.length || x.length == 0) return 0.0;
        
        double sumX = 0, sumY = 0, sumXY = 0;
        double sumX2 = 0, sumY2 = 0;
        int n = x.length;
        
        for (int i = 0; i < n; i++) {
            sumX += x[i];
            sumY += y[i];
            sumXY += x[i] * y[i];
            sumX2 += x[i] * x[i];
            sumY2 += y[i] * y[i];
        }
        
        double numerator = n * sumXY - sumX * sumY;
        double denominator = Math.sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY));
        
        if (denominator == 0) return 0.0;
        return numerator / denominator;
    }
}
