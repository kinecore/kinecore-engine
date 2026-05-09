package org.kinecore.engine;

import java.util.Map;

/**
 * Factory for creating standardized, mathematically safe FeedbackOperators.
 */
public class FeedbackFactory {

    public static LogisticCurveBuilder logisticCurve() {
        return new LogisticCurveBuilder();
    }

    public static class LogisticCurveBuilder {
        private double baseValue = 1.0;
        private double maxValue = 5.0;
        private double midpoint = 0.0;
        private double growthRate = 1.0;
        private FeedbackOperator stressExtractor = (t, flow, state, params) -> flow;

        public LogisticCurveBuilder withBaseValue(double baseValue) {
            this.baseValue = baseValue;
            return this;
        }

        public LogisticCurveBuilder withMaxValue(double maxValue) {
            this.maxValue = maxValue;
            return this;
        }

        public LogisticCurveBuilder withMidpoint(double midpoint) {
            this.midpoint = midpoint;
            return this;
        }

        public LogisticCurveBuilder withGrowthRate(double growthRate) {
            this.growthRate = growthRate;
            return this;
        }

        public LogisticCurveBuilder withStressExtractor(FeedbackOperator extractor) {
            this.stressExtractor = extractor;
            return this;
        }

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
        
        public double evaluate(double stress) {
            if (stress <= 0.0) {
                return baseValue;
            }
            return baseValue + (maxValue - baseValue) / (1.0 + Math.exp(-growthRate * (stress - midpoint)));
        }
    }
}
