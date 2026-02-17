package util;

/**
 * Custom random number generator without using java.util.Random.
 * How this works: It uses an LCG algorithm to generate pseudo-random numbers based on a seed value (gotten from System.nanoTime() by default). 
 * The nextInt, nextDouble, etc. methods produce random values based on the current seed and then update the seed for the next call.
 * This allows for reproducible randomness if you use the same seed, which can be useful for testing or certain game mechanics.
 */
public class RNG {
    private long seed;

    public RNG() {
        this.seed = System.nanoTime();
    }

    public RNG(long seed) {
        this.seed = seed;
    }

    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("Bound must be positive");
        }
        seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
        return (int) ((seed >>> 16) % bound);
    }

    public int nextInt(int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("min must be less than max");
        }
        return min + nextInt(max - min);
    }

    public int nextInt() {
        seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
        return (int) (seed >>> 16);
    }

    public double nextDouble() {
        seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
        return ((double) (seed >>> 16)) / (1L << 32);
    }

    public boolean nextBoolean() {
        return nextInt(2) == 1;
    }

    public long nextLong() {
        seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
        long next = seed;
        seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
        return (next << 32) + seed;
    }
}
