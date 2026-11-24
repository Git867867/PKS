package com.airline.filter;

import com.airline.error.BadRequestException;

public enum PriceComparison {
    GT, GTE, LT, LTE, EQ;

    public static PriceComparison parse(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Укажите операцию сравнения цены: GT, GTE, LT, LTE или EQ.");
        }
        try {
            return PriceComparison.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Неизвестная операция сравнения цены: " + value);
        }
    }
}
