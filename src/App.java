import java.net.DatagramSocket;

public class App {
    public static void main(String[] args) throws Exception {
        DatagramSocket server = new DatagramSocket(5000);
        Receiver rcv = new Receiver(server, 1024);
        Sender snd = new Sender(server);
        SenderProtocolManager spm = new SenderProtocolManager(snd);
        ClientModel clientModel = new ClientModel(spm);
        ReceiverProtocolManager receiveManager = new ReceiverProtocolManager(clientModel);
        rcv.setConsumer(receiveManager);
        Controller controller = new Controller(clientModel);
        ModelObserver obs = new View(controller);
        clientModel.addObserver(obs);
    }
}
