package me.adamix.kidl.style.impl;

import me.adamix.kidl.style.Style;
import me.adamix.kidl.style.Styler;

final class PlainStyler implements Styler {
    public String apply(Style style, String text) { return text; }
}