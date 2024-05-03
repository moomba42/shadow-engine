package com.alexdl.sdng.logging;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import static java.time.temporal.ChronoField.*;

public class Logger {
    final static DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .appendValue(HOUR_OF_DAY, 2)
        .appendLiteral(':')
        .appendValue(MINUTE_OF_HOUR, 2)
        .appendLiteral(':')
        .appendValue(SECOND_OF_MINUTE, 2)
        .toFormatter();

    final Class<?> source;
    final PrintStream printStream;

    public Logger(Class<?> source) {
        this.source = source;
        this.printStream = System.out;
    }

    public void err(String text, Object... variables) {
        print(AnsiColor.RED, "ERROR: " + text, variables);
    }

    public void warn(String text, Object... variables) {
        print(AnsiColor.YELLOW, "WARNING: " + text, variables);
    }

    public void info(String text, Object... variables) {
        print(AnsiColor.RESET, "INFO: " + text, variables);
    }

    void print(AnsiColor color, String text, Object... variables) {
        String time = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        printStream.printf(color.code + time + " [" + source.getName() + "] " + text + AnsiColor.RESET.code + "\n", variables);
    }
}
