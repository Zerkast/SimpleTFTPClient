import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Sender {
    private final DatagramSocket socket;
    private DatagramPacket packet;

    public Sender(DatagramSocket socket) {
        this.socket = socket;
    }
    
    public void send(String data, InetAddress address, int port){
        send(data.getBytes(),address,port);
    }
    
    public void send(byte [] data, InetAddress address, int port){
        try {
            packet=new DatagramPacket(data,data.length,address, port);
            socket.send(packet);
        } catch (SocketException ex ) {
            throw new RuntimeException(ex);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }        
    }
}
