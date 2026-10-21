package me.adamix.kidl.view;

import dev.kdl.KdlNode;
import me.adamix.kidl.Cursor;
import me.adamix.kidl.diagnostic.Diagnostic;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A {@link Cursor} for a child node whose kind has been resolved from a
 * {@link ChildKind} declaration. See {@link Cursor#children(Class)}.
 *
 * @param <E> the declaring enum
 */
public final class TypedCursor<E extends Enum<E> & ChildKind> extends Cursor {

    private final E kind;

    TypedCursor(
            @NotNull String file,
            @NotNull KdlNode node,
            @NotNull List<Diagnostic> diagnostics,
            @NotNull E kind
    ) {
        super(file, node, diagnostics);
        this.kind = kind;
    }

    /** The declared kind of this child. */
    public @NotNull E kind() {
        return kind;
    }
}
