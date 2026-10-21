package me.adamix.kidl;

import me.adamix.kidl.diagnostic.Diagnostic;
import me.adamix.kidl.error.DiagnosticError;
import me.adamix.kidl.validator.Validator;
import me.adamix.kidl.view.ChildKind;
import me.adamix.kidl.view.TypedCursor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CursorSpanTest {

    @TempDir
    Path tempDir;

    private int fileCounter;

    @Test
    void missingChildArgumentUsesChildSpan() throws IOException {
        Result result = load("""
                root {
                    section {
                    }
                }
                """, (rootId, context, cursor) -> {
            for (TypedCursor<RootChild> child : cursor.children(RootChild.class)) {
                child.arguments().getString(0);
            }
        });

        assertLine(result, 2);
    }

    @Test
    void missingChildPropertyUsesChildSpan() throws IOException {
        Result result = load("""
                root {
                    group "enchantments"
                }
                """, (rootId, context, cursor) -> {
            for (TypedCursor<RootChild> child : cursor.children(RootChild.class)) {
                child.arguments().optString(0);
                child.properties().getString("each");
            }
        });

        assertLine(result, 2);
    }

    @Test
    void missingNestedChildUsesNestedNodeSpan() throws IOException {
        Result result = load("""
                root {
                    entry "a" {
                    }
                }
                """, (rootId, context, cursor) -> {
            for (TypedCursor<RootChild> entry : cursor.children(RootChild.class)) {
                entry.arguments().optString(0);
                entry.children(EntryChild.class);
            }
        });

        assertLine(result, 2);
    }

    @Test
    void missingDynamicChildArgumentUsesEntrySpan() throws IOException {
        Result result = load("""
                root {
                    lookup {
                        a
                    }
                }
                """, (rootId, context, cursor) -> {
            for (TypedCursor<RootChild> lookup : cursor.children(RootChild.class)) {
                lookup.dynamicChildren().values().forEach(entry -> entry.arguments().getString(0));
            }
        });

        assertLine(result, 3);
    }

    @Test
    void missingRootArgumentUsesRootSpan() throws IOException {
        Result result = load("""
                root
                """, (rootId, context, cursor) -> cursor.arguments().getString(0));

        assertLine(result, 1);
    }

    @Test
    void validNestedNodesProduceNoDiagnostics() throws IOException {
        Result result = load("""
                root {
                    section "one" {
                        display "x"
                    }
                }
                """, (rootId, context, cursor) -> {
            TypedCursor<RootChild> section = cursor.children(RootChild.class)
                    .get(RootChild.SECTION)
                    .orElseThrow();

            assertEquals("one", section.arguments().getString(0));
            assertEquals(2, section.span().start().line());

            TypedCursor<SectionChild> display = section.children(SectionChild.class)
                    .get(SectionChild.DISPLAY)
                    .orElseThrow();

            assertEquals("x", display.arguments().getString(0));
        });

        assertTrue(result.getDiagnostics().isEmpty(), () -> result.getDiagnostics().toString());
    }

    private Result load(String source, Validator validator) throws IOException {
        Path file = tempDir.resolve("fixture-" + fileCounter++ + ".kdl");
        Files.writeString(file, source);

        return Kidl.builder()
                .renderer((diagnostic, sources) -> null)
                .build()
                .load(file, validator);
    }

    private static void assertLine(Result result, int expectedLine) {
        Diagnostic diagnostic = result.getDiagnostics().stream()
                .filter(candidate -> candidate.error().equals(Kidl.MISSING_REQUIRED))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Missing MISSING_REQUIRED diagnostic: " + result.getDiagnostics()
                ));

        assertEquals(expectedLine, diagnostic.span().start().line(), diagnostic.toString());
    }

    private enum RootChild implements ChildKind {
        SECTION("section", Cardinality.OPTIONAL),
        GROUP("group", Cardinality.OPTIONAL),
        ENTRY("entry", Cardinality.OPTIONAL),
        LOOKUP("lookup", Cardinality.OPTIONAL);

        private final String id;
        private final Cardinality cardinality;

        RootChild(String id, Cardinality cardinality) {
            this.id = id;
            this.cardinality = cardinality;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public Cardinality cardinality() {
            return cardinality;
        }
    }

    private enum SectionChild implements ChildKind {
        DISPLAY("display", Cardinality.ONE);

        private final String id;
        private final Cardinality cardinality;

        SectionChild(String id, Cardinality cardinality) {
            this.id = id;
            this.cardinality = cardinality;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public Cardinality cardinality() {
            return cardinality;
        }
    }

    private enum EntryChild implements ChildKind {
        DISPLAY("display", Cardinality.ONE);

        private final String id;
        private final Cardinality cardinality;

        EntryChild(String id, Cardinality cardinality) {
            this.id = id;
            this.cardinality = cardinality;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public Cardinality cardinality() {
            return cardinality;
        }
    }
}
