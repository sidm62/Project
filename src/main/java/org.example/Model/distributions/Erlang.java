package org.example.Model.distributions;

/**
 * Random number generator based on the Erlang distribution.
 *
 * @author F. Mallet, based on Costas Simatos's original
 * @version 1.0, 2 October 2002
 */
public class Erlang extends Generator implements ContinuousGenerator {

    /**
     * Scale parameter of the Erlang distribution.
     */
    private double scale;

    /**
     * Shape parameter of the Erlang distribution.
     */
    private double shape;

    /**
     * Creates an Erlang random number generator.
     * The seed is automatically provided by the {@code SeedGenerator}.
     *
     * @param shape the shape parameter of the distribution
     * @param scale the scale parameter of the distribution
     */
    public Erlang(double shape, double scale) {
        super();
        set(scale, shape);
    }

    /**
     * Creates an Erlang random number generator with a specific seed.
     *
     * @param shape the shape parameter of the distribution
     * @param scale the scale parameter of the distribution
     * @param seed the initial seed for the generator; two instances with
     *             the same seed will generate the same sequence of numbers
     */
    public Erlang(double shape, double scale, long seed) {
        super(seed);
        set(scale, shape);
    }

    /**
     * Sets the parameters of the Erlang distribution.
     *
     * @param scale the scale parameter
     * @param shape the shape parameter
     * @throws ParameterException if the scale parameter is not greater than zero
     */
    private void set(double scale, double shape) {
        if (scale <= 0.0)
            throw new ParameterException("Erlang: The scale parameter must be greater than 0.");
        this.scale = scale;
        this.shape = shape;
    }

    /**
     * Generates a new random number from the Erlang distribution.
     *
     * @return the next random number in the sequence
     */
    public double sample() {
        return distrib.erlang(shape, scale);
    }
}