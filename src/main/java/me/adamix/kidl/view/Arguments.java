package me.adamix.kidl.view;

import dev.kdl.KdlValue;
import me.adamix.kidl.Cursor;
import me.adamix.kidl.Kidl;
import me.adamix.kidl.diagnostic.Diagnostic;
import me.adamix.kidl.diagnostic.FileSpan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Reads a node's positional arguments, either by index or sequentially.
 * Obtain one via {@link Cursor#arguments()}.
 */
public final class Arguments extends NodeScope {

    private final List<KdlValue<?>> values;
    private final Set<Integer> known = new LinkedHashSet<>();
    private int next = 0;

    public Arguments(
            @NotNull String file,
            @NotNull List<KdlValue<?>> values,
            @NotNull List<Diagnostic> diagnostics,
            @NotNull FileSpan owner
    ) {
        super(file, diagnostics, owner);
        this.values = new ArrayList<>(values);
    }

    /**
     * Number of positional arguments on the node.
     * */
    public int count() { return values.size(); }

    /**
     * Whether an argument exists at {@code index}.
     * */
    public boolean has(int index) { return index >= 0 && index < values.size(); }

    private @Nullable KdlValue<?> get(int index) {
        if (index < 0 || index >= values.size()) return null;
        known.add(index);
        return values.get(index);
    }

    private @Nullable KdlValue<?> require(int index) {
        KdlValue<?> value = get(index);
        if (value == null) reportMissing(index);
        return value;
    }

    private void reportMissing(int index) {
        diagnostics.add(Diagnostic.of(
                Kidl.MISSING_REQUIRED,
                "missing required argument #" + index,
                owner,
                "needs an argument here"
        ));
    }

    private <T> @Nullable T required(int index, @NotNull Kind<T> kind) {
        KdlValue<?> value = require(index);
        return value == null ? null : read(argumentWhere(index), value, kind);
    }

    private <T> @NotNull T withDefault(int index, @NotNull T def, @NotNull Kind<T> kind) {
        KdlValue<?> value = get(index);
        if (value == null) return def;

        T read = read(argumentWhere(index), value, kind);
        return read != null ? read : def;
    }

    private <T> @NotNull Optional<T> optional(int index, @NotNull Kind<T> kind) {
        KdlValue<?> value = get(index);
        if (value == null) return Optional.empty();

        return Optional.ofNullable(read(argumentWhere(index), value, kind));
    }

    private <T> @NotNull List<T> list(int from, @NotNull Kind<T> kind) {
        List<T> result = new ArrayList<>();
        for (int index = Math.max(0, from); index < values.size(); index++) {
            KdlValue<?> value = get(index);
            T item = read(argumentWhere(index), value, kind);
            if (item != null) result.add(item);
        }
        return result;
    }

    private <T> @NotNull List<T> nextList(@NotNull Kind<T> kind) {
        List<T> result = list(next, kind);
        next = values.size();
        return result;
    }

    // ---- string ----
    public @Nullable String getString(int index) { return required(index, STRING); }
    public @NotNull String getString(int index, @NotNull String def) { return withDefault(index, def, STRING); }
    public @NotNull Optional<String> optString(int index) { return optional(index, STRING); }
    public @Nullable String nextString() { return required(next++, STRING); }
    public @NotNull String nextString(@NotNull String def) { return withDefault(next++, def, STRING); }
    public @NotNull Optional<String> optNextString() { return optional(next++, STRING); }
    public @NotNull List<String> getStrings(int from) { return list(from, STRING); }
    public @NotNull List<String> nextStrings() { return nextList(STRING); }

    // ---- int ----
    public @Nullable Integer getInt(int index) { return required(index, INT); }
    public int getInt(int index, int def) { return withDefault(index, def, INT); }
    public @NotNull Optional<Integer> optInt(int index) { return optional(index, INT); }
    public @Nullable Integer nextInt() { return required(next++, INT); }
    public int nextInt(int def) { return withDefault(next++, def, INT); }
    public @NotNull Optional<Integer> optNextInt() { return optional(next++, INT); }
    public @NotNull List<Integer> getInts(int from) { return list(from, INT); }
    public @NotNull List<Integer> nextInts() { return nextList(INT); }

    // ---- long ----
    public @Nullable Long getLong(int index) { return required(index, LONG); }
    public long getLong(int index, long def) { return withDefault(index, def, LONG); }
    public @NotNull Optional<Long> optLong(int index) { return optional(index, LONG); }
    public @Nullable Long nextLong() { return required(next++, LONG); }
    public long nextLong(long def) { return withDefault(next++, def, LONG); }
    public @NotNull Optional<Long> optNextLong() { return optional(next++, LONG); }
    public @NotNull List<Long> getLongs(int from) { return list(from, LONG); }
    public @NotNull List<Long> nextLongs() { return nextList(LONG); }

    // ---- float ----
    public @Nullable Float getFloat(int index) { return required(index, FLOAT); }
    public float getFloat(int index, float def) { return withDefault(index, def, FLOAT); }
    public @NotNull Optional<Float> optFloat(int index) { return optional(index, FLOAT); }
    public @Nullable Float nextFloat() { return required(next++, FLOAT); }
    public float nextFloat(float def) { return withDefault(next++, def, FLOAT); }
    public @NotNull Optional<Float> optNextFloat() { return optional(next++, FLOAT); }
    public @NotNull List<Float> getFloats(int from) { return list(from, FLOAT); }
    public @NotNull List<Float> nextFloats() { return nextList(FLOAT); }

    // ---- double ----
    public @Nullable Double getDouble(int index) { return required(index, DOUBLE); }
    public double getDouble(int index, double def) { return withDefault(index, def, DOUBLE); }
    public @NotNull Optional<Double> optDouble(int index) { return optional(index, DOUBLE); }
    public @Nullable Double nextDouble() { return required(next++, DOUBLE); }
    public double nextDouble(double def) { return withDefault(next++, def, DOUBLE); }
    public @NotNull Optional<Double> optNextDouble() { return optional(next++, DOUBLE); }
    public @NotNull List<Double> getDoubles(int from) { return list(from, DOUBLE); }
    public @NotNull List<Double> nextDoubles() { return nextList(DOUBLE); }

    // ---- boolean ----
    public @Nullable Boolean getBoolean(int index) { return required(index, BOOLEAN); }
    public boolean getBoolean(int index, boolean def) { return withDefault(index, def, BOOLEAN); }
    public @NotNull Optional<Boolean> optBoolean(int index) { return optional(index, BOOLEAN); }
    public @Nullable Boolean nextBoolean() { return required(next++, BOOLEAN); }
    public boolean nextBoolean(boolean def) { return withDefault(next++, def, BOOLEAN); }
    public @NotNull Optional<Boolean> optNextBoolean() { return optional(next++, BOOLEAN); }
    public @NotNull List<Boolean> getBooleans(int from) { return list(from, BOOLEAN); }
    public @NotNull List<Boolean> nextBooleans() { return nextList(BOOLEAN); }

    // ---- char ----
    public @Nullable Character getChar(int index) { return required(index, CHAR); }
    public char getChar(int index, char def) { return withDefault(index, def, CHAR); }
    public @NotNull Optional<Character> optChar(int index) { return optional(index, CHAR); }
    public @Nullable Character nextChar() { return required(next++, CHAR); }
    public char nextChar(char def) { return withDefault(next++, def, CHAR); }
    public @NotNull Optional<Character> optNextChar() { return optional(next++, CHAR); }
    public @NotNull List<Character> getChars(int from) { return list(from, CHAR); }
    public @NotNull List<Character> nextChars() { return nextList(CHAR); }

    /**
     * Reads a required argument at {@code index} that must be one of {@code possibilities}.
     */
    public @Nullable String oneOf(int index, @NotNull Set<String> possibilities) {
        KdlValue<?> value = require(index);
        return value == null ? null : match(argumentWhere(index), value, possibilities);
    }

    /**
     * Like {@link #oneOf(int, Set)}, but returns {@code def} when the argument is absent or unknown.
     */
    public @NotNull String oneOf(int index, @NotNull Set<String> possibilities, @NotNull String def) {
        KdlValue<?> value = get(index);
        if (value == null) return def;

        String match = match(argumentWhere(index), value, possibilities);
        return match != null ? match : def;
    }

    /**
     * Optional argument that must be one of {@code possibilities}; absent argument gives an empty result.
     */
    public @NotNull Optional<String> optOneOf(int index, @NotNull Set<String> possibilities) {
        KdlValue<?> value = get(index);
        if (value == null) return Optional.empty();

        return Optional.ofNullable(match(argumentWhere(index), value, possibilities));
    }

    /**
     * Reads a required argument at {@code index} that must name one of the constants of {@code type}.
     */
    public <E extends Enum<E>> @Nullable E oneOf(int index, @NotNull Class<E> type) {
        KdlValue<?> value = require(index);
        return value == null ? null : matchEnum(argumentWhere(index), value, type);
    }

    /**
     * Like {@link #oneOf(int, Class)}, but returns {@code def} when the argument is absent or unknown.
     */
    public <E extends Enum<E>> @NotNull E oneOf(int index, @NotNull Class<E> type, @NotNull E def) {
        KdlValue<?> value = get(index);
        if (value == null) return def;

        E match = matchEnum(argumentWhere(index), value, type);
        return match != null ? match : def;
    }

    /**
     * Optional argument that must name one of the constants of {@code type}; absent argument gives an empty result.
     */
    public <E extends Enum<E>> @NotNull Optional<E> optOneOf(int index, @NotNull Class<E> type) {
        KdlValue<?> value = get(index);
        if (value == null) return Optional.empty();

        return Optional.ofNullable(matchEnum(argumentWhere(index), value, type));
    }

    // ---- oneOf (sequential) ----
    public @Nullable String nextOneOf(@NotNull Set<String> possibilities) {
        return oneOf(next++, possibilities);
    }

    public @NotNull String nextOneOf(@NotNull Set<String> possibilities, @NotNull String def) {
        return oneOf(next++, possibilities, def);
    }

    public @NotNull Optional<String> optNextOneOf(@NotNull Set<String> possibilities) {
        return optOneOf(next++, possibilities);
    }

    public <E extends Enum<E>> @Nullable E nextOneOf(@NotNull Class<E> type) {
        return oneOf(next++, type);
    }

    public <E extends Enum<E>> @NotNull E nextOneOf(@NotNull Class<E> type, @NotNull E def) {
        return oneOf(next++, type, def);
    }

    public <E extends Enum<E>> @NotNull Optional<E> optNextOneOf(@NotNull Class<E> type) {
        return optOneOf(next++, type);
    }

    /**
     * Reports every argument that was never read.
     */
    public void finish() {
        for (int index = 0; index < values.size(); index++) {
            if (known.contains(index)) continue;

            diagnostics.add(Diagnostic.of(
                    Kidl.UNKNOWN,
                    "unexpected argument #" + index,
                    spanOf(values.get(index)),
                    "not expected here"
            ));
        }
    }
}
