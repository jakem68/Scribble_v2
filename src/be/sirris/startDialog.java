package be.sirris;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



import java.net.ServerSocket;
import java.net.Socket;

import static be.sirris.MakeScribble.*;

public class startDialog extends JFrame {
    public static String _pictures = "/home/jan/Pictures";
    public static File _defaultScribbleSettings = new File("/home/jan/Pictures/_defaultScribbleSettings.set");
    public static File _tempScribbleSettings = new File("/home/jan/Pictures/_tempScribbleSettings.set");
    //TODO: Remove fileList if no further use then to print system out.
    private static ArrayList<File> fileList = new ArrayList<File>();
    private static String newline = "\n";
    private static File[] selectedFiles;
    private static String[] selectedFilesNames;
    public static boolean _settingsFileExists;
    // slider min and max settings
    double maxThreshold = 0.5;
    double minThreshold = 0.02;
    double maxGray_Resolution = 1000;
    double minGray_Resolution = 50;
    double maxScale = 0.4;
    double minScale = 0.1;
    private JPanel contentPane;
    private JButton selectFilesButton;
    private JButton buttonCancel;
    private JTextArea textArea1;
    private JList list1;
    private JButton testScribbleButton;
    private JSlider thresholdSlider;
    private JLabel threshold;
    private JSlider grayResSlider;
    private JLabel grayResolution;
    private JSlider lineWeightSlider;
    private JLabel lineWeight;
    private JButton defaultButton;
    private JButton saveSettingsButton;
    private JButton setNewDefaultButton;
    private JButton scribbleButton;
    private JButton pageSetupButton;
    private JFileChooser fc;

    private static final String UR10_IP = "192.168.2.100";
    private static final int PORT = 30000;
    public static PrintWriter out = null;
    public static Socket server = null;
    public static int _drawingCounter = 0;

    public startDialog(String title) {
        pack();

        setContentPane(contentPane);
        getRootPane().setDefaultButton(selectFilesButton);
        contentPane.setPreferredSize(new Dimension(600, 400));

// when cross is clicked
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

//on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

//on Cancel
        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

//on Select Files
        selectFilesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fileChooser();
            }
        });

//on Scribble
        scribbleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onScribble();
            }
        });

//on Test Scribble
        testScribbleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    onTestScribble();
                } catch (IOException e1) {
                    e1.printStackTrace();
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        });

//on Default
        defaultButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    onDefault();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

//on Set new default
        setNewDefaultButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onNewDefault();
            }
        });

//on Save Settings
        saveSettingsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onSaveSettings();
            }
        });

//on select file in list1 Jlist panel
        list1.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                try {
                    onSelectPicture();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

//on thresholdSliderChange
        thresholdSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                onSliderChange();
            }
        });

//on grayResolutionSliderChange
        grayResSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                onSliderChange();
            }
        });

//on lineWeightSliderChange
        lineWeightSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                onSliderChange();
            }
        });

