package me.adamix.kidl.view;

import dev.kdl.KdlNode;
import me.adamix.kidl.Cursor;
import me.adamix.kidl.Kidl;
import me.adamix.kidl.diagnostic.Diagnostic;
import me.adamix.kidl.diagnostic.FileSpan;
import me.adamix.kidl.diagnostic.Note;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Children whose names are not known in advance, e.g.
 * <pre>{@code
 * lookup {
 *     "common" "<gray>Common"
 *     "rare"   "<blue>Rare"
 * }
 * }</pre>
 * Each child is keyed by its KDL node name. Obtained via {@link Cursor#dynamicChildren()}.
 *
 * <p>Duplicate names are reported as {@link Kidl#DEFINED_TWICE}; the first occurrence wins.
 */
public final class DynamicChildren extends NodeScope {

    private final Map<String, Cursor> entries = new LinkedHashMap<>();

    public DynamicChildren(
            @NotNull String file,
            @NotNull List<KdlNode> nodes,
            @NotNull List<Diagnostic> diagnostics,
            @NotNull FileSpan owner
    ) {
        super(file, diagnostics, owner);

        Map<String, FileSpan> firstSpans = new LinkedHashMap<>();
        for (KdlNode node : nodes) {
            String key = node.name();
            FileSpan span = spanOf(node);

            if (entries.containsKey(key)) {
                diagnostics.add(new Diagnostic(
                        Kidl.DEFINED_TWICE,
                        "child `" + key + "` may only be defined once",
                        span,
                        "redefined here",
                        List.of(new Note(firstSpans.get(key), "first defined here")),
                        null
                ));
                continue;
            }

            entries.put(key, new Cursor(file, node, diagnostics));
            firstSpans.put(key, span);
        }
    }

    public Map<String, Cursor> asMap() {
        return Collections.unmodifiableMap(entries);
    }

    /**
     * Reports unread properties/arguments inside every entry.
     */
    public void finish() {
        for (Cursor entry : entries.values()) {
            entry.finish();
        }
    }
}
