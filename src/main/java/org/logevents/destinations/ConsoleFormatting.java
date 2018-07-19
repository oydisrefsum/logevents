package org.logevents.destinations;

import java.util.Locale;

import org.fusesource.jansi.AnsiConsole;

public class ConsoleFormatting {

    private static ConsoleFormatting instance;

    public synchronized static ConsoleFormatting getInstance() {
        if (instance == null) {
            if (!isWindows() || isUnixShell()) {
                instance = new ConsoleFormatting();
            } else {
                try {
                    AnsiConsole.systemInstall();
                    instance = new ConsoleFormatting();
                } catch (NoClassDefFoundError e) {
                    instance = nullConsoleFormatting();
                }
            }
        }
        return instance;
    }

    private static ConsoleFormatting nullConsoleFormatting() {
        return new ConsoleFormatting() {
            @Override
            protected String ansi(String s, String code) {
                return s;
            }
        };
    }

    private static boolean isUnixShell() {
        return System.getenv("PWD") != null && System.getenv("PWD").startsWith("/");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");
    }

    public String bold(String s) {
        return ansi(s, "1;");
    }

    public String red(String s) {
        return ansi(s, "31");
    }

    public String green(String s) {
        return ansi(s, "32");
    }

    public String yellow(String s) {
        return ansi(s, "33");
    }

    public String blue(String s) {
        return ansi(s, "34");
    }

    protected String ansi(String s, String code) {
        return String.format("\033[%sm%s\033[m", code, s);
    }
}