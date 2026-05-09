package org.kinecore.solver;

import com.tdunning.math.stats.TDigest;
import org.apache.commons.math3.ode.sampling.StepHandler;
import org.apache.commons.math3.ode.sampling.StepInterpolator;
import org.kinecore.engine.CompartmentalNetwork;
import org.kinecore.engine.ModelDefinition;
import org.kinecore.solver.impl.DormandPrince853Solver;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Orchestrates a parallel Monte Carlo simulation.
 */
public class MonteCarloEnsemble {

    private final ModelDefinition modelDefinition;
    private final MonteCarloComponents.ParameterSampler parameterSampler;
    private final ODESolver solver;
    private final double startTime;
    private final double endTime;
    private final double stepSize;
    private final int iterations;
    private final Map<String, Function<double[], Double>> outputAggregators;
    private final Consumer<double[]> discreteShiftHandler;

    /**
     * Immutable result of an ensemble run.
     */
    public static class SimulationResult {
        /**
         * Represents a single data point in time with quantiles.
         */
        public static class Point {
            /** Time of the point */
            public final double time;
            /** 5th percentile value */
            public final double p5;
            /** Median (50th percentile) value */
            public final double p50;
            /** 95th percentile value */
            public final double p95;

            /**
             * Constructs a point.
             * @param time time
             * @param p5 5th percentile
             * @param p50 50th percentile
             * @param p95 95th percentile
             */
            public Point(double time, double p5, double p50, double p95) {
                this.time = time; this.p5 = p5; this.p50 = p50; this.p95 = p95;
            }
        }
        
        private final Map<String, List<Point>> outputs;
        private final List<Map<String, Double>> sampledParameters;
        private final List<Map<String, Double>> finalStateSummaries;

        /**
         * Constructs a simulation result.
         * @param outputs output points
         * @param sampledParameters parameters sampled per iteration
         * @param finalStateSummaries summaries of final states per iteration
         */
        public SimulationResult(Map<String, List<Point>> outputs, 
                                List<Map<String, Double>> sampledParameters,
                                List<Map<String, Double>> finalStateSummaries) { 
            this.outputs = Collections.unmodifiableMap(outputs); 
            this.sampledParameters = Collections.unmodifiableList(sampledParameters);
            this.finalStateSummaries = Collections.unmodifiableList(finalStateSummaries);
        }

        /**
         * Gets all outputs.
         * @return map of outputs
         */
        public Map<String, List<Point>> getOutputs() { return outputs; }
        
        /**
         * Gets points for a specific output.
         * @param outputName name of the output
         * @return list of points
         */
        public List<Point> getPoints(String outputName) { return outputs.get(outputName); }
        
        /**
         * Gets all sampled parameters.
         * @return list of parameter maps
         */
        public List<Map<String, Double>> getSampledParameters() { return sampledParameters; }
        
        /**
         * Gets all final state summaries.
         * @return list of summary maps
         */
        public List<Map<String, Double>> getFinalStateSummaries() { return finalStateSummaries; }

        /**
         * Exports the simulation results to a CSV file (Task 4).
         * @param path target file path
         * @throws IOException if writing fails
         */
        public void toCSV(String path) throws IOException {
            try (PrintWriter writer = new PrintWriter(path)) {
                // Determine headers
                List<String> outputNames = new ArrayList<>(outputs.keySet());
                writer.print("Time");
                for (String name : outputNames) {
                    writer.print("," + name + "_p5," + name + "_p50," + name + "_p95");
                }
                writer.println();

                // Sort time points
                List<Double> times = outputs.get(outputNames.get(0)).stream()
                        .map(p -> p.time).sorted().toList();

                for (int i = 0; i < times.size(); i++) {
                    writer.print(times.get(i));
                    for (String name : outputNames) {
                        Point p = outputs.get(name).get(i);
                        writer.print("," + p.p5 + "," + p.p50 + "," + p.p95);
                    }
                    writer.println();
                }
            }
        }
    }

