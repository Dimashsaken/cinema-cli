package com.cinebook.cli;

import java.util.Scanner;

/** Wraps Scanner for CLI input with prompt display and retry support. */
public class InputReader {

    private final Scanner scanner;

    public InputReader(Scanner scanner) {
        this.scanner = scanner;
    }

    /** Display prompt and read a trimmed line. */
    public String readLine(String prompt) {
        System.out.print(prompt);
        if (scanner.hasNextLine()) {
            return scanner.nextLine().trim();
        }
        return "";
    }

    /** Read a line, retrying until non-blank input is given. */
    public String readNonBlank(String prompt) {
        while (true) {
            String input = readLine(prompt);
            if (!input.isBlank()) return input;
            System.out.println(" Input cannot be blank.");
        }
    }

    /** Read a yes/no confirmation. Returns true for Y/y/yes. */
    public boolean confirm(String prompt) {
        String input = readLine(prompt + " [Y/N] > ");
        return input.equalsIgnoreCase("Y") || input.equalsIgnoreCase("yes");
    }

    /** Read a password (no masking in basic terminal). */
    public String readPassword(String prompt) {
        return readNonBlank(prompt);
    }
}
