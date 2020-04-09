package eu.cloudnetservice.cloudnet.repository.endpoint.discord;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;

import java.util.Optional;

public class DiscordPresence {

    private OnlineStatus status;
    private Activity.ActivityType activityType;
    private String activityText;
    private String streamingUrl;
    private long visibleTimeMillis;

    public DiscordPresence(OnlineStatus status, Activity.ActivityType activityType, String activityText, String streamingUrl, long visibleTimeMillis) {
        this.status = status;
        this.activityType = activityType;
        this.activityText = activityText;
        this.streamingUrl = streamingUrl;
        this.visibleTimeMillis = visibleTimeMillis;
    }

    public Optional<Activity> asActivity() {
        if (this.activityType == null || this.activityText == null) {
            return Optional.empty();
        }
        return Optional.of(Activity.of(this.activityType, this.activityText, this.streamingUrl));
    }

    public OnlineStatus getStatus() {
        return status;
    }

    public Activity.ActivityType getActivityType() {
        return activityType;
    }

    public String getActivityText() {
        return activityText;
    }

    public String getStreamingUrl() {
        return streamingUrl;
    }

    public long getVisibleTimeMillis() {
        return visibleTimeMillis;
    }
}
