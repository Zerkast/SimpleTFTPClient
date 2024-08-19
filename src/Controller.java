import java.net.InetAddress;

public class Controller {
    private ClientModel model;

    public Controller(ClientModel model) {
        this.model = model;
    }

    public void sendWriteRequest(String filepath) {
        model.sendWriteRequest(filepath, 1);
    }

    public void sendReadRequest(String filename, String localDestination) {
        model.sendReadRequest(filename, localDestination, 1);
    }

    public void sendListRequest() {
        model.sendListRequest(1);
    }

    public void close() {
        synchronized(this) {
            Thread.currentThread().interrupt();
        }
    }

    public void connectToServer(InetAddress ipAddress, int port) {
        model.connectToServer(ipAddress, port);
    }

    
}
