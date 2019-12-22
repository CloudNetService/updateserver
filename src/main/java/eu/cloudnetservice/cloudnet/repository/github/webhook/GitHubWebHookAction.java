package eu.cloudnetservice.cloudnet.repository.github.webhook;

public abstract class GitHubWebHookAction {

    private String action;

    public GitHubWebHookAction(String action) {
        this.action = action;
    }

    public String getAction() {
        return this.action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
