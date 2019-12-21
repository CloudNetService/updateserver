package eu.cloudnetservice.cloudnet.repository.github;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Date;

@EqualsAndHashCode
@ToString
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
}
