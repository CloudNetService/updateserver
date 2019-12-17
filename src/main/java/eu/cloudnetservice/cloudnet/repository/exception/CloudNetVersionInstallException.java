package eu.cloudnetservice.cloudnet.repository.exception;

public class CloudNetVersionInstallException extends RuntimeException {
    public CloudNetVersionInstallException(String message) {
        super(message);
    }

    public CloudNetVersionInstallException(String message, Throwable cause) {
        super(message, cause);
    }
}
