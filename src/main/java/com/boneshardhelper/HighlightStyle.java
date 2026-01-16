package com.boneshardhelper;

public enum HighlightStyle {
    HIGHLIGHT_CLICKBOX("Clickbox"),
    HIGHLIGHT_OUTLINE("Outline");

    private final String name;

    HighlightStyle(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
