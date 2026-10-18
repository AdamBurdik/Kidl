package me.adamix.kidl.diagnostic;

import dev.kdl.parse.context.Position;
import dev.kdl.parse.context.Span;

public record FileSpan(
        String file,
        Span span
) {
    public Position start() {
        return span.start();
    }
    
    public Position end() {
        return span.end();
    }
}
