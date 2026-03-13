package org.example.Model.distributions;

/**
 * Random number generator producing pseudorandom numbers based on a seed.
 *
 * <p>This generator is a multiplicative linear congruential generator (LCG)
 * defined by the recurrence:</p>
 *
 * <pre>
 * Y[1] = (742938285 * Y[0]) mod (2^31 − 1)
 * </pre>
 *
 * <p>The seed (Y[0]) is used to generate a sequence of pseudorandom numbers
 * uniformly distributed between {@code 0} and {@code 1}. The cycle length
 * of the generator is {@code 2^31 − 2}.</p>
 *
 * @version 1.0, 2 October 2002
 * @author F. Mallet, based on Costas Simatos's original
 */
public class RandomGenerator implements ContinuousGenerator {

    /**
     * Multiplier used in the linear congruential generator.
     */
    private final long a = 742938285;

    /**
     * Modulus used in the generator.
     */
    private final long m = 2147483647;

    /**
     * Current seed (last generated random value).
     */
    private long seed;

    /**
     * Creates a generator with an automatically generated seed.
     */
    public RandomGenerator () {
        reseed();
    }

    /**
     * Creates a generator with a specific seed.
     *
     * @param seed initial seed value
     */
    public RandomGenerator (long seed) {
        setSeed(seed);
    }

    // ----- implements ContinuousGenerator { -----

    /**
     * Generates a random number uniformly distributed between 0 and 1.
     *
     * @return the next random number in the sequence
     */
    public double sample() {
        return ((double)nextLong()) / m;
    }

    // ----- } implements ContinuousGenerator -----

    /**
     * Generates the next random long value in the sequence.
     *
     * @return the next pseudorandom long value
     */
    public long nextLong() {
        return seed = (a * seed) % m;
    }

    // ----- implements Seedable { -----

    /**
     * Sets the seed of the generator.
     *
     * @param seed the new seed value
     */
    public void setSeed(long seed) {
        this.seed = seed;
    }

    /**
     * Returns the current seed value.
     *
     * @return the current seed
     */
    public long getSeed() {
        return seed;
    }

    /**
     * Reseeds the generator using the default seed generator.
     */
    public void reseed() {
        this.seed = SeedGenerator.getDefaultSeedGenerator().sample();
    }

    // ----- } implements Seedable -----
}