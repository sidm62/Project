public enum Transition {

    CHECKIN_TO_SECURITY {
        public double getMean(Configuration config) {
            return config.getTraversalCheckinToSecurityMean();
        }

        public double getStdDev(Configuration config) {
            return config.getTraversalCheckinToSecurityStdDev();
        }
    },

    SECURITY_TO_CUSTOMS {
        public double getMean(Configuration config) {
            return config.getTraversalSecurityToCustomsMean();
        }

        public double getStdDev(Configuration config) {
            return config.getTraversalSecurityToCustomsStdDev();
        }
    },

    SECURITY_TO_BOARDING {
        public double getMean(Configuration config) {
            return config.getTraversalSecurityToBoardingMean();
        }

        public double getStdDev(Configuration config) {
            return config.getTraversalSecurityToBoardingStdDev();
        }
    },

    CUSTOMS_TO_BOARDING {
        public double getMean(Configuration config) {
            return config.getTraversalCustomsToBoardingMean();
        }

        public double getStdDev(Configuration config) {
            return config.getTraversalCustomsToBoardingStdDev();
        }
    };

    public abstract double getMean(Configuration config);
    public abstract double getStdDev(Configuration config);
}
