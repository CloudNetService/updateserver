package eu.cloudnetservice.cloudnet.repository.github;

import com.google.gson.annotations.SerializedName;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Date;

@EqualsAndHashCode
@ToString
public class GitHubReleaseInfo {

    private String url;
    @SerializedName("assets_url")
    private String assetsUrl;
    @SerializedName("upload_url")
    private String uploadUrl;
    @SerializedName("html_url")
    private String htmlUrl;
    private long id;
    @SerializedName("node_id")
    private String nodeId;
    @SerializedName("tag_name")
    private String tagName;
    @SerializedName("target_commitish")
    private String targetCommitish;
    private String name;
    private boolean draft;
    private GitHubUserInfo author;
    @SerializedName("prerelease")
    private boolean preRelease;
    @SerializedName("created_at")
    private Date createdAt;
    @SerializedName("published_at")
    private Date publishedAt;
    private GitHubAssetInfo[] assets;
    @SerializedName("tarball_url")
    private String tarballUrl;
    @SerializedName("zipball_url")
    private String zipballUrl;
    private String body;

    public GitHubReleaseInfo(String url, String assetsUrl, String uploadUrl, String htmlUrl, long id, String nodeId,
                             String tagName, String targetCommitish, String name, boolean draft, GitHubUserInfo author,
                             boolean preRelease, Date createdAt, Date publishedAt, GitHubAssetInfo[] assets,
                             String tarballUrl, String zipballUrl, String body) {
        this.url = url;
        this.assetsUrl = assetsUrl;
        this.uploadUrl = uploadUrl;
        this.htmlUrl = htmlUrl;
        this.id = id;
        this.nodeId = nodeId;
        this.tagName = tagName;
        this.targetCommitish = targetCommitish;
        this.name = name;
        this.draft = draft;
        this.author = author;
        this.preRelease = preRelease;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
        this.assets = assets;
        this.tarballUrl = tarballUrl;
        this.zipballUrl = zipballUrl;
        this.body = body;
    }

    public String getUrl() {
        return this.url;
    }

    public String getAssetsUrl() {
        return this.assetsUrl;
    }

    public String getUploadUrl() {
        return this.uploadUrl;
    }

    public String getHtmlUrl() {
        return this.htmlUrl;
    }

    public long getId() {
        return this.id;
    }

    public String getNodeId() {
        return this.nodeId;
    }

    public String getTagName() {
        return this.tagName;
    }

    public String getTargetCommitish() {
        return this.targetCommitish;
    }

    public String getName() {
        return this.name;
    }

    public boolean isDraft() {
        return this.draft;
    }

    public GitHubUserInfo getAuthor() {
        return this.author;
    }

    public boolean isPreRelease() {
        return this.preRelease;
    }

    public Date getCreatedAt() {
        return this.createdAt;
    }

    public Date getPublishedAt() {
        return this.publishedAt;
    }

    public GitHubAssetInfo[] getAssets() {
        return this.assets;
    }

    public String getTarballUrl() {
        return this.tarballUrl;
    }

    public String getZipballUrl() {
        return this.zipballUrl;
    }

    public String getBody() {
        return this.body;
    }
}
