package me.adamix.kidl;

import me.adamix.kidl.view.ChildKind;

/**
 * How many children of a given {@link ChildKind} a node may have.
 */
public enum Cardinality {

    /** Exactly one. */
    ONE(1, 1),

    /** Zero or one. */
    OPTIONAL(0, 1),

    /** Zero or more. */
    MANY(0, Integer.MAX_VALUE),

    /** One or more. */
    AT_LEAST_ONE(1, Integer.MAX_VALUE);

    private final int min;
    private final int max;

    Cardinality(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    public boolean allows(int count) {
        return count >= min && count <= max;
    }
}
