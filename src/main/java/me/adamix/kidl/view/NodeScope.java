package me.adamix.kidl.view;

import dev.kdl.KdlBoolean;
import dev.kdl.KdlNode;
import dev.kdl.KdlNumber;
import dev.kdl.KdlProperty;
import dev.kdl.KdlString;
import dev.kdl.KdlValue;
import me.adamix.kidl.Cursor;
import me.adamix.kidl.Kidl;
import me.adamix.kidl.diagnostic.Diagnostic;
import me.adamix.kidl.diagnostic.FileSpan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Shared machinery for everything that reads part of a KDL node: {@link Cursor} itself
 * and its views ({@link Properties}, {@link Arguments}, {@link Children}). Each scope
 * carries the source file, the diagnostic sink and the owning span.
 */
public abstract class NodeScope {

    /**
     * How to convert a KDL value into {@code T} and how to describe it in diagnostics.
     */
    protected record Kind<T>(@NotNull String label, @NotNull Function<KdlValue<?>, T> convert) {}

    /**
     * Where a value came from, used to phrase diagnostics. {@code mismatch} is a full message,
     * {@code noun} names the thing in "unknown ..." diagnostics.
     */
    protected record Where(@NotNull String mismatch, @NotNull String noun) {}

    protected static final Kind<String> STRING = new Kind<>("needs to be a string", value -> {
        if (!(value instanceof KdlString string)) throw new IllegalArgumentException();
        return string.value();
    });

    protected static final Kind<Boolean> BOOLEAN = new Kind<>("needs to be a boolean", value -> {
        if (!(value instanceof KdlBoolean bool)) throw new IllegalArgumentException();
        return bool.value();
    });

    protected static final Kind<Integer> INT = new Kind<>("needs to be a whole number", value -> {
        if (!(value instanceof KdlNumber<?> number) || !(number.value() instanceof BigInteger))
            throw new IllegalArgumentException();
        return number.asBigInteger().intValueExact();
    });

    protected static final Kind<Long> LONG = new Kind<>("needs to be a whole number", value -> {
        if (!(value instanceof KdlNumber<?> number) || !(number.value() instanceof BigInteger))
            throw new IllegalArgumentException();
        return number.asBigInteger().longValueExact();
    });

    protected static final Kind<Float> FLOAT = new Kind<>("needs to be a float number", value -> {
        if (!(value instanceof KdlNumber<?> number)) throw new IllegalArgumentException();
        return number.value().floatValue();
    });

    protected static final Kind<Double> DOUBLE = new Kind<>("needs to be a double number", value -> {
        if (!(value instanceof KdlNumber<?> number)) throw new IllegalArgumentException();
        return number.value().doubleValue();
    });

    protected static final Kind<Character> CHAR = new Kind<>("needs to be a single character", value -> {
        if (!(value instanceof KdlString string) || string.value().length() != 1)
            throw new IllegalArgumentException();
        return string.value().charAt(0);
    });

    protected final String file;
    protected final List<Diagnostic> diagnostics;
    protected final FileSpan owner;

    protected NodeScope(@NotNull String file, @NotNull List<Diagnostic> diagnostics, @NotNull FileSpan owner) {
        this.file = file;
        this.diagnostics = diagnostics;
        this.owner = owner;
    }

    protected static Where propertyWhere(@NotNull String key) {
        return new Where("property type mismatch `" + key + "`", key);
    }

    protected static Where argumentWhere(int index) {
        return new Where("argument #" + index + " type mismatch", "argument");
    }

    protected <T> @Nullable T read(@NotNull Where where, @NotNull KdlValue<?> value, @NotNull Kind<T> kind) {
        try {
            return kind.convert().apply(value);
        } catch (IllegalArgumentException | ArithmeticException e) {
            mismatch(where, value, kind);
            return null;
        }
    }

    protected void mismatch(@NotNull Where where, @NotNull KdlValue<?> value, @NotNull Kind<?> kind) {
        diagnostics.add(Diagnostic.of(
                Kidl.WRONG_TYPE,
                where.mismatch(),
                spanOf(value),
                kind.label()
        ));
    }

    protected @Nullable String match(
            @NotNull Where where,
            @NotNull KdlValue<?> value,
            @NotNull Set<String> possibilities
    ) {
        if (!(value instanceof KdlString string)) {
            mismatch(where, value, STRING);
            return null;
        }

        String text = string.value();
        if (possibilities.contains(text)) return text;

        reportUnknown(where, text, spanOf(value), possibilities);
        return null;
    }

    protected <E extends Enum<E>> @Nullable E matchEnum(
            @NotNull Where where,
            @NotNull KdlValue<?> value,
            @NotNull Class<E> type
    ) {
        if (!(value instanceof KdlString string)) {
            mismatch(where, value, STRING);
            return null;
        }

        String text = string.value();
        E[] constants = type.getEnumConstants();
        List<String> names = new ArrayList<>(constants.length);
        for (E constant : constants) {
            if (constant.name().equals(text)) return constant;
            names.add(constant.name());
        }

        reportUnknown(where, text, spanOf(value), names);
        return null;
    }

    protected void reportUnknown(
            @NotNull Where where,
            @NotNull String value,
            @NotNull FileSpan span,
            @NotNull Collection<String> possibilities
    ) {
        diagnostics.add(Diagnostic.of(
                Kidl.UNKNOWN,
                "unknown " + where.noun() + " `" + value + "`",
                span,
                "no such " + where.noun(),
                suggest(value, possibilities)
        ));
    }

    protected @NotNull FileSpan spanOf(@NotNull KdlValue<?> value) {
        var context = value.parseContext();
        return context != null ? new FileSpan(file, context.span()) : owner;
    }

    protected @NotNull FileSpan spanOf(@NotNull KdlProperty<?> property) {
        var context = property.parseContext();
        return context != null ? new FileSpan(file, context.span()) : owner;
    }

    protected @NotNull FileSpan spanOf(@NotNull KdlNode node) {
        var nameSpan = node.nameSpan();
        if (nameSpan != null) return new FileSpan(file, nameSpan);

        var context = node.parseContext();
        return context != null ? new FileSpan(file, context.span()) : owner;
    }

    protected static @Nullable String suggest(@NotNull String input, @NotNull Collection<String> candidates) {
        String best = null;
        int bestD = Integer.MAX_VALUE;
        for (String c : candidates) {
            int distance = lev(input.toLowerCase(), c.toLowerCase());
            if (distance < bestD) {
                bestD = distance;
                best = c;
            }
        }
        return best != null && bestD <= Math.max(1, input.length() / 3)
                ? "did you mean `" + best + "`?" : null;
    }

    /**
     * Levenshtein edit distance.
     * The number of single-character edits (insert, delete, substitute) needed to turn one string into another
     */
    private static int lev(@NotNull String a, @NotNull String b) {
        int[] prev = new int[b.length() + 1], cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++)
                cur[j] = Math.min(Math.min(cur[j - 1], prev[j]) + 1,
                        prev[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1));
            int[] t = prev;
            prev = cur;
            cur = t;
        }
        return prev[b.length()];
    }
}
