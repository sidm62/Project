package org.example.Model.distributions;

/**
 * Random number generator based on the F-distribution.
 *
 * @author F. Mallet, based on Costas Simatos's original
 * @version 1.0, 2 October 2002
 */
public class FDistribution extends Generator implements ContinuousGenerator {

    /**
     * Numerator degrees of freedom of the F-distribution.
     */
    private long num_deg_freedom;

    /**
     * Denominator degrees of freedom of the F-distribution.
     */
    private long den_deg_freedom;

    /**
     * Creates an F-distribution random number generator.
     * The seed is automatically provided by the {@code SeedGenerator}.
     *
     * @param num_deg_freedom numerator degrees of freedom
     * @param den_deg_freedom denominator degrees of freedom
     */
    public FDistribution(long num_deg_freedom, long den_deg_freedom) {
        super();
        set(num_deg_freedom, den_deg_freedom);
    }

    /**
     * Creates an F-distribution random number generator with a specific seed.
     *
     * @param num_deg_freedom numerator degrees of freedom
     * @param den_deg_freedom denominator degrees of freedom
     * @param seed initial seed for the generator; two instances with
     *             the same seed will generate the same sequence of numbers
     */
    public FDistribution(long num_deg_freedom, long den_deg_freedom, long seed) {
        super(seed);
        set(num_deg_freedom, den_deg_freedom);
    }

    /**
     * Sets the parameters of the F-distribution.
     *
     * @param num_deg_freedom numerator degrees of freedom
     * @param den_deg_freedom denominator degrees of freedom
     * @throws ParameterException if the degrees of freedom are not positive
     */
    private void set(long num_deg_freedom, long den_deg_freedom) {
        if ((num_deg_freedom <= 0L) || (den_deg_freedom <= 0L))
            throw new ParameterException("FDistribution: The degrees of freedom must be positive integers.");
        this.num_deg_freedom = num_deg_freedom;
        this.den_deg_freedom = den_deg_freedom;
    }

    /**
     * Generates a new random number from the F-distribution.
     *
     * @return the next random number in the sequence
     */
    public double sample() {
        return distrib.f(num_deg_freedom, den_deg_freedom);
    }
}