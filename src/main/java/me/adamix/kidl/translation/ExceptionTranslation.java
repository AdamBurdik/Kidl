package me.adamix.kidl.translation;

import dev.kdl.parse.KdlParseException;
import me.adamix.kidl.diagnostic.Diagnostic;
import me.adamix.kidl.translation.impl.StandardExceptionTranslation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ExceptionTranslation {
    @NotNull Diagnostic translate(
            @NotNull String file,
            @NotNull KdlParseException exception,
            @NotNull List<String> lines
    );
    
    static ExceptionTranslation standard() {
        return new StandardExceptionTranslation();
    }
}
