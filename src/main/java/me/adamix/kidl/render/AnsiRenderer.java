package me.adamix.kidl.render;

import lombok.RequiredArgsConstructor;
import me.adamix.kidl.config.DiagnosticsConfig;
import me.adamix.kidl.diagnostic.Diagnostic;
import me.adamix.kidl.diagnostic.FileSpan;
import me.adamix.kidl.diagnostic.Note;
import me.adamix.kidl.error.Severity;
import me.adamix.kidl.style.Style;
import me.adamix.kidl.style.Styler;
import me.adamix.kidl.style.impl.AnsiStyler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class AnsiRenderer implements DiagnosticsRenderer {
    private final Styler styler;
    private final DiagnosticsConfig diagnosticsConfig;

    public AnsiRenderer(@NotNull DiagnosticsConfig diagnosticsConfig) {
        this.diagnosticsConfig = diagnosticsConfig;
        this.styler = new AnsiStyler();
    } 
    
    @Override
    public @Nullable String render(@NotNull Diagnostic diagnostic, @NotNull Map<String, List<String>> sources) {
        int maxLine = diagnostic.span().start().line();
        for (Note n : diagnostic.notes()) {
            maxLine = Math.max(maxLine, n.span().start().line());
        }
        int w = String.valueOf(maxLine).length();
        String pad = " ".repeat(w);

        Severity severity = diagnosticsConfig.severityOf(diagnostic.error());
        if (severity == Severity.SUPPRESSED) return null;
        
        Style severityStyle = severity == Severity.ERROR ? Style.ERROR : Style.WARNING;

        StringBuilder sb = new StringBuilder();

        sb.append(styler.apply(severityStyle, severity.name().toLowerCase()))
                .append(": ").append(diagnostic.message()).append('\n');

        appendSpan(sb, "-->", diagnostic.span(), diagnostic.label(), '^', severityStyle, w, sources, styler);

        for (Note n : diagnostic.notes()) {
            sb.append(pad).append(' ').append(styler.apply(Style.GUTTER, "|")).append('\n');
            appendSpan(sb, ":::", n.span(), n.label(), '-', Style.NOTE, w, sources, styler);
        }

        // Help
        if (diagnostic.help() != null) {
            sb.append(pad).append(' ').append(styler.apply(Style.GUTTER, "|")).append('\n')
                    .append(pad).append(' ').append(styler.apply(Style.GUTTER, "=")).append(' ')
                    .append(styler.apply(Style.HELP, "help:")).append(' ')
                    .append(diagnostic.help()).append('\n');
        }
        return sb.toString();
    }

    private void appendSpan(
            StringBuilder sb, String arrow, FileSpan s, String label,
            char marker, Style markerStyle, int w,
            Map<String, List<String>> sources, Styler styler
    ) {
        String pad = " ".repeat(w);
        int line = s.start().line() - 1;
        int startCol = s.start().column() - 1;

        sb.append(pad).append(styler.apply(Style.GUTTER, arrow)).append(' ')
                .append(s.file()).append(':').append(line + 1)
                .append(':').append(startCol + 1).append('\n');

        List<String> lines = sources.get(s.file());
        if (lines == null || line < 0 || line >= lines.size()) {
            return;
        }
        String text = lines.get(line);

        int endCol = (s.end().line() - 1) == line ? s.end().column() : text.length();
        int width = Math.max(1, endCol - startCol);

        String number = String.format("%" + w + "d", line + 1);

        sb.append(pad).append(' ').append(styler.apply(Style.GUTTER, "|")).append('\n');

        sb.append(styler.apply(Style.GUTTER, number + " |")).append(' ')
                .append(text).append('\n');

        String underline = String.valueOf(marker).repeat(width);
        if (label != null && !label.isEmpty()) {
            underline += " " + label;
        }
        String indent = text.substring(0, Math.min(startCol, text.length())).replaceAll("[^\t]", " ");

        sb.append(pad).append(' ').append(styler.apply(Style.GUTTER, "|")).append(' ')
                .append(indent)
                .append(styler.apply(markerStyle, underline)).append('\n');
    }
}
