package pl.klamborowski.jacksongenerator;

import javax.swing.*;
import java.awt.event.*;

public class GenerateDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextPane typeMainClassNameTextPane;
    private JTextPane pasteYourJsonStringTextPane;
    private JTextArea jsonStringTextArea;
    private JCheckBox generateORMLiteDatabaseFiledAnnotationCheckBox;
    private JTextField mainClassNameTextField;
    private OnDialogCloseListener onDialogCloseListener;


    public GenerateDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK(e);
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

    // not used, instead use show
    public static void main(String[] args) {
        GenerateDialog dialog = new GenerateDialog();
        dialog.pack();
        dialog.setVisible(true);
//        System.exit(0);
    }

    public static void show(OnDialogCloseListener onDialogCloseListener) {
        GenerateDialog dialog = new GenerateDialog();
        dialog.setOnDialogCloseListener(onDialogCloseListener);
        dialog.setLocationRelativeTo(null);
        dialog.pack();
        dialog.setVisible(true);

    }

    private void onOK(ActionEvent event) {
        if (onDialogCloseListener != null) {
            onDialogCloseListener.onDialogClose(mainClassNameTextField.getText(),
                    jsonStringTextArea.getText(),
                    generateORMLiteDatabaseFiledAnnotationCheckBox.isSelected());
        }
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    public void setOnDialogCloseListener(OnDialogCloseListener onDialogCloseListener) {
        this.onDialogCloseListener = onDialogCloseListener;
    }
}
