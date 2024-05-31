package com.alexdl.sdng.logging;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.regex.Pattern;

import static java.time.temporal.ChronoField.*;

public class Logger {
    final static Pattern LINK_REGEX = Pattern.compile("'(.*\\.[a-z]+)'");
    final static DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(SECOND_OF_MINUTE, 2)
            .toFormatter();

    final String name;
    final PrintStream printStream;

    public Logger(Class<?> source) {
        this.name = shortenClassName(source.getName());
        this.printStream = System.out;
    }

    private String shortenClassName(String className) {
        String[] parts = className.trim().split("\\.");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            result.append(parts[i].charAt(0));
            result.append('.');
        }
        result.append(parts[parts.length - 1]);
        return result.toString();
    }

    public void err(String text, Object... variables) {
        print(AnsiColor.RED, "   ERROR", text, variables);
    }

    public void warn(String text, Object... variables) {
        print(AnsiColor.YELLOW, "WARNING", text, variables);
    }

    public void info(String text, Object... variables) {
        print(AnsiColor.RESET, "   INFO", text, variables);
    }

    void print(AnsiColor color, String type, String text, Object... variables) {
        String time = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        String result = String.format(text, variables);
        result = String.format("%s [%s] %s: \t%s\n", time, name, type, result);
        result = LINK_REGEX.matcher(result).replaceAll("'" + AnsiColor.GREEN.code + "$1" + color.code + "'");
        result = color.code + result + AnsiColor.RESET.code;
        printStream.print(result);
    }
}
