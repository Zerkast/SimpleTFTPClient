import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class ReceiverProtocolManager implements IDataConsumer{

    private ICommandConsumer commandConsumer;
    private Map<Integer, String> errors;    

    public ReceiverProtocolManager(ICommandConsumer commandConsumer) {
        this.commandConsumer = commandConsumer;
        errors = new HashMap<>();
        errors.put(1, "File not found\0");
        errors.put(2, "Access violation\0");
        errors.put(3, "Disk full or allocation exceeded\0");
        errors.put(4, "Illegal TFTP operation\0");
        errors.put(5, "Unknown transfer ID\0");
        errors.put(6, "File already exists\0");
    }

    @Override
    public void consumeData(byte[] arg0, int arg1, InetAddress arg2, int arg3) {
        // System.out.println(new String(arg0));
        System.out.println("opcode " + ((arg0[0]&0xFF)<<8) + arg0[1]);
        System.out.println("num " + ((((arg0[2]&0xFF)<<8) + arg0[3])&0xFF));
        switch (((arg0[0]&0xFF)<<8) + arg0[1]) { //anche se il numero sta solo nel byte di destra, ho deciso di leggerli insieme per correttezza, visto che il protocollo prevede entrambi i byte
            case 3: //data
                commandConsumer.receiveData((((arg0[2]&0xFF)<<8) + arg0[3])&0xFF,Arrays.copyOfRange(arg0, 4, 516));
                break;
            case 4: //ack
                commandConsumer.receiveAck((((arg0[2]&0xFF)<<8) + arg0[3])&0xFF);
                break;
            case 5:
                commandConsumer.receiveError(errors.get(((arg0[2]&0xFF)<<8) + arg0[3]));
            case 7: //list response
                commandConsumer.receiveFileInList(((arg0[2]&0xFF)<<8) + arg0[3], stringReader(arg0, 8), byteArrayToInt(Arrays.copyOfRange(arg0, 4, 8))); 
                break;
        }
    }

    private String stringReader(byte[] byteArray, int beginIndex) {
        int end = beginIndex;
        for (; byteArray[end]!=0 && end < byteArray.length; end++);
        return new String(byteArray, StandardCharsets.UTF_8).substring(beginIndex, end);
    }
    private int byteArrayToInt(byte[] data) {
        int value = 0;
        for (byte b : data) {
            value = (value << 8) + (b & 0xFF);
        }
        return value;
    }

    // private int byteArrayDataLength(byte[] array) {
    //     int i = array.length-1;
    //     for (; i >= 0; i--) {
    //         if (array[i]!=0) {
    //             break;
    //         }     
    //     }
    //     return i;
    // }

}
