import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.NumberFormatter;

import java.awt.*;
import java.awt.event.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class View extends JFrame implements ModelObserver{
    private JPanel center; //dove sta la lista scrollabile dei file 
    private JPanel bottom; //dove stanno i bottoni
    private Controller controller;
    private List<JButton> downloadButtons;
    private String sizes[];

    public View(Controller controller) {
        this.controller = controller;
        setTitle("TFTP Client");
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                int response = JOptionPane.showConfirmDialog(null, "Are you sure you want to quit?", "Exit", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (response == JOptionPane.YES_OPTION) { //metti il controllo sulla chiusura se ci sono operazioni in corso
                    controller.close();
                    dispose();
                }
            }
        });
        bottom = new JPanel();
        center = new JPanel();
        downloadButtons = new ArrayList<>();
        sizes= new String[3];
        sizes[0] = "b";
        sizes[1] = "kb";
        sizes[2] = "mb";
        setResizable(false);
    }

    public void initialize() {
        setVisible(false);
        getContentPane().removeAll();//potrebbe volerci un repaint dopo
        setJMenuBar(null);
        setLayout(new FlowLayout(FlowLayout.LEADING, 5, 10));
        setResizable(false);
        setLocationRelativeTo(null);
        setSize(300, 140);
        JLabel serverIp = new JLabel("Insert server IP:"); 
        add(serverIp);
        JFormattedTextField formattedIpFields[] = new JFormattedTextField[4];
        NumberFormat format = NumberFormat.getInstance();
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(0);
        format.setMinimumIntegerDigits(1);
        format.setGroupingUsed(false);
        NumberFormatter formatter = new NumberFormatter(format) {
            @Override
            public Object stringToValue(String text) throws ParseException {
                if (text.isBlank()) return null; //serve per permettere di cancellare tutto il contenuto nel JFormattedTextField
                return super.stringToValue(text);
            }
        };
        formatter.setValueClass(Integer.class);
        formatter.setMinimum(0);
        formatter.setMaximum(255);
        formatter.setAllowsInvalid(false);
        formatter.setCommitsOnValidEdit(true);
        for (int i = 0; i < formattedIpFields.length; i++) {
            formattedIpFields[i] = new JFormattedTextField(formatter);
            formattedIpFields[i].setColumns(3);
            add(formattedIpFields[i]);
            add(new JLabel("."));
        }
        getContentPane().remove(getContentPane().getComponent(getContentPane().getComponentCount()-1));

        JLabel portnum = new JLabel("Insert server port:"); 
        add(portnum);

        NumberFormatter pFormatter = new NumberFormatter(format) {
            @Override
            public Object stringToValue(String text) throws ParseException {
                if (text.isBlank()) return null; //serve per permettere di cancellare tutto il contenuto nel JFormattedTextField
                return super.stringToValue(text);
            }
        };
        pFormatter.setValueClass(Integer.class);
        pFormatter.setMinimum(0);
        pFormatter.setMaximum(65535);
        pFormatter.setAllowsInvalid(false);
        pFormatter.setCommitsOnValidEdit(true);
        JFormattedTextField portField = new JFormattedTextField(pFormatter);
        portField.setColumns(5);
        add(portField);

        JButton confirmButton = new JButton("Connect");
        confirmButton.setBorder(BorderFactory.createEmptyBorder(10, 120, 10, 120));
        add(confirmButton);

        confirmButton.addActionListener((ActionEvent e) -> {
            String ipAddress = "";
            for (int i = 0; i < formattedIpFields.length; i++) {
                if (formattedIpFields[i].getText().isBlank()) {
                    notifyErrorObserver("one or more ip fields cannot be empty");
                    return;
                }
                ipAddress+=formattedIpFields[i].getText()+".";
            }
            ipAddress = ipAddress.substring(0, ipAddress.length()-1);
            if (!portField.getText().isBlank()) {
                try {
                    controller.connectToServer(InetAddress.getByName(ipAddress), Integer.parseInt(portField.getText()));
                    initializeConnectedUI();
                } catch (UnknownHostException e1) {
                    e1.printStackTrace();
                }
            } else {
                notifyErrorObserver("the port field cannot be empty");
            }

        });
        setVisible(true);
    }

    public void initializeConnectedUI() {
        setVisible(false);
        getContentPane().removeAll();
        setTitle("TFTP Client");
        setLayout(new BorderLayout());
        bottom.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "Current operation status", TitledBorder.LEFT, TitledBorder.TOP));
        center = new JPanel(new GridLayout(0, 1));
        JScrollPane scrollPane = new JScrollPane(center);
        add(scrollPane, BorderLayout.CENTER);
        JMenuBar menu = new JMenuBar();
        JMenuItem newConnection = new JMenuItem("Connect to a new server");
        newConnection.setMaximumSize(new Dimension(newConnection.getPreferredSize().width, newConnection.getMaximumSize().height));
        newConnection.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        newConnection.addActionListener((ActionEvent e) -> {
            initialize();
        });
        JMenuItem uploadButton = new JMenuItem("Upload file");
        uploadButton.setMaximumSize(new Dimension(uploadButton.getPreferredSize().width, uploadButton.getMaximumSize().height));
        uploadButton.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        uploadButton.addActionListener((ActionEvent e) -> {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                controller.sendWriteRequest(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });

        JMenuItem refreshButton = new JMenuItem("Refresh file list");
        refreshButton.setMaximumSize(new Dimension(refreshButton.getPreferredSize().width, refreshButton.getMaximumSize().height));
        refreshButton.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        refreshButton.addActionListener((ActionEvent e) -> {
            controller.sendListRequest();
        });
        
        menu.add(uploadButton);
        menu.add(newConnection);
        menu.add(refreshButton);
        setJMenuBar(menu);
        add(bottom, BorderLayout.SOUTH);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    @Override
    public void notifyErrorObserver(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
        if (msg=="Lost connection to server") {
            initialize();
        }
    }

    @Override
    public void update(String[] fileNames, Integer[] filesizes) {
        downloadButtons.clear();
        center.removeAll();
        ImageIcon fileIcon = new ImageIcon("./icon.png");
        System.out.println(fileIcon.getIconWidth());
        for (int i = 0; i < fileNames.length; i++) {
            JPanel entry = new JPanel(new GridLayout(1, 2));
            int j = 0;
            for (; j < 3 && filesizes[i]>1000; j++) {
                filesizes[i]/=1000;
            }
            JLabel file = new JLabel(fileNames[i] + " - " + Integer.toString(filesizes[i])+sizes[j], fileIcon, JLabel.LEFT);
            JButton downloadButton = new JButton("Download");
            String currentFilename = fileNames[i];
            downloadButton.addActionListener((ActionEvent e) -> {
                for (int k = 0; k < downloadButtons.size(); k++) {
                    downloadButtons.get(k).setEnabled(false);
                }
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setAcceptAllFileFilterUsed(true);
                if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    // scarica file nella directory data
                    System.out.println();
                    controller.sendReadRequest(currentFilename, fileChooser.getSelectedFile().getAbsolutePath()+"/");
                }
            });
            entry.setBorder(BorderFactory.createEmptyBorder(40, 50, 40, 50));
            entry.add(file);
            entry.add(downloadButton);
            entry.setPreferredSize(new Dimension(300, 150));
            center.add(entry);
        }
        center.repaint();
        center.revalidate();
    }

    @Override
    public void notifyStatusObserver(int percenteage, String currentOperation, String currentlyTransferingFileName) {
        Graphics g = bottom.getGraphics();            
        g.setColor(Color.BLACK);
        g.drawRect(this.getWidth()-420, 15, 400, 15);
        g.drawString(currentOperation + " " + currentlyTransferingFileName, 8, 28);
        g.setColor(Color.GREEN);
        g.fillRect(this.getWidth()-420, 15, 400*percenteage/100, 15);
        if (percenteage>=100) {
            JOptionPane.showMessageDialog(this, "Operation terminated succesfully", "Operation", JOptionPane.INFORMATION_MESSAGE);
            g.clearRect(0, 15, bottom.getWidth(), bottom.getHeight());

        }
    }
}
