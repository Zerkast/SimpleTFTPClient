
public interface ICommandConsumer {
    void receiveData(int sequenceNumber, byte[] data);
    void receiveAck(int sequenceNumber);
    void receiveFileInList(int sequenceNumber, String filename, int filesize);
    void receiveError(String errorMessage);
} 
