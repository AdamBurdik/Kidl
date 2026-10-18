package me.adamix.kidl.view;

import dev.kdl.KdlProperties;
import dev.kdl.KdlProperty;
import me.adamix.kidl.Cursor;
import me.adamix.kidl.Kidl;
import me.adamix.kidl.diagnostic.Diagnostic;
import me.adamix.kidl.diagnostic.FileSpan;
import me.adamix.kidl.diagnostic.Note;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Reads a node's keyed properties. Obtain one via {@link Cursor#properties()}.
 */
public final class Properties extends NodeScope {

    private final Map<String, KdlProperty<?>> entries = new LinkedHashMap<>();
    private final Set<String> known = new LinkedHashSet<>();

    public Properties(
            @NotNull String file,
            @NotNull KdlProperties properties,
            @NotNull List<Diagnostic> diagnostics,
            @NotNull FileSpan owner
    ) {
        super(file, diagnostics, owner);

        Map<String, FileSpan> firstSeen = new HashMap<>();
        for (KdlProperty<?> property : properties.properties()) {
            String name = property.name();

            FileSpan first = firstSeen.get(name);
            if (first != null) {
                diagnostics.add(new Diagnostic(
                        Kidl.DEFINED_TWICE,
                        "Property `" + name + "` is defined twice",
                        spanOf(property),
                        "redefined here",
                        List.of(new Note(first, "firsts defined here")),
                        null
                ));
                continue;
            }

            firstSeen.put(name, spanOf(property));
            entries.put(name, property);
        }
    }

    /**
     * Returns the property for {@code key} without reporting a missing key.
     */
    private @Nullable KdlProperty<?> getProperty(@NotNull String key) {
        known.add(key);
        return entries.get(key);
    }

    /**
     * Like {@link #getProperty}, but reports a missing key.
     */
    private @Nullable KdlProperty<?> requireProperty(@NotNull String key) {
        KdlProperty<?> property = getProperty(key);
        if (property == null && !entries.containsKey(key)) reportMissing(key);
        return property;
    }

    private void reportMissing(@NotNull String key) {
        diagnostics.add(Diagnostic.of(
                Kidl.MISSING_REQUIRED,
                "missing required property `" + key + "`",
                owner,
                "needs a `" + key + "` property "
        ));
    }

    private <T> @Nullable T required(@NotNull String key, @NotNull Kind<T> kind) {
        KdlProperty<?> property = requireProperty(key);
        return property == null ? null : read(propertyWhere(key), property.value(), kind);
    }

    private <T> @NotNull T withDefault(@NotNull String key, @NotNull T def, @NotNull Kind<T> kind) {
        KdlProperty<?> property = getProperty(key);
        if (property == null) return def;

        T value = read(propertyWhere(key), property.value(), kind);
        return value != null ? value : def;
    }

    private <T> @NotNull Optional<T> optional(@NotNull String key, @NotNull Kind<T> kind) {
        KdlProperty<?> property = getProperty(key);
        if (property == null) return Optional.empty();

        return Optional.ofNullable(read(propertyWhere(key), property.value(), kind));
    }

    // ---- string ----
    public @Nullable String getString(@NotNull String key) { return required(key, STRING); }
    public @NotNull String getString(@NotNull String key, @NotNull String def) { return withDefault(key, def, STRING); }
    public @NotNull Optional<String> optString(@NotNull String key) { return optional(key, STRING); }

    // ---- int ----
    public @Nullable Integer getInt(@NotNull String key) { return required(key, INT); }
    public int getInt(@NotNull String key, int def) { return withDefault(key, def, INT); }
    public @NotNull Optional<Integer> optInt(@NotNull String key) { return optional(key, INT); }

    // ---- long ----
    public @Nullable Long getLong(@NotNull String key) { return required(key, LONG); }
    public long getLong(@NotNull String key, long def) { return withDefault(key, def, LONG); }
    public @NotNull Optional<Long> optLong(@NotNull String key) { return optional(key, LONG); }

