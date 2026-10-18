package me.adamix.kidl.validator;

import lombok.RequiredArgsConstructor;
import me.adamix.kidl.diagnostic.Diagnostic;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@RequiredArgsConstructor
public class Context {
    private final List<Diagnostic> diagnostics;
    
    public void addDiagnostic(@NotNull Diagnostic diagnostic) {
        diagnostics.add(diagnostic);
    }
}
