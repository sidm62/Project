package org.example.Model.distributions;

/**
 * Random number generator based on the Binomial distribution.
 *
 * @author F. Mallet, based on Costas Simatos's original
 * @version 1.0, 2 October 2002
 */
public class Binomial extends Generator implements DiscreteGenerator {

    /**
     * Probability of success in a single trial.
     */
    private double prob;

    /**
     * Number of trials in the binomial experiment.
     */
    private int trials;

    /**
     * Creates a binomial random number generator.
     * The seed is automatically provided by the {@code SeedGenerator}.
     *
     * @param prob probability of success in a trial
     * @param trials number of trials
     */
    public Binomial(double prob, int trials) {
        super();
        set(prob, trials);
    }

    /**
     * Creates a binomial random number generator with a specific seed.
     *
     * @param prob probability of success
     * @param trials number of trials
     * @param seed initial seed for the generator; two instances with
     *             the same seed will generate the same sequence of numbers
     */
    public Binomial(double prob, int trials, long seed) {
        super(seed);
        set(prob, trials);
    }

    /**
     * Sets the parameters for the binomial distribution.
     *
     * @param prob probability of success
     * @param trials number of trials
     * @throws ParameterException if the probability or number of trials is invalid
     */
    private void set(double prob, int trials) {
        if (prob <= 0.0)
            throw new ParameterException("Binomial: The probability of success must be between 0 and 1.");
        if (trials <= 0)
            throw new ParameterException("Binomial: The number of trials must be a positive integer.");
        this.prob = prob;
        this.trials = trials;
    }

    /**
     * Generates a new random number from the binomial distribution.
     *
     * @return the next random number in the sequence
     */
    public long sample() {
        return distrib.binomial(prob, trials);
    }

    /**
     * Returns a string representation of this generator.
     *
     * @return a string describing the binomial generator
     */
    public String toString() {
        return "Binomial("+prob+", "+trials+")";
    }
}