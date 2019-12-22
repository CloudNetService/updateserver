package eu.cloudnetservice.cloudnet.repository.github;

import java.util.Date;
import java.util.Objects;

public class GitHubAuthorInfo {

    private String name;
    private String email;
    private Date date;

    public GitHubAuthorInfo(String name, String email, Date date) {
        this.name = name;
        this.email = email;
        this.date = date;
    }

    public Date getDate() {
        return this.date;
    }

    public String getEmail() {
        return this.email;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GitHubAuthorInfo that = (GitHubAuthorInfo) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(email, that.email) &&
                Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, email, date);
    }
}
