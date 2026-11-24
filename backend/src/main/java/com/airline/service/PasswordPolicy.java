package com.airline.service;

import com.airline.error.BadRequestException;

import java.security.SecureRandom;

public final class PasswordPolicy {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordPolicy() {
    }

    public static void check(String password) {
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw new BadRequestException("Пароль: от 8 до 72 символов, строчная и заглавная буква, цифра и спецсимвол.");
        }
        boolean lower = false;
        boolean upper = false;
        boolean digit = false;
        boolean special = false;
        for (int i = 0; i < password.length(); i++) {
            char ch = password.charAt(i);
            if (Character.isLowerCase(ch)) {
                lower = true;
            } else if (Character.isUpperCase(ch)) {
                upper = true;
            } else if (Character.isDigit(ch)) {
                digit = true;
            } else {
                special = true;
            }
        }
        if (!lower || !upper || !digit || !special) {
            throw new BadRequestException("Пароль: от 8 до 72 символов, строчная и заглавная буква, цифра и спецсимвол.");
        }
    }

    public static String temporary() {
        StringBuilder password = new StringBuilder("Tm1!");
        for (int i = 0; i < 8; i++) {
            password.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return password.toString();
    }
}
