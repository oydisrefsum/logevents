package org.logevents.formatting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

public class ExceptionFormatterTest {

    private StackTraceElement mainMethod = new StackTraceElement("org.logeventsdemo.MyApplication", "main", "MyApplication.java", 20);
    private StackTraceElement publicMethod = new StackTraceElement("org.logeventsdemo.internal.MyClassName", "publicMethod", "MyClassName.java", 31);
    private StackTraceElement internalMethod = new StackTraceElement("org.logeventsdemo.internal.MyClassName", "internalMethod", "MyClassName.java", 311);
    private StackTraceElement nioApiMethod = new StackTraceElement("java.nio.file.Files", "write", "Files.java", 3292);
    private StackTraceElement nioInternalMethod = new StackTraceElement("sun.nio.fs.WindowsException", "translateToIOException", "WindowsException.java", 79);
    private StackTraceElement ioApiMethod = new StackTraceElement("java.io.FilterOutputStream", "close", "FilterOutputStream.java", 180);
    private StackTraceElement ioInternalMethod = new StackTraceElement("java.io.FileOutputStream", "close", "FileOutputStream.java", 323);

    private ExceptionFormatter formatter = new ExceptionFormatter();

    @Test
    public void shouldFormatStackTrace() {
        RuntimeException exception = new RuntimeException("This is an error message");
        exception.setStackTrace(new StackTraceElement[] {
                internalMethod, publicMethod, mainMethod
        });

        String[] lines = formatter.format(exception, 100).split("\r?\n");
        assertEquals("java.lang.RuntimeException: This is an error message", lines[0]);
        assertEquals("\tat org.logeventsdemo.internal.MyClassName.internalMethod(MyClassName.java:311)", lines[1]);
        assertEquals("\tat org.logeventsdemo.internal.MyClassName.publicMethod(MyClassName.java:31)", lines[2]);
        assertEquals("\tat org.logeventsdemo.MyApplication.main(MyApplication.java:20)", lines[3]);
        assertEquals(4, lines.length);
    }

    @Test
    public void shouldOutputNestedExceptions() {
        IOException ioException = new IOException("An IO exception happened");
        ioException.setStackTrace(new StackTraceElement[] {
                nioInternalMethod, nioApiMethod, internalMethod, publicMethod, mainMethod
        });

        RuntimeException exception = new RuntimeException("This is an error message", ioException);
        exception.setStackTrace(new StackTraceElement[] {
                internalMethod, publicMethod, mainMethod
        });

        String[] lines = formatter.format(exception, 100).split("\r?\n");

        assertEquals("java.lang.RuntimeException: This is an error message", lines[0]);
        assertEquals("\tat " + internalMethod, lines[1]);
        assertEquals("Caused by: " + ioException, lines[4]);
        assertEquals("\tat " + nioInternalMethod, lines[5]);
        assertEquals("\tat " + nioApiMethod, lines[6]);
        assertEquals("\t... 3 more", lines[7]);
    }


    @Test
    public void shouldOutputSuppressedExceptions() {
        IOException nested = new IOException("Nested");
        nested.setStackTrace(new StackTraceElement[] {
                nioInternalMethod, nioInternalMethod, internalMethod, publicMethod, mainMethod
        });
        IOException nestedSuppressed = new IOException("Nested suppressed");
        nestedSuppressed.setStackTrace(new StackTraceElement[] {
                ioApiMethod, nioInternalMethod, internalMethod, publicMethod, mainMethod
        });
        nested.addSuppressed(nestedSuppressed);
        IOException suppressedSuppressed = new IOException("Suppressed, suppressed");
        suppressedSuppressed.setStackTrace(new StackTraceElement[] {
                ioInternalMethod, ioApiMethod, nioInternalMethod, internalMethod, publicMethod, mainMethod
        });
        nestedSuppressed.addSuppressed(suppressedSuppressed);

        String[] lines = formatter.format(nested, 100).split("\r?\n");

        assertEquals(nested.toString(), lines[0]);
        assertEquals("\tat " + nioInternalMethod, lines[1]);

        assertEquals("\tSuppressed: " + nestedSuppressed, lines[6]);
        assertEquals("\t\tat " + ioApiMethod, lines[7]);
        assertEquals("\t\t... 4 more", lines[8]);

        assertEquals("\t\tSuppressed: " + suppressedSuppressed, lines[9]);
        assertEquals("\t\t\tat " + ioInternalMethod, lines[10]);
        assertEquals("\t\t\t... 5 more", lines[11]);
    }

