package eu.cloudnetservice.cloudnet.repository.github;

import com.google.gson.annotations.SerializedName;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public class GitHubCommitInfo {

    private GitHubAuthorInfo author;
    private GitHubAuthorInfo committer;
    private String message;
    private String url;
    @SerializedName("comment_count")
    private int commentCount;

    public GitHubCommitInfo(GitHubAuthorInfo author, GitHubAuthorInfo committer, String message, String url, int commentCount) {
        this.author = author;
        this.committer = committer;
        this.message = message;
        this.url = url;
        this.commentCount = commentCount;
    }

    public GitHubAuthorInfo getAuthor() {
        return this.author;
    }

    public GitHubAuthorInfo getCommitter() {
        return this.committer;
    }

    public String getMessage() {
        return this.message;
    }

    public String getUrl() {
        return this.url;
    }

    public int getCommentCount() {
        return this.commentCount;
    }
}
