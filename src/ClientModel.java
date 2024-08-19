import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ClientModel implements ICommandConsumer {
    private SenderProtocolManager spm;
    private HashMap<String, Integer> filesOnServerInfo;
    private List<ModelObserver> observers;
    private Timer timeoutHandler;
    private String currentPath;
    private String currentFilename;
    private long currentyTransferingFileSize;
    private int currentSequenceNum;
    private boolean wasLastPiece;
    private boolean isOperationFinished; //TODO: fare un bottone che permette di riconettersi dopo aver perso la connessione


    public ClientModel(SenderProtocolManager spm) {
        this.spm = spm;
        filesOnServerInfo = new HashMap<>();
        currentyTransferingFileSize = 0;
        currentSequenceNum = 0;
        observers = new ArrayList<>();
        timeoutHandler = new Timer();
        wasLastPiece = false;
        isOperationFinished = false;
    }

    public void sendReadRequest(String filename, String localDestination, int attemptNumber) {
        if (attemptNumber==5) {
            for (ModelObserver observer : observers) {
                observer.notifyErrorObserver("Lost connection to server");
                observer.initialize();
            }
        } else {
            isOperationFinished = false;
            currentFilename = filename;
            currentPath = localDestination;
            currentSequenceNum=0;
            spm.sendReadRequest(filename);
            TimerTask ttr = new TimerTask() {
                @Override
                public void run() {
                    if (currentSequenceNum==0 && !isOperationFinished) {
                        sendReadRequest(filename, localDestination, attemptNumber+1);
                    }
                }
            };
            timeoutHandler.schedule(ttr, 1000);
        }
    }

    public void sendWriteRequest(String filepath, int attemptNumber) {
        if (attemptNumber==5) {
            for (ModelObserver observer : observers) {
                observer.notifyErrorObserver("Lost connection to server");
                observer.initialize();
            }
        } else {
            currentPath = filepath;
            currentFilename = filepath.split("/")[filepath.split("/").length-1];
            currentSequenceNum = 0;
            isOperationFinished = false;
            spm.sendWriteRequest(currentFilename);
            TimerTask ttw = new TimerTask() {
                @Override
                public void run() {
                    if (currentSequenceNum==0 && !isOperationFinished) {
                        sendWriteRequest(filepath, attemptNumber+1);
                    }
                }
            };
            timeoutHandler.schedule(ttw, 1000);
        }
    }

    @Override
    public void receiveData(int sequenceNumber, byte[] data) {
        if (sequenceNumber<currentSequenceNum) {
           spm.sendAck(sequenceNumber); 
        } else {
            try {
                RandomAccessFile stream = new RandomAccessFile(currentPath+currentFilename, "rw");
                stream.seek((sequenceNumber-1)*512);
                stream.write(data,0,512);
                stream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            currentSequenceNum++;
            int percenteage = (currentSequenceNum-1)*512*100/filesOnServerInfo.get(currentFilename);
            if (percenteage>=100) { 
                sequenceNumber = 0;
                currentFilename = "";
                isOperationFinished = true;
            }
            int lastSequenceNum = sequenceNumber;
            TimerTask ttd = new TimerTask() {
                @Override
                public void run() {
                    if (currentSequenceNum==lastSequenceNum+1 && !isOperationFinished) {
                        for (ModelObserver observer : observers) {
                            observer.notifyErrorObserver("Lost connection to server");
                        }
                    }
                }
            };
            timeoutHandler.schedule(ttd, 5000);
            spm.sendAck(sequenceNumber);
            for (ModelObserver observer : observers) {
                observer.notifyStatusObserver(percenteage, "downloading", currentFilename);
            }
            // try {
            //     Thread.sleep(400); //per vedere che la barra del progresso nella view funziona correttamente
            // } catch (InterruptedException e) {
            //     e.printStackTrace();
            // }
        }
    }

    @Override
    public void receiveAck(int sequenceNumber) {
        if (sequenceNumber==0) {
            try {
                currentyTransferingFileSize = Files.size(Paths.get(currentPath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        currentSequenceNum++;
        for (ModelObserver observer : observers) {
            observer.notifyStatusObserver((int)((512*(currentSequenceNum-1))*100/currentyTransferingFileSize), "uploading",currentFilename);
        }
        if (wasLastPiece) {
            currentSequenceNum = 0;
            isOperationFinished = true;
        } else {
            sendData(1); 
        } 
    }

    @Override
    public void receiveFileInList(int sequenceNumber, String filename, int filesize) {
        if (sequenceNumber<currentSequenceNum) {
            spm.sendAck(sequenceNumber);
            return;
        }
        if (sequenceNumber==1) {
            filesOnServerInfo.clear();
            currentSequenceNum = 1;
        }
        if (filename=="") {
            isOperationFinished = true;
            String[] filenames = new String[filesOnServerInfo.keySet().size()];
            System.out.println("Ora stampo la lista dei file a schermo");
            for (ModelObserver observer : observers) {
                observer.update(filesOnServerInfo.keySet().toArray(new String[filesOnServerInfo.size()]), filesOnServerInfo.values().toArray(new Integer[filesOnServerInfo.size()]));
            }
            spm.sendAck(sequenceNumber);
            currentSequenceNum=0;
            return;
        }
        int lastSequenceNum = sequenceNumber;
        TimerTask ttl = new TimerTask() {
            @Override
            public void run() {
                if (currentSequenceNum==lastSequenceNum+1 && !isOperationFinished) {
                    for (ModelObserver observer : observers) {
                        observer.notifyErrorObserver("Lost connection to server");
                    }
                }
            }
        };
        timeoutHandler.schedule(ttl, 5000);
        currentSequenceNum++;
        spm.sendAck(sequenceNumber);
        filesOnServerInfo.put(filename, filesize);
    }

    public void sendData(int attemptNumber) {
        if (attemptNumber==5) {
            for (ModelObserver modelObserver : observers) {
                modelObserver.notifyErrorObserver("Lost connection to server");
                currentSequenceNum = 0;
            }
        } else {
            byte[] buffer = new byte[512]; 
            RandomAccessFile stream;
            try {
                stream = new RandomAccessFile(currentPath, "r");
                stream.seek((currentSequenceNum-1)*512);
                int bytesNum = stream.read(buffer, 0, 512);
                if (bytesNum<512) {
                    wasLastPiece = true;
                }
                spm.sendData(currentSequenceNum, buffer);
                int lastSequenceNum = currentSequenceNum;
                TimerTask tt = new TimerTask() {
                    @Override
                    public void run() {
                        if (currentSequenceNum==lastSequenceNum && !isOperationFinished) {
                            sendData(attemptNumber+1);
                        }
                    }  
                };
                timeoutHandler.schedule(tt, 800);
                stream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void receiveError(String errorMessage) {
        isOperationFinished = true;
        for (ModelObserver modelObserver : observers) {
            modelObserver.notifyErrorObserver(errorMessage);
        }
    }

    public void sendListRequest(int attemptNumber) {
        if (attemptNumber!=5) {
            spm.sendFileListRequest();
            currentSequenceNum = 0;
            TimerTask ttl = new TimerTask() {
                int lastSequenceNum = currentSequenceNum;
                @Override
                public void run() {
                    if (currentSequenceNum==lastSequenceNum&&!isOperationFinished) {
                        sendListRequest(attemptNumber+1);
                    }
                }  
            };
            timeoutHandler.schedule(ttl, 400);
        } else {
            for (ModelObserver modelObserver : observers) {
                modelObserver.notifyErrorObserver("Lost connection to server");
            }
        }
    }
    
    public void addObserver(ModelObserver modelObserver) {
        observers.add(modelObserver);
        modelObserver.initialize();
    }

    public void connectToServer(InetAddress ipAddress, int port) {
        spm.setServerAddress(ipAddress);
        spm.setServerPort(port);
        for (ModelObserver modelObserver : observers) {
            modelObserver.initializeConnectedUI();
        }
        sendListRequest(1);
    }
}
