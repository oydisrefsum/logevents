package org.logevents;

import org.junit.Rule;
import org.junit.Test;
import org.logevents.extend.junit.LogEventRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

public class LogEventRuleTest {

    private Logger logger = LoggerFactory.getLogger("com.example.application.Service");

    @Rule
    public LogEventRule logEventRule = new LogEventRule("com.example", Level.DEBUG);

    @Test
    public void shouldCaptureSingleLogEvent() {
        logger.debug("Hello world");
        logEventRule.assertSingleMessage("Hello world", Level.DEBUG);
    }

    @Test
    public void shouldCaptureMultipleLogEvent() {
        logger.debug("Not this one");
        logger.debug("Hello world");
        logger.info("Hello world");
        logEventRule.assertContainsMessage("Hello world", Level.DEBUG);
    }

    @Test
    public void shouldSuppressLogEvent() {
        logger.error("Even though this is an error event, it is not displayed");
    }

    @Test
    public void shouldNotCollectEventsBelowThreshold() {
        logger.trace("Even though this is an error event, it is not displayed");
        logEventRule.assertNoMessages();
        logEventRule.assertDoesNotContainMessage("Even though this is an error event, it is not displayed");
    }
}
