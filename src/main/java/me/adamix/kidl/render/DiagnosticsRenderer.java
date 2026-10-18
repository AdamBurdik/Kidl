package me.adamix.kidl.render;

import me.adamix.kidl.diagnostic.Diagnostic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public interface DiagnosticsRenderer {
    @Nullable String render(@NotNull Diagnostic diagnostic, @NotNull Map<String, List<String>> sources);

    /**
     * Renders and prints all diagnostics to standard output
     */
    default void sout(@NotNull List<Diagnostic> diagnostics,  @NotNull Map<String, List<String>> sources) {
        diagnostics.forEach(d -> {
            String line = render(d, sources);
            if (line == null) return;
            System.out.println(line);
        });
    }
}
