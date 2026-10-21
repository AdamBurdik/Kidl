package me.adamix.kidl;

import dev.kdl.KdlNode;
import me.adamix.kidl.diagnostic.Diagnostic;
import me.adamix.kidl.diagnostic.FileSpan;
import me.adamix.kidl.view.Arguments;
import me.adamix.kidl.view.ChildKind;
import me.adamix.kidl.view.Children;
import me.adamix.kidl.view.DynamicChildren;
import me.adamix.kidl.view.NodeScope;
import me.adamix.kidl.view.Properties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A view over a single KDL node. Values are read through {@link #properties()},
 * {@link #arguments()}, {@link #children(Class)} and {@link #dynamicChildren()}.
 *
 * <p>Call {@link #finish()} once the node has been validated to report anything
 * that was never read.
 */
public class Cursor extends NodeScope {

    private final KdlNode root;
    private final List<KdlNode> childNodes;
    private final Properties properties;
    private final Arguments arguments;

    private final Map<Class<?>, Children<?>> childViews = new HashMap<>();
    private @Nullable DynamicChildren dynamicChildren;

    public Cursor(
            @NotNull String file,
            @NotNull KdlNode root,
            @NotNull List<Diagnostic> diagnostics,
            @NotNull FileSpan owner
    ) {
        super(file, diagnostics, owner);
        this.root = root;
        this.childNodes = root.children();
        this.properties = new Properties(file, root.properties(), diagnostics, owner);
        this.arguments = new Arguments(file, root.arguments(), diagnostics, owner);
    }

    /**
     * The nodes keyed properties.
     * */
    public @NotNull Properties properties() {
        return properties;
    }

    /**
     * The nodes positional arguments.
     * */
    public @NotNull Arguments arguments() {
        return arguments;
    }


    public @NotNull FileSpan span() {
        return new FileSpan(file, root.parseContext().span());
    }
    
    /**
     * The nodes children, validated and typed against {@code type} {@link ChildKind} declaration.
     * Calling this twice with the same type returns the same view.
     */
    @SuppressWarnings("unchecked")
    public <E extends Enum<E> & ChildKind> @NotNull Children<E> children(@NotNull Class<E> type) {
        if (dynamicChildren != null) {
            throw new IllegalStateException("children(...) cannot be combined with dynamicChildren()");
        }

        Children<E> existing = (Children<E>) childViews.get(type);
        if (existing != null) return existing;

        Children<E> view = new Children<>(file, childNodes, type, diagnostics, owner);
        childViews.put(type, view);
        return view;
    }

    /**
     * The nodes children keyed by their KDL node name, for children whose names are not known
     * in advance (e.g. a {@code lookup} mapping). Returns an unmodifiable, source-ordered map.
     * Calling this twice returns the same view.
     *
     * <p>Duplicate child names are reported as {@link Kidl#DEFINED_TWICE} (first occurrence wins).
     * Cannot be combined with {@link #children(Class)} on the same node.
     */
    public @NotNull Map<String, Cursor> dynamicChildren() {
        if (!childViews.isEmpty()) {
            throw new IllegalStateException("dynamicChildren() cannot be combined with children(...)");
        }

        if (dynamicChildren == null) {
            dynamicChildren = new DynamicChildren(file, childNodes, diagnostics, owner);
        }
        return dynamicChildren.asMap();
    }

    /**
     * Reports every property and argument that was never read.
     * When children were declared, also finishes each child cursor,
     * when no children were declared, reports every child node as unexpected.
     */
    public void finish() {
        properties.finish();
        arguments.finish();

        if (childViews.isEmpty() && dynamicChildren == null) {
            for (KdlNode node : childNodes) {
                diagnostics.add(Diagnostic.of(
                        Kidl.UNKNOWN,
                        "unexpected child `" + node.name() + "`",
                        spanOf(node),
                        "not expected here"
                ));
            }
            return;
        }

        for (Children<?> view : childViews.values()) {
            view.finish();
        }
        if (dynamicChildren != null) {
            dynamicChildren.finish();
        }
    }
}
