package be.sirris;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class startDialog extends JDialog {
    private JPanel contentPane;
    private JButton selectFilesButton;
    private JButton buttonCancel;
    private JTextArea textArea1;
    private JList list1;
    private JButton testScribbleButton;
    private JSlider tresholdSlider;
    private JLabel darkness;
    private JSlider grayResSlider;
    private JLabel grayResolution;
    private JSlider lineWeightSlider;
    private JLabel lineWeight;
    private JButton defaultButton;
    private JButton saveSettingsButton;
    private JButton setNewDefaultButton;
    private JButton scribbleButton;
    private JFileChooser fc;
    public static File defaultScribbleSettings = new File("/home/jan/Pictures/defaultScribbleSettings.set");

//TODO: Remove fileList if no further use then to print.
    private static ArrayList<File> fileList = new ArrayList<File>();
    private static String newline = "\n";
    private static File[] selectedFiles;
    private static String[] selectedFilesNames;

//    static {
//        // System.loadLibrary("opencv_java246");
//        System.loadLibrary("opencv_java300");
//    }


    public startDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(selectFilesButton);
        contentPane.setPreferredSize(new Dimension(600, 400));


        selectFilesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onSelectFiles();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        testScribbleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {onTestScribble();}
        });


// call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

// call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onSelectFiles() {
// add your code here
        fileChooser();
    }

    private void onCancel() {
// add your code here if necessary
        dispose();
    }

    private void onTestScribble() {
// add your code here if necessary
        //select the desired file name for testing
        String scribbleArg[] = new String[1];
        scribbleArg[0] = ((String) list1.getSelectedValue());
        try {
            MakeScribble.main(scribbleArg);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


//TODO: why is Jpane in MakeScribble not showing as before? + Jpane can not be closed as before

        //change the "Scribble parameters" for testing

        //click button "test Scribble" to view scribble result

        //save the right settings of "Scribble parameters" +
        // have possibility to set these "Scribble parameters" as new default




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
            fc.setCurrentDirectory(new File("/home/jan/Pictures"));
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

    public static void main(String[] args) {
        startDialog dialog = new startDialog();
        dialog.pack();
        dialog.setVisible(true);


        System.exit(0);
    }
}
