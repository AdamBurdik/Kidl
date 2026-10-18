package me.adamix.kidl.view;

import dev.kdl.KdlNode;
import me.adamix.kidl.Cardinality;
import me.adamix.kidl.Cursor;
import me.adamix.kidl.Kidl;
import me.adamix.kidl.diagnostic.Diagnostic;
import me.adamix.kidl.diagnostic.FileSpan;
import me.adamix.kidl.diagnostic.Note;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The children of a node, validated against a {@link ChildKind} declaration and iterable
 * as {@link TypedCursor}s. Obtain one via {@link Cursor#children(Class)}.
 *
 * <p>Validation happens eagerly: unknown children, duplicates (for {@link Cardinality#ONE}
 * and {@link Cardinality#OPTIONAL}) and missing required children are reported on creation.
 *
 * @param <E> the declaring enum
 */
public final class Children<E extends Enum<E> & ChildKind> extends NodeScope implements Iterable<TypedCursor<E>> {

    private final List<TypedCursor<E>> children = new ArrayList<>();

    public Children(
            @NotNull String file,
            @NotNull List<KdlNode> nodes,
            @NotNull Class<E> type,
            @NotNull List<Diagnostic> diagnostics,
            @NotNull FileSpan owner
    ) {
        super(file, diagnostics, owner);

        E[] constants = type.getEnumConstants();
        Map<String, E> byId = new LinkedHashMap<>();
        for (E constant : constants) byId.put(constant.id(), constant);

        Map<String, List<KdlNode>> grouped = new LinkedHashMap<>();
        for (KdlNode node : nodes) {
            E constant = byId.get(node.name());
            if (constant == null) {
                diagnostics.add(Diagnostic.of(
                        Kidl.UNKNOWN,
                        "unknown child `" + node.name() + "`",
                        spanOf(node),
                        "not a recognised child",
                        suggest(node.name(), byId.keySet())
                ));
                continue;
            }
            grouped.computeIfAbsent(node.name(), key -> new ArrayList<>()).add(node);
        }

        for (E constant : constants) {
            List<KdlNode> matching = grouped.getOrDefault(constant.id(), List.of());
            Cardinality cardinality = constant.cardinality();

            if (matching.size() < cardinality.min()) {
                diagnostics.add(Diagnostic.of(
                        Kidl.MISSING_REQUIRED,
                        "missing required child `" + constant.id() + "`",
                        owner,
                        "needs a `" + constant.id() + "` child"
                ));
            }

            for (int i = cardinality.max(); i < matching.size(); i++) {
                diagnostics.add(new Diagnostic(
                        Kidl.DEFINED_TWICE,
                        "child `" + constant.id() + "` may only be defined once",
                        spanOf(matching.get(i)),
                        "redefined here",
                        List.of(new Note(spanOf(matching.get(0)), "first defined here")),
                        null
                ));
            }
        }

        for (KdlNode node : nodes) {
            E constant = byId.get(node.name());
            if (constant != null) {
                children.add(new TypedCursor<>(file, node, diagnostics, owner, constant));
            }
        }
    }

    /** Number of known (declared) children, in source order. */
    public int count() {
        return children.size();
    }

    /** Whether there are no known (declared) children. */
    public boolean isEmpty() {
        return children.isEmpty();
    }

    /** The first child of {@code kind}, if present. */
    public @NotNull Optional<TypedCursor<E>> get(@NotNull E kind) {
        for (TypedCursor<E> child : children) {
            if (child.kind() == kind) return Optional.of(child);
        }
        return Optional.empty();
    }

    /** All children of {@code kind}, in source order. */
    public @NotNull List<TypedCursor<E>> list(@NotNull E kind) {
        List<TypedCursor<E>> result = new ArrayList<>();
        for (TypedCursor<E> child : children) {
            if (child.kind() == kind) result.add(child);
        }
        return result;
    }

    @Override
    public @NotNull Iterator<TypedCursor<E>> iterator() {
        return children.iterator();
    }

    /**
     * Reports unread properties/arguments inside every child.
     */
    public void finish() {
        for (TypedCursor<E> child : children) {
            child.finish();
        }
    }
}
