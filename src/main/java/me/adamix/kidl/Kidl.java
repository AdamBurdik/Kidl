package me.adamix.kidl;

import dev.kdl.KdlNode;
import dev.kdl.parse.Kdl2Parser;
import dev.kdl.parse.KdlParseException;
import dev.kdl.parse.KdlParser;
import lombok.Builder;
import me.adamix.kidl.config.DiagnosticsConfig;
import me.adamix.kidl.diagnostic.Diagnostic;
import me.adamix.kidl.diagnostic.FileSpan;
import me.adamix.kidl.error.DiagnosticError;
import me.adamix.kidl.error.Severity;
import me.adamix.kidl.render.AnsiRenderer;
import me.adamix.kidl.render.DiagnosticsRenderer;
import me.adamix.kidl.translation.ExceptionTranslation;
import me.adamix.kidl.validator.Context;
import me.adamix.kidl.validator.Validator;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class Kidl {
    private final DiagnosticsConfig diagnosticsConfig;
    private final ExceptionTranslation exceptionTranslation;
    private final DiagnosticsRenderer renderer;
    private final KdlParser kdlParser;

    private final Map<String, List<String>> sources = new HashMap<>();
    private final List<Diagnostic> diagnostics = new ArrayList<>();

    @Builder
    private Kidl(
            DiagnosticsConfig diagnosticsConfig,
            Function<DiagnosticsConfig, DiagnosticsRenderer> rendererFactory,
            ExceptionTranslation exceptionTranslation,
            DiagnosticsRenderer renderer,
            KdlParser kdlParser
    ) {
        this.diagnosticsConfig = diagnosticsConfig != null
                ? diagnosticsConfig
                : DiagnosticsConfig.builder().build();
        this.exceptionTranslation = exceptionTranslation;
        
        if (renderer != null) {
            this.renderer = renderer;
        } else if (rendererFactory != null) {
            this.renderer = rendererFactory.apply(this.diagnosticsConfig);
        } else {
            throw new IllegalStateException("No renderer configured");
        }

        this.kdlParser = Objects.requireNonNullElseGet(kdlParser, Kdl2Parser::new);
    }

    public @NotNull Result load(@NotNull Path path, @NotNull Validator validator) throws IOException {
        String file = path.toString();
        String text = Files.readString(path);

        sources.put(file, text.lines().toList());

        Context context = new Context(diagnostics);
        
        try {
            var document = kdlParser.parse(file, text);
            for (KdlNode root : document.nodes()) {
                
                Cursor cursor = new Cursor(
                        file,
                        root,
                        diagnostics,
                        new FileSpan(
                                file,
                                root.parseContext().span()
                        )
                );

                validator.accept(
                        root.name(),
                        context,
                        cursor
                );
                
                cursor.finish();
                
            }
            
        } catch (KdlParseException e) {
            exceptionTranslation.translate(file, e, sources.get(file));
            throw new RuntimeException(e);
        }
        
        renderer.sout(diagnostics, sources);
        
        return new Result(diagnosticsConfig, diagnostics);
    }

    public static class KidlBuilder {
        public KidlBuilder ansiRenderer() {
            this.rendererFactory = AnsiRenderer::new;
            return this;
        }

        public KidlBuilder standardExceptionTranslation() {
            this.exceptionTranslation = ExceptionTranslation.standard();
            return this;
        }
    }

    public static final DiagnosticError
            MISSING_REQUIRED = error("MISSING_REQUIRED", Severity.ERROR),
            UNKNOWN = error("UNKNOWN", Severity.WARNING),
    
            WRONG_TYPE = error("WRONG_TYPE", Severity.ERROR),
    
            DEFINED_TWICE = error("DEFINED_TWICE", Severity.ERROR),
            INVALID_KDL = error("INVALID_KDL", Severity.ERROR);

    private static DiagnosticError error(@NotNull String path, @NotNull Severity defaultSeverity) {
        return new DiagnosticError("kidl", path, defaultSeverity);
    }
}
