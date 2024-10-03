package com.dev.gear.type;

public enum ConditionType {
    EQUALS("="), NOT_EQUALS("!="), LESS_THAN("<"), LESS_THAN_OR_EQUALS("<="),
    GREATER_THAN(">"), GREATER_THAN_OR_EQUALS(">="), LIKE("like"), IN("in");

    private final String symbol;

    ConditionType(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}