package com.airline.security;

public final class Correlation {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private Correlation() {
    }

    public static String get() {
        return CURRENT.get();
    }

    public static void set(String id) {
        CURRENT.set(id);
    }

    public static void clear() {
        CURRENT.remove();
    }
}
