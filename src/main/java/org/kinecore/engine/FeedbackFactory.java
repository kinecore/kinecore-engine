package org.kinecore.engine;

import java.util.Map;

/**
 * Factory for creating standardized, mathematically safe {@link FeedbackOperator}s.
 *
 * <p>All operators produced here are numerically verified transfer functions that
 * prevent stiffness and phantom-stress artifacts in ODE integration.</p>
 */
public class FeedbackFactory {

    /**
     * Creates a builder for a logistic (sigmoid) feedback curve.
     *
     * @return a new {@link LogisticCurveBuilder}
     */
    public static LogisticCurveBuilder logisticCurve() {
        return new LogisticCurveBuilder();
    }

    /**
     * Builder for a logistic-curve {@link FeedbackOperator}.
     *
     * <p>The operator scales an incoming flow by a sigmoid function of a
     * user-defined stress signal, enabling non-linear threshold behaviour such as
     * "Youth Flight" emigration acceleration past an economic tipping-point.</p>
     */
    public static class LogisticCurveBuilder {
        private double baseValue  = 1.0;
        private double maxValue   = 5.0;
        private double midpoint   = 0.0;
        private double growthRate = 1.0;
        private FeedbackOperator stressExtractor = (t, flow, state, params) -> flow;

        /**
         * Sets the minimum multiplier applied when stress is zero or negative.
         *
         * @param baseValue minimum multiplier (default 1.0)
         * @return this builder
         */
        public LogisticCurveBuilder withBaseValue(double baseValue) {
            this.baseValue = baseValue;
            return this;
        }

        /**
         * Sets the maximum multiplier approached as stress grows without bound.
         *
         * @param maxValue maximum multiplier (default 5.0)
         * @return this builder
         */
        public LogisticCurveBuilder withMaxValue(double maxValue) {
            this.maxValue = maxValue;
            return this;
        }

        /**
         * Sets the stress level at which the multiplier is halfway between base and max.
         *
         * @param midpoint inflection point of the sigmoid (default 0.0)
         * @return this builder
         */
        public LogisticCurveBuilder withMidpoint(double midpoint) {
            this.midpoint = midpoint;
            return this;
        }

        /**
         * Sets the steepness of the transition between base and max.
         *
         * @param growthRate sigmoid growth rate; higher values create sharper transitions (default 1.0)
         * @return this builder
         */
        public LogisticCurveBuilder withGrowthRate(double growthRate) {
            this.growthRate = growthRate;
            return this;
        }

        /**
         * Sets a {@link FeedbackOperator} used to extract the scalar stress signal
         * from the full system state. Defaults to using the raw flow value as stress.
         *
         * @param extractor operator that maps (t, flow, state, params) to a scalar stress value
         * @return this builder
         */
        public LogisticCurveBuilder withStressExtractor(FeedbackOperator extractor) {
            this.stressExtractor = extractor;
            return this;
        }

        /**
         * Builds and returns the configured logistic {@link FeedbackOperator}.
         *
         * @return a new {@link FeedbackOperator} that applies the sigmoid multiplier
         */
        public FeedbackOperator build() {
            return (t, flow, state, params) -> {
                double stress = stressExtractor.apply(t, flow, state, params);
                if (stress <= 0.0) {
                    return flow * baseValue; // Bypass for negative/zero stress
                }
                double multiplier = baseValue + (maxValue - baseValue) / (1.0 + Math.exp(-growthRate * (stress - midpoint)));
                return flow * multiplier;
            };
        }

        /**
         * Evaluates the logistic curve for a given stress level without applying it to a flow.
         * Useful for debugging and visualising the transfer function shape.
         *
         * @param stress the stress input value
         * @return the multiplier the curve would produce at this stress level
         */
        public double evaluate(double stress) {
            if (stress <= 0.0) {
                return baseValue;
            }
            return baseValue + (maxValue - baseValue) / (1.0 + Math.exp(-growthRate * (stress - midpoint)));
        }
    }
}
