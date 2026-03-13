package org.example.Model;

/**
 * Represents transitions of passengers between different airport service points.
 *
 * Each transition corresponds to movement between two stages in the airport
 * process. The traversal time is defined by a mean value and a standard
 * deviation that are retrieved from the simulation configuration.
 */
public enum Transition {

    /**
     * Transition from check-in to security control.
     */
    CHECKIN_TO_SECURITY {
        /**
         * Returns the mean traversal time for the transition.
         *
         * @param config simulation configuration containing traversal parameters
         * @return mean traversal time
         */
        public double getMean(Configuration config) {
            return config.getTraversalCheckinToSecurityMean();
        }

        /**
         * Returns the standard deviation of traversal time.
         *
         * @param config simulation configuration containing traversal parameters
         * @return traversal time standard deviation
         */
        public double getStdDev(Configuration config) {
            return config.getTraversalCheckinToSecurityStdDev();
        }
    },

    /**
     * Transition from security control to customs.
     */
    SECURITY_TO_CUSTOMS {
        /**
         * Returns the mean traversal time for the transition.
         *
         * @param config simulation configuration containing traversal parameters
         * @return mean traversal time
         */
        public double getMean(Configuration config) {
            return config.getTraversalSecurityToCustomsMean();
        }

        /**
         * Returns the standard deviation of traversal time.
         *
         * @param config simulation configuration containing traversal parameters
         * @return traversal time standard deviation
         */
        public double getStdDev(Configuration config) {
            return config.getTraversalSecurityToCustomsStdDev();
        }
    },

    /**
     * Transition from security control directly to boarding.
     */
    SECURITY_TO_BOARDING {
        /**
         * Returns the mean traversal time for the transition.
         *
         * @param config simulation configuration containing traversal parameters
         * @return mean traversal time
         */
        public double getMean(Configuration config) {
            return config.getTraversalSecurityToBoardingMean();
        }

        /**
         * Returns the standard deviation of traversal time.
         *
         * @param config simulation configuration containing traversal parameters
         * @return traversal time standard deviation
         */
        public double getStdDev(Configuration config) {
            return config.getTraversalSecurityToBoardingStdDev();
        }
    },

    /**
     * Transition from customs to boarding.
     */
    CUSTOMS_TO_BOARDING {
        /**
         * Returns the mean traversal time for the transition.
         *
         * @param config simulation configuration containing traversal parameters
         * @return mean traversal time
         */
        public double getMean(Configuration config) {
            return config.getTraversalCustomsToBoardingMean();
        }

        /**
         * Returns the standard deviation of traversal time.
         *
         * @param config simulation configuration containing traversal parameters
         * @return traversal time standard deviation
         */
        public double getStdDev(Configuration config) {
            return config.getTraversalCustomsToBoardingStdDev();
        }
    };

    /**
     * Returns the mean traversal time for the transition.
     *
     * @param config simulation configuration containing traversal parameters
     * @return mean traversal time
     */
    public abstract double getMean(Configuration config);

    /**
     * Returns the standard deviation of traversal time.
     *
     * @param config simulation configuration containing traversal parameters
     * @return traversal time standard deviation
     */
    public abstract double getStdDev(Configuration config);
}