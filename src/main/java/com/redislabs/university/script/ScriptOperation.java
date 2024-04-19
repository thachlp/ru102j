package com.redislabs.university.script;

public enum ScriptOperation {
    GREATER_THAN(">"),
    LESS_THAN("<");

    private final String symbol;

    ScriptOperation(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
