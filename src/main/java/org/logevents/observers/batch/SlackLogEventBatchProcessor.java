package org.logevents.observers.batch;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.logevents.status.LogEventStatus;
import org.logevents.util.JsonUtil;
import org.logevents.util.NetUtils;

public class SlackLogEventBatchProcessor implements LogEventBatchProcessor {

    private Optional<String> username = Optional.empty();
    private Optional<String> channel = Optional.empty();
    private URL slackUrl;
    private SlackLogMessageFactory slackLogMessageFactory;

    public SlackLogEventBatchProcessor(URL url) {
        this.slackUrl = url;
        slackLogMessageFactory = new SlackLogMessageFactory();
    }

    public SlackLogEventBatchProcessor(Properties properties, String prefix) throws MalformedURLException {
        setUsername(properties.getProperty(prefix + ".username"));
        setChannel(properties.getProperty(prefix + ".channel"));
        this.slackUrl = new URL(properties.getProperty(prefix + ".slackUrl"));
        LogEventStatus.getInstance().addInfo(this, "Configured " + prefix);
        slackLogMessageFactory = new SlackLogMessageFactory();
    }

    public void setUsername(String username) {
        this.username = Optional.ofNullable(username);
    }

    public void setChannel(String channel) {
        this.channel = Optional.ofNullable(channel);
    }

    public void setSlackUrl(URL slackUrl) {
        this.slackUrl = slackUrl;
    }

    @Override
    public void processBatch(List<LogEventGroup> batch) {
        Map<String, Object> slackMessage;
        try {
            slackMessage = slackLogMessageFactory.createSlackMessage(batch, username, channel);
        } catch (Exception e) {
            LogEventStatus.getInstance().addFatal(this, "Runtime error generating slack message", e);
            return;
        }
        try {
            NetUtils.postJson(slackUrl, JsonUtil.toIndentedJson(slackMessage));
        } catch (IOException e) {
            LogEventStatus.getInstance().addError(this, "Failed to send slack message", e);
            return;
        }
    }

}
