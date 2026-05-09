package org.kinecore.solver;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Calculates sensitivity of simulation outcomes to input parameters.
 * 
 * <p>Supports both linear (Pearson) and non-linear (Spearman Rank) correlation
 * analysis to identify critical model drivers.</p>
 */
public class SensitivityEngine {

    /**
     * Private constructor for utility class.
     */
    private SensitivityEngine() {}

    /**
     * Analyzes the sensitivity of a target output variable using Spearman Rank Correlation (Default).
     * 
     * <p>Spearman is the default as it robustly captures non-linear, monotonic
     * relationships common in demographic feedback loops.</p>
     */
    public static Map<String, Double> analyze(MonteCarloEnsemble.SimulationResult result, String targetOutputName) {
        return analyze(result, targetOutputName, true);
    }

    /**
     * Analyzes the sensitivity of a target output variable.
     * 
     * @param result the simulation result containing iteration-level data
     * @param targetOutputName the name of the output variable to analyze
     * @param useSpearman if true, use Spearman Rank correlation (non-linear); if false, Pearson (linear)
     * @return a map of parameter names (prefixed with 'p.') to their correlation coefficient
     */
    public static Map<String, Double> analyze(MonteCarloEnsemble.SimulationResult result, 
                                             String targetOutputName, 
                                             boolean useSpearman) {
        List<Map<String, Double>> paramsList = result.getSampledParameters();
        List<Map<String, Double>> outcomesList = result.getFinalStateSummaries();
        
        if (paramsList.isEmpty()) return Collections.emptyMap();
        
        Set<String> paramNames = paramsList.get(0).keySet();
        Map<String, Double> correlations = new HashMap<>();
        
        double[] outcomes = outcomesList.stream()
                .mapToDouble(m -> m.getOrDefault(targetOutputName, 0.0))
                .toArray();
        
        if (useSpearman) {
            outcomes = rankTransform(outcomes);
        }
        
        for (String paramName : paramNames) {
            double[] paramValues = paramsList.stream()
                    .mapToDouble(m -> m.getOrDefault(paramName, 0.0))
                    .toArray();
            
            if (useSpearman) {
                paramValues = rankTransform(paramValues);
            }
            
            // Prefix keys with 'p.' to avoid collision with outcomes in joint analysis maps
            correlations.put("p." + paramName, calculatePearsonCorrelation(paramValues, outcomes));
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

    /**
     * Converts raw values to fractional ranks (1-indexed).
     * Handles ties by averaging ranks (e.g. [10, 20, 20, 30] -> [1, 2.5, 2.5, 4]).
     */
    private static double[] rankTransform(double[] values) {
        int n = values.length;
        double[] ranks = new double[n];
        
        Integer[] indices = IntStream.range(0, n).boxed().toArray(Integer[]::new);
        Arrays.sort(indices, Comparator.comparingDouble(i -> values[i]));
        
        int i = 0;
        while (i < n) {
            int j = i + 1;
            while (j < n && values[indices[j]] == values[indices[i]]) {
                j++;
            }
            
            // Average rank for the tied group
            double avgRank = (i + 1 + j) / 2.0;
            for (int k = i; k < j; k++) {
                ranks[indices[k]] = avgRank;
            }
            i = j;
        }
        
        return ranks;
    }
}
