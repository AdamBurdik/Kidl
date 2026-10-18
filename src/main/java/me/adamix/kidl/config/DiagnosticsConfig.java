package me.adamix.kidl.config;

import lombok.Builder;
import me.adamix.kidl.error.DiagnosticError;
import me.adamix.kidl.error.Severity;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Builder
public class DiagnosticsConfig {
    @Builder.Default
    private final Map<String, Severity> map = new HashMap<>();


    public Severity severityOf(@NotNull DiagnosticError error) {
        return map.getOrDefault(key(error.namespace(), error.path()), error.defaultSeverity());
    }
    
    private static String key(String namespace, String id) {
        return (namespace + "." + id).toLowerCase(Locale.ROOT);
    }
}
