public interface ModelObserver {
    void update(String[] fileNames, Integer[] filesizes);
    void notifyStatusObserver(int percenteage, String currentOperation, String currentlyTransferingFileName);
    void notifyErrorObserver(String msg);
    void initialize();
    void initializeConnectedUI();
}
