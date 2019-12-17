package eu.cloudnetservice.cloudnet.repository.github;

import com.google.gson.annotations.SerializedName;

public class GitHubUserInfo {

    private String login;
    private long id;
    @SerializedName("node_id")
    private String nodeId;
    @SerializedName("gravatar_id")
    private String gravatarId;
    private String url;
    @SerializedName("html_url")
    private String htmlUrl;
    @SerializedName("followers_url")
    private String followersUrl;
    @SerializedName("following_url")
    private String followingUrl;
    @SerializedName("gists_url")
    private String gistsUrl;
    @SerializedName("starred_url")
    private String starredUrl;
    @SerializedName("subscriptions_url")
    private String subscriptionsUrl;
    @SerializedName("organizations_url")
    private String organizationsUrl;
    @SerializedName("repos_url")
    private String reposUrl;
    @SerializedName("events_url")
    private String eventsUrl;
    @SerializedName("received_events_url")
    private String receivedEventsUrl;
    private String type;
    @SerializedName("site_admin")
    private boolean siteAdmin;

    public GitHubUserInfo(String login, long id, String nodeId, String gravatarId, String url, String htmlUrl,
                          String followersUrl, String followingUrl, String gistsUrl, String starredUrl,
                          String subscriptionsUrl, String organizationsUrl, String reposUrl, String eventsUrl,
                          String receivedEventsUrl, String type, boolean siteAdmin) {
        this.login = login;
        this.id = id;
        this.nodeId = nodeId;
        this.gravatarId = gravatarId;
        this.url = url;
        this.htmlUrl = htmlUrl;
        this.followersUrl = followersUrl;
        this.followingUrl = followingUrl;
        this.gistsUrl = gistsUrl;
        this.starredUrl = starredUrl;
        this.subscriptionsUrl = subscriptionsUrl;
        this.organizationsUrl = organizationsUrl;
        this.reposUrl = reposUrl;
        this.eventsUrl = eventsUrl;
        this.receivedEventsUrl = receivedEventsUrl;
        this.type = type;
        this.siteAdmin = siteAdmin;
    }

    public String getLogin() {
        return this.login;
    }

    public long getId() {
        return this.id;
    }

    public String getNodeId() {
        return this.nodeId;
    }

    public String getGravatarId() {
        return this.gravatarId;
    }

    public String getUrl() {
        return this.url;
    }

    public String getHtmlUrl() {
        return this.htmlUrl;
    }

    public String getFollowersUrl() {
        return this.followersUrl;
    }

    public String getFollowingUrl() {
        return this.followingUrl;
    }

    public String getGistsUrl() {
        return this.gistsUrl;
    }

    public String getStarredUrl() {
        return this.starredUrl;
    }

    public String getSubscriptionsUrl() {
        return this.subscriptionsUrl;
    }

    public String getOrganizationsUrl() {
        return this.organizationsUrl;
    }

    public String getReposUrl() {
        return this.reposUrl;
    }

    public String getEventsUrl() {
        return this.eventsUrl;
    }

    public String getReceivedEventsUrl() {
        return this.receivedEventsUrl;
    }

    public String getType() {
        return this.type;
    }

    public boolean isSiteAdmin() {
        return this.siteAdmin;
    }
}
