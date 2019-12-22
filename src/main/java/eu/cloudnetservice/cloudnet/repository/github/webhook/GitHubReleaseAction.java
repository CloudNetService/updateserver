package eu.cloudnetservice.cloudnet.repository.github.webhook;

import eu.cloudnetservice.cloudnet.repository.github.GitHubReleaseInfo;

public class GitHubReleaseAction extends GitHubWebHookAction {

    private GitHubReleaseInfo release;

    public GitHubReleaseAction(String action, GitHubReleaseInfo release) {
        super(action);
        this.release = release;
    }

    public GitHubReleaseInfo getRelease() {
        return this.release;
    }
}
