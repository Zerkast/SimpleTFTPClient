import java.lang.reflect.Array;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.Arrays;


public class SenderProtocolManager {
    private Sender sender;
    private InetAddress serverAddress;
    private int serverPort;

    public SenderProtocolManager(Sender sender) {
        this.sender = sender;
    }

    public void sendData(int sequenceNumber, byte[] data) {
        byte[] message = new byte[4 + data.length];
        message[0] = 0;
        message[1] = 3; 
        message[2] = (byte)(sequenceNumber >> 8);
        message[3] = (byte)(sequenceNumber);
        for (int i = 0; i < data.length; i++) {
            message[4+i] = data[i];
        }
        sender.send(message, serverAddress, serverPort);
    }

    public void sendFileListRequest() {
        byte[] message = new byte[2];
        message[0] = 0;
        message[1] = 6;
        sender.send(message, serverAddress, serverPort);
    }

    public void sendAck(int sequenceNumber) {
        byte[] message = new byte[4];
        message[0] = 0;
        message[1] = 4;
        message[2] = (byte)(sequenceNumber>>8);
        message[3] = (byte)(sequenceNumber);
        sender.send(message, serverAddress, serverPort);
    }

    public void sendReadRequest(String filename) {
        byte[] stringInBytes = (filename + "\0"+"octet\0").getBytes(); 
        byte[] message = new byte[stringInBytes.length+2];
        message[0] = 0;
        message[1] = 1;
        for (int i = 0; i < stringInBytes.length; i++) {
            message[i+2]=stringInBytes[i];
        }
        sender.send(message, serverAddress, serverPort);
    }

    public void sendWriteRequest(String filename) {
        byte[] stringInBytes = (filename + "\0"+"octet\0").getBytes(); 
        byte[] message = new byte[stringInBytes.length+2];
        message[0] = 0;
        message[1] = 2;
        for (int i = 0; i < stringInBytes.length; i++) {
            message[i+2]=stringInBytes[i];
        }
        sender.send(message, serverAddress, serverPort);
    }

    public void setServerAddress(InetAddress serverAddress) {
        this.serverAddress = serverAddress;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }
    private int byteArrayToInt(byte[] data) {
        int value = 0;
        for (byte b : data) {
            value = (value << 8) + (b & 0xFF);
        }
        return value;
    }
}