//on pageSetup
        pageSetupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onPageSetup();
            }
        });
    }

    private void onPageSetup() {

        pageSetup.run();

    }


    public static void setOriginalDefaults() {
        THRESHOLD = THRESHOLD_ORIGINAL;
        GRAY_RESOLUTION = GRAY_RESOLUTION_ORIGINAL;
        SCALE = SCALE_ORIGINAL;
    }

    //parse incoming file and set variables THRESHOLD, GRAY_RESOLUTION, SCALE
    public static void parseAndSetScribbleSettings(File sfile) throws IOException {
        // create a Buffered Reader object instance with a FileReader
        BufferedReader br = new BufferedReader(new FileReader(sfile));

        // read the first line from the text file
        String fileRead = br.readLine();

        int i = 2;
        int j = 3;
        //String[] lineParsed = new String[i];
        String[][] fileParsed = new String[j][i];
        j = 0;
        // loop until all lines are read
        while (fileRead != null) {
            // use string.split to load a string array with the values from each line of
            // the file, using a comma as the delimiter
            String[] lineParsed = fileRead.split("=");
            fileParsed[j] = lineParsed;
            j += 1;

            // read next line before looping
            // if end of file reached
            fileRead = br.readLine();
        }
        // close file stream
        br.close();

        // set Variables
        THRESHOLD = Double.parseDouble(fileParsed[0][1]);
        System.out.println(THRESHOLD);
        GRAY_RESOLUTION = Double.parseDouble(fileParsed[1][1]);
        SCALE = Double.parseDouble(fileParsed[2][1]);
    }

    private static void deleteFile(Path path) {
        try {
            Files.delete(path);
        } catch (NoSuchFileException x) {
            System.err.format("%s: no such" + " file or directory%n", path);
        } catch (DirectoryNotEmptyException x) {
            System.err.format("%s not empty%n", path);
        } catch (IOException x) {
            // File permission problems are caught here.
            System.err.println(x);
        }
    }

    private void onCancel() {
// add your code here if necessary
        System.out.println("TAG: I am here 1");
//        dispose();
        try {
            out.close();
            server.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally {
            System.exit(0);
        }
    }

    private void onSliderChange() {
        createTempSettingsFile();
//        contentPane.transferFocusBackward();
    }

    private void onScribble() {
        //turn of Robot communication
        MakeScribble.ROBOT = true;
        ScribbleRunnable scribbleRunnable = new ScribbleRunnable();
        Thread t = new Thread(scribbleRunnable);
        t.start();
    }

    private void onTestScribble() throws IOException, InterruptedException {
        //turn of Robot communication
        MakeScribble.ROBOT = false;
        System.out.println("TAG: I am here 2");

        ScribbleRunnable scribbleRunnable = new ScribbleRunnable();
        Thread t = new Thread(scribbleRunnable);
        t.start();
    }

    private void onSelectPicture() throws IOException {
        //check whether specific settingsfile exists
//        String selected = (String) list1.getSelectedValue();
        if (_tempScribbleSettings.exists()) {
            deleteFile(_tempScribbleSettings.toPath());
        }

        int nrSelected = list1.getSelectedIndices().length;
        if (nrSelected < 1) {
            saveSettingsButton.setEnabled(false);
            lineWeightSlider.setEnabled(false);
            grayResSlider.setEnabled(false);
            thresholdSlider.setEnabled(false);
        }
        if (nrSelected == 1) {
            saveSettingsButton.setEnabled(true);
            lineWeightSlider.setEnabled(true);
            grayResSlider.setEnabled(true);
            thresholdSlider.setEnabled(true);
            String selected = (String) list1.getSelectedValue();
            determineApplicableSettingsfile_AndParse(selected);
            System.out.println(selected);
            setSliders();
            saveSettingsButton.setEnabled(true);
        }

        if (nrSelected > 1) {
            saveSettingsButton.setEnabled(false);
            lineWeightSlider.setEnabled(false);
            grayResSlider.setEnabled(false);
            thresholdSlider.setEnabled(false);
            if (_tempScribbleSettings.exists()) {
                deleteFile(_tempScribbleSettings.toPath());
            }
            int selectedIx = list1.getLeadSelectionIndex();
            String selected = (String) list1.getModel().getElementAt(selectedIx);
            System.out.println(selected);
        }
    }

    private void onDefault() throws IOException {
        //parse default settings file and set values in MakeScribble
        parseAndSetScribbleSettings(_defaultScribbleSettings);
        //adjust sliders accordingly: set sliders based on values in MakeScribble
        setSliders();
        if (_tempScribbleSettings.exists()) {
            deleteFile(_tempScribbleSettings.toPath());
        }
    }

    private void onNewDefault() {
        String settingsFilename = _defaultScribbleSettings.getPath();

        //read the values from the sliders and return a list of strings containing the content for the .set file
        List<String> lines = readSliders();

        //write the List of Strings to the settings file
        writeSettingsfile(settingsFilename, lines);
    }

    private void onSaveSettings() {
        String selected = (String) list1.getSelectedValue();
        String settingsFilename = selected + ".set";

        //read the values from the sliders and return a list of strings containing the content for the .set file
        List<String> lines = readSliders();

        //write the List of strings to the settings file
        writeSettingsfile(settingsFilename, lines);
    }

    private void fileChooser() {
        //Set up the file chooser.
        if (fc == null) {
            fc = new JFileChooser();

            //Add a custom file filter and disable the default
            //(Accept All) file filter.
            fc.addChoosableFileFilter(new ImageFilter());
            fc.setAcceptAllFileFilterUsed(false);

            //Add custom icons for file types.
            fc.setFileView(new ImageFileView());

            //Add the preview pane.
            fc.setAccessory(new ImagePreview(fc));

            fc.setMultiSelectionEnabled(true);
            fc.setCurrentDirectory(new File(_pictures));

        }
        //Show it.
        int returnVal = fc.showDialog(startDialog.this,
                "Attach");

        //Process the results.
        if (returnVal == JFileChooser.APPROVE_OPTION) {

            selectedFiles = fc.getSelectedFiles();
            for (File f : selectedFiles) {
                textArea1.append("Attaching file: " + f.getName()
                        + "." + newline);
                fileList.add(f);
            }
        } else {
            textArea1.append("Attachment cancelled by user." + newline);
        }

        //Place the names of the selected files in a String Array to pass as argument to MakeScribble.main()
        selectedFilesNames = new String[selectedFiles.length];
        for (int i = 0; i < selectedFiles.length; i++) {
            selectedFilesNames[i] = selectedFiles[i].getAbsolutePath();
            System.out.println(selectedFilesNames[i]);
        }
        //Place selected files names in Jlist list1
        list1.setListData(selectedFilesNames);

        textArea1.setCaretPosition(textArea1.getDocument().getLength());
        System.out.println(fileList);

        //Reset the file chooser for the next time it's shown.
        fc.setSelectedFile(null);
    }

    private void determineApplicableSettingsfile_AndParse(String iFile) throws IOException {
        String applicableSettingsFile;

        String settingsFilename = iFile + ".set";
        File settingsFile = new File(settingsFilename);
        _settingsFileExists = settingsFile.exists();

        //set slider values to either tempSettingsFile, specific settings file or default file
        if (_tempScribbleSettings.exists()) {
            parseAndSetScribbleSettings(_tempScribbleSettings);
            System.out.println(1);
        } else {
            if (_settingsFileExists) {
                parseAndSetScribbleSettings(settingsFile);
                System.out.println(2);
            } else {
                if (_defaultScribbleSettings.exists()) {
                    parseAndSetScribbleSettings(_defaultScribbleSettings);
                    System.out.println(3);
                } else {
                    setOriginalDefaults();
                    System.out.println(4);
                }
            }
        }
    }

    public List<String> readSliders() {
        double tempThreshold;
        double tempGray_Resolution;
        double tempScale;
        double sliderThreshold;
        double sliderGray_Resolution;
        double sliderScale;

        //read values from sliders

        sliderThreshold = (double) thresholdSlider.getValue();
        sliderGray_Resolution = (double) grayResSlider.getValue();
        sliderScale = (double) lineWeightSlider.getValue();

        //recalculate to desired min and max values
        tempThreshold = (sliderThreshold / 100 * (maxThreshold - minThreshold)) + minThreshold;
        tempGray_Resolution = (sliderGray_Resolution / 100 * (maxGray_Resolution - minGray_Resolution)) + minGray_Resolution;
        tempScale = (sliderScale / 100 * (maxScale - minScale)) + minScale;

        // prepare String List for writing
        List<String> lines = Arrays.asList("Threshold = " + tempThreshold, "Gray resolution = " + tempGray_Resolution, "Scale = " + tempScale);
        return lines;
    }

//    private static void setScribblerSettings(String ifilename) throws IOException {
//        //check whether there is a settings file with the name filename+".set" in /home/jan/Pictures
//        File sfile = new File(ifilename + ".set");
//        boolean sfileExists = sfile.exists();
//        boolean tempFileExists = startDialog._tempScribbleSettings.exists();
//        if (tempFileExists) {
//            parseAndSetScribbleSettings(startDialog._tempScribbleSettings);
//            System.out.println("TAG: using tempfile settings");
//            deleteFile(Paths.get(String.valueOf(startDialog._tempScribbleSettings)));
//        } else if (sfileExists) {
//            //if yes use the values in this file
//            //HERE
//            // https://www.reddit.com/r/javaexamples/comments/344kch/reading_and_parsing_data_from_a_file/
//            parseAndSetScribbleSettings(sfile);
//            System.out.println("TAG: using image settings file");
//        } else {
//            //if no use the values in _defaultScribbleSettings.set
//            boolean defaultFileExists = startDialog._defaultScribbleSettings.exists();
//            if (defaultFileExists) {
//                parseAndSetScribbleSettings(startDialog._defaultScribbleSettings);
//                System.out.println("TAG: using default settings file");
//            }
//            //if no file exist use the original default values
//            else {
//
//                setOriginalDefaults();
//                System.out.println("TAG: using original settings");
//            }
//        }
//    }

    private void writeSettingsfile(String settingsFilename, List<String> lines) {
        Path file = Paths.get(settingsFilename);
        try {
            Files.write(file, lines, Charset.forName("UTF-8"));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    private void setSliders() {

        //recalculate to slidervalues
        int tempThreshold = (int) ((THRESHOLD - minThreshold) * 100 / (maxThreshold - minThreshold));
        int tempGrayGray_Resolution = (int) ((GRAY_RESOLUTION - minGray_Resolution) * 100 / (maxGray_Resolution - minGray_Resolution));
        int tempScale = (int) ((SCALE - minScale) * 100 / (maxScale - minScale));

        thresholdSlider.setValue(tempThreshold);
        grayResSlider.setValue(tempGrayGray_Resolution);
        lineWeightSlider.setValue(tempScale);
    }

    private void createTempSettingsFile() {

        if (list1.getSelectedIndices().length == 1) {
            String settingsFilename = _tempScribbleSettings.getPath();

            //read the values from the sliders and return a list of strings containing the content for the .set file
            List<String> lines = readSliders();
            //write the List of Strings to the settings file
            writeSettingsfile(settingsFilename, lines);

            //TODO: return focus to previous
        }
    }
private void startServer() {
//TAG:server
    try {
        ServerSocket listener = new ServerSocket(PORT);
        server = listener.accept();
        out = new PrintWriter(server.getOutputStream(), true);
        System.out.println(
                "Generic Network Server: got connection from " + server.getInetAddress().getHostName() + "\n");
        listener.close();
    } catch (IOException ioe) {
        System.out.println("IOException: " + ioe);
        ioe.printStackTrace();
    }
//TAG:server
    }

    public class ScribbleRunnable implements Runnable {

        public void run() {

            startServer();

            // code in the other thread, can reference "var" variable
            //create array of strings to pass as argument to MakeScribble with  length 1
            System.out.println("trying to start new worker");

            String scribbleArg[] = new String[1];

            //check whether something has been selected
            if (list1.getSelectedValue() != null) {

                //get the indices of the selected items and run through them
                int selectedIx[] = list1.getSelectedIndices();
                for (int i = 0; i < selectedIx.length; i++) {
                    //get item based on index
                    Object sel = list1.getModel().getElementAt(selectedIx[i]);
                    //place the string representation in scribbleArg to pass to MakeScribble
                    scribbleArg[0] = ((String) sel);

                    //pass the values as arguments to MakeScribble
                    try {
                        determineApplicableSettingsfile_AndParse(scribbleArg[0]);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
//reduce to 1 main only --> call MakeScribble.run instead of MakeScribble.main
                        MakeScribble.run(scribbleArg);
                        setSliders();
//                    MakeScribble.main(scribbleArg);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                _drawingCounter = 0;
            } else {
                JOptionPane.showMessageDialog(null, "There is nothing selected.");
            }
        }
    }
    private static void createGUI() {
        startDialog dialog = new startDialog("Scribble");
        dialog.setContentPane(new startDialog("Scribble").contentPane);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.pack();
        dialog.setVisible(true);
    }
    public static void main(String[] args) {

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createGUI();
            }
        });
    }
}
