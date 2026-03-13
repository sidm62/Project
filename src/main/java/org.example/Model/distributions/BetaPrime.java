package org.example.Model.distributions;

/**
 * Random number generator based on the beta prime distribution.
 *
 * @author F. Mallet, based on Costas Simatos's original
 * @version 1.0, 2 October 2002
 */
public class BetaPrime extends Beta {

    /**
     * Creates a beta prime random number generator.
     * The seed is automatically provided by the {@code SeedGenerator}.
     *
     * @param shape_a the first shape parameter of the distribution
     * @param shape_b the second shape parameter of the distribution
     */
    public BetaPrime(double shape_a, double shape_b) {
        super(shape_a, shape_b);
    }

    /**
     * Creates a beta prime random number generator with a specific seed.
     *
     * @param shape_a the first shape parameter of the distribution
     * @param shape_b the second shape parameter of the distribution
     * @param seed the initial seed for the generator; two instances with
     *             the same seed will generate the same sequence of numbers
     */
    public BetaPrime(double shape_a, double shape_b, long seed) {
        super(shape_a, shape_b, seed);
    }

    /**
     * Generates a new random number.
     *
     * @return the next random number in the sequence
     */
    public double sample() {
        return distrib.betaprime(shape_a, shape_b);
    }
}