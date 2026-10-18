package me.adamix.kidl.validator;

import me.adamix.kidl.Cursor;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface Validator {
    void accept(
            @NotNull String rootId,
            @NotNull Context context,
            @NotNull Cursor cursor
    );
}
