package eu.cloudnetservice.cloudnet.repository.web;

public class APIAvailableResponse {
    private boolean available;

    public APIAvailableResponse(boolean available) {
        this.available = available;
    }

    public boolean isAvailable() {
        return this.available;
    }
}
