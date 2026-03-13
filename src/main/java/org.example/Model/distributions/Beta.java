package org.example.Model.distributions;

/**
 * A random number generator based on the beta distribution.
 * @version     1.0, 2 October 2002
 * @author      F.Mallet from Costas Simatos's original
 */

public class Beta extends Generator implements ContinuousGenerator {
    /** Shape parameter 'a' of the Beta distribution (must be > 0). */
    protected double shape_a;

    /** Shape parameter 'b' of the Beta distribution (must be > 0). */
    protected double shape_b;
    
    /**
     * the seed is automatically provided by the <code>SeedGenerator</code>
     * @param shape_a The a shape parameter of the distribution
     * @param shape_b The b shape parameter of the distribution
     */
    public Beta(double shape_a, double shape_b) {
	super();
	set(shape_a, shape_b);
    }

    /**
     * The constructor with which a specific seed is set for the random
     * number generator
     * @param shape_a The a shape parameter of the distribution
     * @param shape_b The b shape parameter of the distribution
     * @param seed The initial seed for the generator, two instances with
     *             the same seed will generate the same sequence of numbers
     */
    public Beta(double shape_a, double shape_b, long seed) {
	super(seed);
	set(shape_a, shape_b);
    }

    /**
     * Sets the shape parameters for this Beta distribution.
     *
     * @param shape_a first shape parameter (must be > 0)
     * @param shape_b second shape parameter (must be > 0)
     */
    private void set(double shape_a, double shape_b) {
	if ((shape_a <= 0.0) || (shape_b <= 0.0))
	    throw new ParameterException("Beta: The shape parameters must be greater than 0.");
	this.shape_a = shape_a;
	this.shape_b = shape_b;
    }

    /**
     * Generates a new random number following the Beta distribution.
     *
     * @return the next random number generated from the Beta(shape_a, shape_b)
     */
    public double sample() { return distrib.beta(shape_a, shape_b); }
}
