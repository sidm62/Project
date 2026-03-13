package org.example.Model.distributions;

/**
 * A SeedGenerator is a {@link DiscreteGenerator} that produces well-spaced seeds.
 *
 * <p>The generator creates seeds that are separated by a configurable spacing
 * value. It relies internally on a {@link RandomGenerator} to generate the
 * sequence of seeds.</p>
 */
public class SeedGenerator implements DiscreteGenerator {

    /**
     * Root seed used for the default generator.
     */
    private static long root = 4851L;

    /**
     * Number of steps between generated seeds.
     */
    private int spacing;

    /**
     * Flag indicating whether the assigned seed has already been sampled.
     */
    private boolean not_sampled = true;

    /**
     * Internal random generator used to produce seeds.
     */
    private RandomGenerator source;

    /**
     * Creates a seed generator with default settings.
     * Default spacing is 100000.
     */
    public SeedGenerator () {
        this(root, 100000);
    }

    /**
     * Creates a seed generator with a specific seed and spacing.
     *
     * @param seed initial seed value
     * @param spacing number of steps between generated seeds
     */
    public SeedGenerator (long seed, int spacing) {
        source = new RandomGenerator(seed);
        this.spacing = spacing;
    }

    // ----- implements Seedable { -----

    /**
     * Sets the seed of the underlying random generator.
     *
     * @param seed the new seed value
     */
    public void setSeed(long seed) {
        source.setSeed(seed);
        not_sampled = true;
    }

    /**
     * Returns the current seed value.
     *
     * @return the current seed
     */
    public long getSeed() {
        return source.getSeed();
    }

    /**
     * Reseeds the generator using the internal random generator.
     */
    public void reseed() {
        source.reseed();
        not_sampled = true;
    }

    // ----- } implements Seedable -----

    // ----- implements DiscreteGenerator { -----

    /**
     * Generates a new seed value.
     *
     * <p>The generator advances the underlying random generator by
     * {@code spacing} steps between successive seeds.</p>
     *
     * @return the generated seed value
     */
    public long sample() {
        if (not_sampled)
            not_sampled = false;
        else
            for (int i = 0; i < spacing; i++)
                source.nextLong();
        return getSeed();
    }

    // ----- } implements DiscreteGenerator -----

    /**
     * Default seed generator used by the library.
     */
    private static SeedGenerator defaut = new SeedGenerator();

    /**
     * Returns the default seed generator.
     *
     * @return the default seed generator
     */
    public static SeedGenerator getDefaultSeedGenerator() {
        return defaut;
    }

    /**
     * Sets a new default seed generator.
     *
     * @param seed initial seed value
     * @param spacing spacing between generated seeds
     */
    static void setDefaultSeedGenerator(long seed, int spacing) {
        defaut = new SeedGenerator(seed, spacing);
    }
}