package org.logevents.status;

import org.junit.Test;
import org.logevents.config.Configuration;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LogEventStatusTest {

    private LogEventStatus instance = LogEventStatus.getInstance();

    @Test
    public void shouldConfigureLogEventStatusLevelForClass() {
        Properties properties = new Properties();
        properties.setProperty("logevents.status", "INFO");
        instance.configure(new Configuration(properties, "logevents"));
        assertEquals(StatusEvent.StatusLevel.INFO, instance.getThreshold(this));

        properties.setProperty("logevents.status.LogEventStatusTest", "DEBUG");
        instance.configure(new Configuration(properties, "logevents"));
        assertEquals(StatusEvent.StatusLevel.DEBUG, instance.getThreshold(this));
    }

    @Test
    public void shouldListTailMessage() {
        instance.clear();
        assertNull(instance.lastMessage());
        instance.addTrace(this, "Dummy message");
        assertEquals("Dummy message", instance.lastMessage().getMessage());
        for (int i=0; i<1000; i++) {
            instance.addTrace(this, "Dummy message");
        }
        instance.addTrace(this, "Tail message");
        assertEquals("Tail message", instance.lastMessage().getMessage());
        instance.clear();
    }
}