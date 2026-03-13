package org.example.Model.distributions;

/**
 * Random number generator based on the Gamma distribution.
 *
 * @author F. Mallet, based on Costas Simatos's original
 * @version 1.0, 2 October 2002
 */
public class Gamma extends Generator implements ContinuousGenerator {

    /**
     * Scale parameter of the Gamma distribution.
     */
    private double scale;

    /**
     * Shape parameter of the Gamma distribution.
     */
    private double shape;

    /**
     * Creates a Gamma random number generator.
     * The seed is automatically provided by the {@code SeedGenerator}.
     *
     * @param scale the scale parameter of the distribution
     * @param shape the shape parameter of the distribution
     */
    public Gamma(double scale, double shape) {
        super();
        set(scale, shape);
    }

    /**
     * Creates a Gamma random number generator with a specific seed.
     *
     * @param scale the scale parameter of the distribution
     * @param shape the shape parameter of the distribution
     * @param seed the initial seed for the generator; two instances with
     *             the same seed will generate the same sequence of numbers
     */
    public Gamma(double scale, double shape, long seed) {
        super(seed);
        set(scale, shape);
    }

    /**
     * Sets the parameters of the Gamma distribution.
     *
     * @param scale the scale parameter
     * @param shape the shape parameter
     * @throws ParameterException if scale or shape are not greater than zero
     */
    private void set(double scale, double shape) {
        if ((scale <= 0.0) || (shape <= 0.0))
            throw new ParameterException("Gamma: The scale and shape parameters must be greater than 0.");
        this.scale = scale;
        this.shape = shape;
    }

    /**
     * Generates a new random number from the Gamma distribution.
     *
     * @return the next random number in the sequence
     */
    public double sample() {
        return distrib.gamma(scale, shape);
    }
}