package be.sirris;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class pageSetup extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField textField1;
    private JTextField textField2;
    private JLabel referenceHeightLabel;
    private JLabel approachHeightLabel;
    private JLabel refToStartXLabel;
    private JLabel refToStartYLabel;
    private JLabel numberColumnsLabel;
    private JLabel numberRowsLabel;
    private JLabel deltaColumnLabel;
    private JLabel deltaRowLabel;
    private JLabel paperWidthLabel;
    private JLabel paperHeightLabel;
    private JLabel paperWhitebandLabel;

    public pageSetup(String title) {

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
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

    private void onOK() {
// add your code here
        dispose();
    }

    private void onCancel() {
// add your code here if necessary
        dispose();
    }

    public static void run() {
        createGUIPageSetup();
    }

    private static void createGUIPageSetup() {
        pageSetup dialog = new pageSetup("Page Setup");
//        dialog.setContentPane(new pageSetup("Page Setup").contentPane);
//        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