    // ---- float ----
    public @Nullable Float getFloat(@NotNull String key) { return required(key, FLOAT); }
    public float getFloat(@NotNull String key, float def) { return withDefault(key, def, FLOAT); }
    public @NotNull Optional<Float> optFloat(@NotNull String key) { return optional(key, FLOAT); }

    // ---- double ----
    public @Nullable Double getDouble(@NotNull String key) { return required(key, DOUBLE); }
    public double getDouble(@NotNull String key, double def) { return withDefault(key, def, DOUBLE); }
    public @NotNull Optional<Double> optDouble(@NotNull String key) { return optional(key, DOUBLE); }

    // ---- boolean ----
    public @Nullable Boolean getBoolean(@NotNull String key) { return required(key, BOOLEAN); }
    public boolean getBoolean(@NotNull String key, boolean def) { return withDefault(key, def, BOOLEAN); }
    public @NotNull Optional<Boolean> optBoolean(@NotNull String key) { return optional(key, BOOLEAN); }

    // ---- char ----
    public @Nullable Character getChar(@NotNull String key) { return required(key, CHAR); }
    public char getChar(@NotNull String key, char def) { return withDefault(key, def, CHAR); }
    public @NotNull Optional<Character> optChar(@NotNull String key) { return optional(key, CHAR); }

    /**
     * Reads a required property that must be one of {@code possibilities}.
     */
    public @Nullable String oneOf(@NotNull String key, @NotNull Set<String> possibilities) {
        KdlProperty<?> property = requireProperty(key);
        return property == null ? null : match(propertyWhere(key), property.value(), possibilities);
    }

    /**
     * Like {@link #oneOf(String, Set)}, but returns {@code def} when the key is absent or its value is unknown.
     */
    public @NotNull String oneOf(@NotNull String key, @NotNull Set<String> possibilities, @NotNull String def) {
        KdlProperty<?> property = getProperty(key);
        if (property == null) return def;

        String value = match(propertyWhere(key), property.value(), possibilities);
        return value != null ? value : def;
    }

    /**
     * Optional property that must be one of {@code possibilities}; absent key gives an empty result.
     */
    public @NotNull Optional<String> optOneOf(@NotNull String key, @NotNull Set<String> possibilities) {
        KdlProperty<?> property = getProperty(key);
        if (property == null) return Optional.empty();

        return Optional.ofNullable(match(propertyWhere(key), property.value(), possibilities));
    }

    /**
     * Reads a required property that must name one of the constants of {@code type}.
     */
    public <E extends Enum<E>> @Nullable E oneOf(@NotNull String key, @NotNull Class<E> type) {
        KdlProperty<?> property = requireProperty(key);
        return property == null ? null : matchEnum(propertyWhere(key), property.value(), type);
    }

    /**
     * Like {@link #oneOf(String, Class)}, but returns {@code def} when the key is absent or its value is unknown.
     */
    public <E extends Enum<E>> @NotNull E oneOf(@NotNull String key, @NotNull Class<E> type, @NotNull E def) {
        KdlProperty<?> property = getProperty(key);
        if (property == null) return def;

        E value = matchEnum(propertyWhere(key), property.value(), type);
        return value != null ? value : def;
    }

    /**
     * Optional property that must name one of the constants of {@code type}; absent key gives an empty result.
     */
    public <E extends Enum<E>> @NotNull Optional<E> optOneOf(@NotNull String key, @NotNull Class<E> type) {
        KdlProperty<?> property = getProperty(key);
        if (property == null) return Optional.empty();

        return Optional.ofNullable(matchEnum(propertyWhere(key), property.value(), type));
    }

    /**
     * Reports every property that was never read.
     */
    public void finish() {
        for (Map.Entry<String, KdlProperty<?>> entry : entries.entrySet()) {
            if (!known.contains(entry.getKey())) {
                diagnostics.add(new Diagnostic(
                        Kidl.UNKNOWN,
                        "unknown property key `" + entry.getKey() + "`",
                        spanOf(entry.getValue()),
                        "not a recognised option",
                        null,
                        suggest(entry.getKey(), known))
                );
            }
        }
    }
}
