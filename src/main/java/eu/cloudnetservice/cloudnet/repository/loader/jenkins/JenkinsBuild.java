package eu.cloudnetservice.cloudnet.repository.loader.jenkins;

public class JenkinsBuild {

    private String _class;
    private int number;
    private String url;

    public JenkinsBuild(String _class, int number, String url) {
        this._class = _class;
        this.number = number;
        this.url = url;
    }

    public String getJobClass() {
        return this._class;
    }

    public int getNumber() {
        return this.number;
    }

    public String getUrl() {
        return this.url;
    }

    public String getApiUrl() {
        return this.getUrl() + "api/json/";
    }

}
