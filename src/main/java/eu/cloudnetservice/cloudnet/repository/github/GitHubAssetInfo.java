package eu.cloudnetservice.cloudnet.repository.github;

import com.google.gson.annotations.SerializedName;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Date;

@EqualsAndHashCode
@ToString
public class GitHubAssetInfo {

    private String url;
    private long id;
    @SerializedName("node_id")
    private String nodeId;
    private GitHubUserInfo uploader;
    @SerializedName("content_type")
    private String contentType;
    private String state;
    private long size;
    @SerializedName("download_count")
    private long downloadCount;
    @SerializedName("created_at")
    private Date createdAt;
    @SerializedName("updated_at")
    private Date updatedAt;
    @SerializedName("browser_download_url")
    private String downloadUrl;

    public GitHubAssetInfo(String url, long id, String nodeId, GitHubUserInfo uploader, String contentType,
                           String state, long size, long downloadCount, Date createdAt, Date updatedAt,
                           String downloadUrl) {
        this.url = url;
        this.id = id;
        this.nodeId = nodeId;
        this.uploader = uploader;
        this.contentType = contentType;
        this.state = state;
        this.size = size;
        this.downloadCount = downloadCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.downloadUrl = downloadUrl;
    }

    public String getUrl() {
        return this.url;
    }

    public long getId() {
        return this.id;
    }

    public String getNodeId() {
        return this.nodeId;
    }

    public GitHubUserInfo getUploader() {
        return this.uploader;
    }

    public String getContentType() {
        return this.contentType;
    }

    public String getState() {
        return this.state;
    }

    public long getSize() {
        return this.size;
    }

    public long getDownloadCount() {
        return this.downloadCount;
    }

    public Date getCreatedAt() {
        return this.createdAt;
    }

    public Date getUpdatedAt() {
        return this.updatedAt;
    }

    public String getDownloadUrl() {
        return this.downloadUrl;
    }
}
