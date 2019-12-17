package eu.cloudnetservice.cloudnet.repository.loader;

public class CloudNetVersionLoadException extends RuntimeException {

    private CloudNetVersionFileLoader fileLoader;

    public CloudNetVersionLoadException(String message, CloudNetVersionFileLoader fileLoader) {
        super(message);
        this.fileLoader = fileLoader;
    }

    public CloudNetVersionLoadException(String message, Throwable cause, CloudNetVersionFileLoader fileLoader) {
        super(message, cause);
        this.fileLoader = fileLoader;
    }

    public CloudNetVersionFileLoader getFileLoader() {
        return this.fileLoader;
    }
}
