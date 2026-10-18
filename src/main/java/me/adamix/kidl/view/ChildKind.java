package me.adamix.kidl.view;

import me.adamix.kidl.Cardinality;

/**
 * A declaration of a child node: the KDL node name it matches ({@link #id()}) and how
 * many times it may appear ({@link #cardinality()}).
 *
 * <p>Implementations are normally enums, e.g.
 * <pre>{@code
 * enum ItemChild implements ChildKind {
 *     VALUE("value", Cardinality.ONE),
 *     VARIABLE("variable", Cardinality.MANY),
 *     ITEM_MODEL("item-model", Cardinality.OPTIONAL);
 *     ...
 * }
 * }</pre>
 */
public interface ChildKind {

    /** The KDL node name this child matches. */
    String id();

    /** How many children of this kind a node may have. */
    Cardinality cardinality();
}