    private MonteCarloEnsemble(Builder b) {
        this.modelDefinition   = b.modelDefinition;
        this.parameterSampler  = b.sampler;
        this.solver            = b.solver;
        this.startTime         = b.startTime;
        this.endTime           = b.endTime;
        this.stepSize          = b.stepSize;
        this.iterations        = b.iterations;
        this.outputAggregators = Collections.unmodifiableMap(new HashMap<>(b.outputAggregators));
        this.discreteShiftHandler = b.discreteShiftHandler;
    }

    /**
     * Runs the ensemble simulation.
     * @return simulation result
     */
    public SimulationResult run() {
        Map<String, ConcurrentSkipListMap<Double, TDigest>> digests = new ConcurrentHashMap<>();
        for (String outName : outputAggregators.keySet()) {
            digests.put(outName, new ConcurrentSkipListMap<>());
        }

        // Parallel lists to store raw iteration data for sensitivity analysis (Task 2)
        Map<String, Double>[] sampledParams = new Map[iterations];
        Map<String, Double>[] finalSummaries = new Map[iterations];

        IntStream.range(0, iterations).parallel().forEach(iter -> {
            Map<String, Double> sampled = parameterSampler.sample(iter + 99991L);
            sampledParams[iter] = sampled;
            
            CompartmentalNetwork network = modelDefinition.bind(sampled);
            Map<String, double[]> traj = solveSingle(network);
            
            // Record final state summary
            Map<String, Double> summary = new HashMap<>();
            for (var entry : traj.entrySet()) {
                double[] values = entry.getValue();
                summary.put(entry.getKey(), values[values.length - 1]);
            }
            finalSummaries[iter] = summary;

            int nSteps = traj.values().iterator().next().length;
            for (int step = 0; step < nSteps; step++) {
                double t = startTime + step * stepSize;
                for (Map.Entry<String, double[]> e : traj.entrySet()) {
                    TDigest d = digests.get(e.getKey()).computeIfAbsent(t, k -> TDigest.createDigest(100.0));
                    synchronized (d) { d.add(e.getValue()[step]); }
                }
            }
        });

        Map<String, List<SimulationResult.Point>> finalOutputs = new HashMap<>();
        for (var entry : digests.entrySet()) {
            List<SimulationResult.Point> points = new ArrayList<>();
            for (var te : entry.getValue().entrySet()) {
                TDigest d = te.getValue();
                points.add(new SimulationResult.Point(te.getKey(), d.quantile(0.05), d.quantile(0.50), d.quantile(0.95)));
            }
            points.sort(Comparator.comparingDouble(p -> p.time));
            finalOutputs.put(entry.getKey(), points);
        }
        
        return new SimulationResult(finalOutputs, Arrays.asList(sampledParams), Arrays.asList(finalSummaries));
    }

    private Map<String, double[]> solveSingle(CompartmentalNetwork network) {
        int nSteps = (int) Math.round((endTime - startTime) / stepSize) + 1;
        Map<String, double[]> results = new HashMap<>();
        for (String name : outputAggregators.keySet()) results.put(name, new double[nSteps]);
        
        double[] y = network.buildInitialState();

        boolean hasDiscreteLogic = discreteShiftHandler != null || !network.getAdvectionChains().isEmpty();

        if (hasDiscreteLogic) {
            // Record initial state
            for (var e : outputAggregators.entrySet()) {
                results.get(e.getKey())[0] = e.getValue().apply(y);
            }
            
            for (int step = 1; step < nSteps; step++) {
                double tStart = startTime + (step - 1) * stepSize;
                double tEnd = startTime + step * stepSize;
                
                try {
                    solver.integrate(network, tStart, tEnd, y, null);
                } catch (Exception e) {
                    // handle error
                }
                
                // Legacy support for user-defined handlers
                if (discreteShiftHandler != null) {
                    discreteShiftHandler.accept(y);
                }
                
                // Native KineCore Advection processing (Upgrade 1)
                for (CompartmentalNetwork.AdvectionChain chain : network.getAdvectionChains()) {
                    double rem = tEnd % chain.shiftFrequency;
                    if (rem < 1e-9 || (chain.shiftFrequency - rem) < 1e-9) {
                        int[] idx = chain.indices;
                        if (chain.accumulateTerminal) {
                            y[idx[idx.length - 1]] += y[idx[idx.length - 2]];
                        }
                        for (int i = idx.length - 1; i > 0; i--) {
                            y[idx[i]] = y[idx[i - 1]];
                        }
                        y[idx[0]] = 0.0;
                    }
                }
                
                for (var e : outputAggregators.entrySet()) {
                    results.get(e.getKey())[step] = e.getValue().apply(y);
                }
            }
        } else {
            StepHandler handler = new StepHandler() {
                double nextT = startTime;
                int idx = 0;
                @Override public void init(double t0, double[] y0, double t) {
                    if (idx == 0 && nextT == startTime) {
                        for (var e : outputAggregators.entrySet()) {
                            results.get(e.getKey())[idx] = e.getValue().apply(y0);
                        }
                        nextT += stepSize;
                        idx++;
                    }
                }
                @Override public void handleStep(StepInterpolator interp, boolean isLast) {
                    double stepEnd = interp.getCurrentTime();
                    while (nextT <= stepEnd + 1e-12 && idx < nSteps) {
                        interp.setInterpolatedTime(nextT);
                        double[] state = interp.getInterpolatedState();
                        
                        for (var e : outputAggregators.entrySet()) {
                            results.get(e.getKey())[idx] = e.getValue().apply(state);
                        }
                        nextT += stepSize;
                        idx++;
                    }
                }
            };

            try {
                solver.integrate(network, startTime, endTime, y, handler);
            } catch (Exception e) {
                // handle error
            }
        }
        return results;
    }