    @Test
    public void shouldLimitStackTrace() {
        IOException nestedNested = new IOException("Nested nested");
        nestedNested.setStackTrace(new StackTraceElement[] {
                ioInternalMethod, ioApiMethod, nioInternalMethod, nioInternalMethod, internalMethod, publicMethod, mainMethod
        });
        IOException nested = new IOException("Nested", nestedNested);
        nested.setStackTrace(new StackTraceElement[] {
                ioApiMethod, nioInternalMethod, nioInternalMethod, internalMethod, publicMethod, mainMethod
        });
        RuntimeException exception = new RuntimeException("This is an error message", nested);
        exception.setStackTrace(new StackTraceElement[] {
                internalMethod, publicMethod, mainMethod
        });

        String[] lines = formatter.format(exception, 2).split("\r?\n");

        assertEquals(exception.toString(), lines[0]);
        assertEquals("\tat " + internalMethod, lines[1]);
        assertEquals("\tat " + publicMethod, lines[2]);
        assertEquals("Caused by: " + nested, lines[3]);
        assertEquals("\tat " + ioApiMethod, lines[4]);
        assertEquals("\tat " + nioInternalMethod, lines[5]);
        assertEquals("Caused by: " + nestedNested, lines[6]);
        assertEquals("\tat " + ioInternalMethod, lines[7]);
        assertEquals("\t... 6 more", lines[8]);
        assertEquals(1 + 2 + 1 + 2 + 1 + 2, lines.length);
    }

    @Test
    public void shouldFilterStackTrace() {
        IOException exceptions = new IOException("Nested nested");
        exceptions.setStackTrace(new StackTraceElement[] {
                ioInternalMethod, ioApiMethod,
                nioInternalMethod, nioInternalMethod, nioInternalMethod, nioInternalMethod, nioInternalMethod,
                internalMethod, publicMethod, mainMethod
        });

        formatter.setPackageFilter(new String[] {
                "sun.nio.fs", "java.nio"
        });
        String[] lines = formatter.format(exceptions, 4).split("\r?\n");

        assertEquals(exceptions.toString(), lines[0]);
        assertEquals("\tat " + ioInternalMethod, lines[1]);
        assertEquals("\tat " + ioApiMethod, lines[2]);
        assertEquals("\tat " + internalMethod + " [5 skipped]", lines[3]);
        assertEquals("\tat " + publicMethod, lines[4]);
        assertEquals(1+4, lines.length);
    }

    @Test
    public void shouldOutputFinalIgnoredLineCount() {
        IOException exceptions = new IOException("Nested nested");
        exceptions.setStackTrace(new StackTraceElement[] {
                ioInternalMethod, ioApiMethod,
                nioInternalMethod, nioInternalMethod, nioInternalMethod, nioInternalMethod, nioInternalMethod
        });

        formatter.setPackageFilter(new String[] {
                "sun.nio.fs", "java.nio"
        });
        String[] lines = formatter.format(exceptions, 100).split("\r?\n");

        assertEquals(exceptions.toString(), lines[0]);
        assertEquals("\tat " + ioInternalMethod, lines[1]);
        assertEquals("\tat " + ioApiMethod, lines[2]);
        assertEquals("[5 skipped]", lines[3]);
        assertEquals(4, lines.length);

    }


    @Test
    public void shouldFindPackagingInformation() throws IOException, URISyntaxException {
        RuntimeException exception = new RuntimeException("Something wen wrong");
        StackTraceElement[] stackTrace = new StackTraceElement[] {
            new StackTraceElement("sun.nio.fs.WindowsFileSystemProvider", "newByteChannel", "WindowsFileSystemProvider.java", 230),
            new StackTraceElement("java.nio.file.Files", "write", "Files.java", 3292),


            new StackTraceElement("org.logevents.formatting.ExceptionFormatterTest", "shouldFindPackagingInformation", "ExceptionFormatterTest.java", 175),
            new StackTraceElement("org.logevents.formatting.NoSuchClass", "unknownMethod", "NoSuchClass.java", 17),
            new StackTraceElement("org.junit.runners.model.FrameworkMethod$1", "runReflectiveCall", "FrameworkMethod.java", 50),
            new StackTraceElement("org.junit.internal.runners.model.ReflectiveCallable", "run", ".ReflectiveCallable.java", 12),
        };
        exception.setStackTrace(stackTrace);

        formatter.setIncludePackagingData(true);

        String[] lines = formatter.format(exception, 100).split("\r?\n");

        String javaVersion = System.getProperty("java.version");

        assertEquals(exception.toString(), lines[0]);
        assertEquals("\tat " + stackTrace[0] + " [rt.jar:" + javaVersion + "]", lines[1]);
        assertEquals("\tat " + stackTrace[1] + " [rt.jar:" + javaVersion + "]", lines[2]);
        assertEquals("\tat " + stackTrace[2] + " [test-classes:na]", lines[3]);
        assertEquals("\tat " + stackTrace[3] + " [na:na]", lines[4]);
        assertEquals("\tat " + stackTrace[4] + " [junit-4.12.jar:4.12]", lines[5]);
        assertEquals("\tat " + stackTrace[5] + " [junit-4.12.jar:4.12]", lines[6]);
    }


}