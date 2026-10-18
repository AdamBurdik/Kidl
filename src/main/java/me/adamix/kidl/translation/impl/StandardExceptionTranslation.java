package me.adamix.kidl.translation.impl;

import dev.kdl.parse.KdlParseException;
import dev.kdl.parse.context.Span;
import me.adamix.kidl.Kidl;
import me.adamix.kidl.diagnostic.Diagnostic;
import me.adamix.kidl.diagnostic.FileSpan;
import me.adamix.kidl.translation.ExceptionTranslation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class StandardExceptionTranslation implements ExceptionTranslation {
    @Override
    public @NotNull Diagnostic translate(
            @NotNull String file,
            @NotNull KdlParseException exception,
            @NotNull List<String> lines
    ) {
        String problem = exception.getMessage() != null ? exception.getMessage() : "unknown error";
        Span span = errorSpan(exception);

        FileSpan primary = new FileSpan(file, span);

        return new Diagnostic(
                Kidl.INVALID_KDL,
                "invalid KDL: " + problem,
                primary,
                "unexpected here",
                List.of(),
                null
        );
    }

    static Span errorSpan(KdlParseException e) {
        return e.getContext().span();
    }
}
