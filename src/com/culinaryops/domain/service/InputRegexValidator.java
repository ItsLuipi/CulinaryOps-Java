package com.culinaryops.domain.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InputRegexValidator {

    public static final String NAME_REGEX = "^[\\p{L}0-9][\\p{L}0-9\\s.,%()/'\"-]{1,149}$";
    public static final String ISO_DATE_REGEX = "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$";
    public static final String POSITIVE_DECIMAL_REGEX = "^\\d+(\\.\\d{1,4})?$";
    public static final String PERCENTAGE_REGEX = "^(100(\\.0{1,2})?|\\d{1,2}(\\.\\d{1,2})?)$";

    private static final Pattern NAME_PATTERN = Pattern.compile(NAME_REGEX);
    private static final Pattern ISO_DATE_PATTERN = Pattern.compile(ISO_DATE_REGEX);
    private static final Pattern POSITIVE_DECIMAL_PATTERN = Pattern.compile(POSITIVE_DECIMAL_REGEX);
    private static final Pattern PERCENTAGE_PATTERN = Pattern.compile(PERCENTAGE_REGEX);

    private InputRegexValidator() {
    }

    public static boolean isValidName(String input) {
        return matches(NAME_PATTERN, input);
    }

    public static boolean isValidIsoDate(String input) {
        return matches(ISO_DATE_PATTERN, input);
    }

    public static boolean isValidPositiveDecimal(String input) {
        return matches(POSITIVE_DECIMAL_PATTERN, input);
    }

    public static boolean isValidPercentage(String input) {
        return matches(PERCENTAGE_PATTERN, input);
    }

    private static boolean matches(Pattern pattern, String input) {
        if (input == null) {
            return false;
        }
        Matcher matcher = pattern.matcher(input.trim());
        return matcher.matches();
    }
}