    /**
     * Builder for MonteCarloEnsemble.
     */
    public static class Builder {
        private ModelDefinition modelDefinition;
        private MonteCarloComponents.ParameterSampler sampler;
        private ODESolver solver = new DormandPrince853Solver(1e-6, 1.0, 1e-8, 1e-8);
        private double startTime = 0, endTime = 100, stepSize = 1;
        private int iterations = 1000;
        private final Map<String, Function<double[], Double>> outputAggregators = new HashMap<>();
        private Consumer<double[]> discreteShiftHandler = null;

        /** Constructor for the builder. */
        public Builder() {}

        /** 
         * Sets the model definition.
         * @param model model definition 
         * @return this builder
         */
        public Builder model(ModelDefinition model) { this.modelDefinition = model; return this; }
        
        /** 
         * Sets the parameter sampler.
         * @param sampler parameter sampler 
         * @return this builder
         */
        public Builder sampler(MonteCarloComponents.ParameterSampler sampler) { this.sampler = sampler; return this; }
        
        /** 
         * Sets the ODE solver.
         * @param solver ODE solver 
         * @return this builder
         */
        public Builder solver(ODESolver solver) { this.solver = solver; return this; }
        
        /** 
         * Sets the simulation time range.
         * @param start start time 
         * @param end end time 
         * @return this builder
         */
        public Builder timeRange(double start, double end) { this.startTime = start; this.endTime = end; return this; }
        
        /** 
         * Sets the output recording step size.
         * @param step step size 
         * @return this builder
         */
        public Builder stepSize(double step) { this.stepSize = step; return this; }
        
        /** 
         * Sets the number of iterations.
         * @param iters number of iterations 
         * @return this builder
         */
        public Builder iterations(int iters) { this.iterations = iters; return this; }
        
        /** 
         * Adds an output variable to track.
         * @param name name of output 
         * @param agg aggregator function 
         * @return this builder
         */
        public Builder addOutput(String name, Function<double[], Double> agg) {
            this.outputAggregators.put(name, agg); return this;
        }

        /** 
         * Sets a discrete shift handler to run at each step interval.
         * @param handler discrete shift handler
         * @return this builder
         */
        public Builder discreteShift(Consumer<double[]> handler) {
            this.discreteShiftHandler = handler; return this;
        }

        /** 
         * Finalizes and builds the ensemble.
         * @return the built ensemble 
         */
        public MonteCarloEnsemble build() {
            if (modelDefinition == null || sampler == null) throw new IllegalStateException("Model and sampler must be set.");
            if (outputAggregators.isEmpty()) outputAggregators.put("Total_Mass", state -> Arrays.stream(state).sum());
            return new MonteCarloEnsemble(this);
        }
    }
}
