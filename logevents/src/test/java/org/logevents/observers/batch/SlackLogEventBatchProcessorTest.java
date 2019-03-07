package org.logevents.observers.batch;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.Test;
import org.logevents.LogEvent;
import org.logevents.observers.SlackLogEventObserver;
import org.logevents.status.LogEventStatus;
import org.logevents.status.StatusEvent;
import org.logevents.status.StatusEvent.StatusLevel;
import org.slf4j.event.Level;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("restriction")
public class SlackLogEventBatchProcessorTest {

    public static class Formatter extends SlackLogEventsFormatter {
        @Override
        protected List<Map<String, Object>> createAttachments(LogEventBatch batch) {
            return null;
        }

        @Override
        protected String createText(LogEventGroup mainGroup) {
            return mainGroup.headMessage().formatMessage();
        }
    }

    private List<String> buffer = new ArrayList<>();

    @Test
    public void shouldSendSlackMessage() throws IOException {
        HttpServer server = startServer(t -> {
                    buffer.add(toString(t.getRequestBody()));
                    t.sendResponseHeaders(200, 0);
                    t.close();
                });
        int port = server.getAddress().getPort();

        Properties properties = new Properties();
        properties.setProperty("observer.slack.slackUrl", "http://localhost:" + port);
        properties.setProperty("observer.slack.slackLogEventsFormatter", Formatter.class.getName());
        properties.setProperty("observer.slack.username", "loguser");
        properties.setProperty("observer.slack.channel", "general");

        SlackLogEventObserver observer = new SlackLogEventObserver(properties, "observer.slack");

        LogEvent logEvent = new LogEvent("org.example", Level.WARN, "Nothing");
        observer.getBatchProcessor().processBatch(new LogEventBatch().add(logEvent));

        assertEquals(Arrays.asList("{\n"
                + "  \"username\": \"loguser\",\n" + "  \"channel\": \"general\",\n"
                + "  \"attachments\": null,\n"+ "  \"text\": \"Nothing\"\n" +  "}"),
            buffer);
    }

    @Test
    public void shouldHandleErrors() throws IOException {
        LogEventStatus.getInstance().setThreshold(StatusLevel.FATAL);
        HttpServer server = startServer(t -> {
            t.sendResponseHeaders(400, 0);
            t.getResponseBody().write("A detailed error message".getBytes());
            t.getResponseBody().flush();
            t.close();
        });
        int port = server.getAddress().getPort();

        URL url = new URL("http://localhost:" + port);
        SlackLogEventObserver observer = new SlackLogEventObserver(url, Optional.empty(), Optional.empty());
        LogEvent logEvent = new LogEvent("org.example", Level.WARN, "Nothing");
        observer.getBatchProcessor().processBatch(new LogEventBatch().add(logEvent));

        List<StatusEvent> events = LogEventStatus.getInstance().getHeadMessages(observer.getBatchProcessor(), StatusLevel.ERROR);
        assertTrue("Expected 1 event, was " + events, events.size() == 1);
        assertEquals("Failed to send slack message", events.get(0).getMessage());
        assertEquals("Failed to POST to " + url + ", status code: 400: A detailed error message",
                events.get(0).getThrowable().getMessage());
    }

    @Test
    public void shouldConfigureSlackObserver() throws MalformedURLException {
        Properties properties = new Properties();
        properties.put("observer.slack.slackUrl", "http://localhost:1234");
        properties.put("observer.slack.channel", "general");
        properties.put("observer.slack.username", "MyTestApp");
        properties.put("observer.slack.sourceCode.0.package", "org.logevents");
        properties.put("observer.slack.sourceCode.0.maven", "org.logevents/logevents");
        properties.put("observer.slack.sourceCode.1.package", "org.junit");
        properties.put("observer.slack.sourceCode.1.github", "https://github.com/junit-team/junit4");

        SlackLogEventObserver observer = new SlackLogEventObserver(properties, "observer.slack");

        assertEquals("SlackLogEventObserver{batchProcessor=HttpPostJsonBatchProcessor{jsonMessageFormatter=SlackLogEventsFormatter{username=MyTestApp,channel=general},url=http://localhost:1234}}",
                observer.toString());
    }

    private HttpServer startServer(HttpHandler httpHandler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", httpHandler);
        server.start();
        return server;
    }

    private String toString(InputStream input) {
        return new BufferedReader(new InputStreamReader(input))
                .lines().collect(Collectors.joining("\n"));
    }
}
