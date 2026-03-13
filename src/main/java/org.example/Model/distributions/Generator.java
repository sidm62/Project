package org.example.Model.distributions;

/**
 * Abstract base class for random number generators.
 *
 * This class delegates its {@link Seedable} functionality to a
 * {@link Distributions} object, which provides the underlying
 * pseudo-random number generation.
 */
public abstract class Generator implements Seedable {

    /**
     * Distribution helper object used to generate random values.
     */
    protected Distributions distrib;

    /**
     * Creates a generator with an automatically generated seed.
     */
    Generator () { distrib = new Distributions(); }

    /**
     * Creates a generator with a specific seed.
     *
     * @param seed initial seed for the random number generator
     */
    Generator (long seed) { distrib = new Distributions(seed); }

    // ----- implements Seedable { -----

    /**
     * Sets the seed of the underlying random generator.
     *
     * @param seed the new seed value
     */
    public void setSeed(long seed) { distrib.source.setSeed(seed); }

    /**
     * Returns the current seed of the generator.
     *
     * @return the current seed value
     */
    public long getSeed() { return distrib.source.getSeed(); }

    /**
     * Reseeds the generator using the seed generator.
     */
    public void reseed() { distrib.source.reseed(); }

    // ----- } implements Seedable -----
}