package org.logevents.observers.batch;

import org.junit.Before;
import org.junit.Test;
import org.logevents.LogEvent;
import org.slf4j.event.Level;

import java.time.Duration;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class BatchThrottlerTest {

    private Scheduler mockExecutor = mock(Scheduler.class);
    private LogEventBatchProcessor mockProcessor = mock(LogEventBatchProcessor.class);
    private BatchThrottler batcher = new BatchThrottler(mockExecutor, mockProcessor)
            .setThrottle(Arrays.asList(Duration.ofMinutes(1), Duration.ofMinutes(5)));

    @Before
    public void shouldSetAction() {
        verify(mockExecutor).setAction(any());
    }

    @Test
    public void shouldScheduleFirstEventForImmediateExecution() {
        batcher.logEvent(createLogEvent());
        verify(mockExecutor).schedule(Duration.ZERO);
    }

    @Test
    public void shouldSendOnFlush() {
        LogEvent logEvent = createLogEvent();
        batcher.logEvent(logEvent);
        batcher.flush();
        verify(mockProcessor).processBatch(new LogEventBatch().add(logEvent));
    }

    @Test
    public void shouldDelaySubsequentEvents() {
        batcher.logEvent(createLogEvent());
        verify(mockExecutor).schedule(Duration.ZERO);
        batcher.flush();

        batcher.logEvent(createLogEvent());
        verify(mockExecutor).schedule(Duration.ofMinutes(1));
        batcher.logEvent(createLogEvent());
        verifyNoMoreInteractions(mockExecutor);
    }

    @Test
    public void shouldIncreaseDelay() {
        batcher.logEvent(createLogEvent());
        verify(mockExecutor).schedule(Duration.ZERO);

        batcher.flush();
        batcher.logEvent(createLogEvent());
        verify(mockExecutor).schedule(Duration.ofMinutes(1));

        batcher.flush();
        batcher.logEvent(createLogEvent());
        verify(mockExecutor).schedule(Duration.ofMinutes(5));

        batcher.flush();
        verify(mockExecutor).schedule(Duration.ofMinutes(5));
    }


    private LogEvent createLogEvent() {
        return new LogEvent(getClass().getName(), Level.WARN, "Test");
    }

}