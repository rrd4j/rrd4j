package org.rrd4j.inspector;

import org.rrd4j.ConsolFun;
import org.rrd4j.core.ArcDef;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

class EditArchiveDialog extends JDialog {
    private static final int FIELD_SIZE = 20;
    private static final String TITLE_NEW = "New archive";
    private static final String TITLE_EDIT = "Edit archive";

    private JLabel consolFunLabel = new JLabel("Consolidation function: ");
    private JLabel xffLabel = new JLabel("X-files factor: ");
    private JLabel stepsLabel = new JLabel("Steps: ");
    private JLabel rowsLabel = new JLabel("Rows: ");

    private JComboBox consolFunCombo = new JComboBox();
    private JTextField xffField = new JTextField(FIELD_SIZE);
    private JTextField stepsField = new JTextField(FIELD_SIZE);
    private JTextField rowsField = new JTextField(FIELD_SIZE);

    private JButton okButton = new JButton("OK");
    private JButton cancelButton = new JButton("Cancel");

    private ArcDef arcDef;

    EditArchiveDialog(Frame parent, ArcDef arcDef) {
        super(parent, arcDef == null ? TITLE_NEW : TITLE_EDIT, true);
        constructUI(arcDef);
        pack();
        Util.centerOnScreen(this);
        setVisible(true);
    }

    private void constructUI(ArcDef arcDef) {
        // fill controls
        for (ConsolFun fun : ConsolFun.values()) {
            consolFunCombo.addItem(fun);
        }
        consolFunCombo.setSelectedIndex(0);
        if (arcDef == null) {
            // NEW
            xffField.setText("" + 0.5);
        }
        else {
            // EDIT
            consolFunCombo.setSelectedItem(arcDef.getConsolFun());
            consolFunCombo.setEnabled(false);
            xffField.setText("" + arcDef.getXff());
            stepsField.setText("" + arcDef.getSteps());
            stepsField.setEnabled(false);
            rowsField.setText("" + arcDef.getRows());
            // rowsField.setEnabled(false);
        }

        // layout
        JPanel content = (JPanel) getContentPane();
        GridBagLayout layout = new GridBagLayout();
        content.setLayout(layout);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        layout.setConstraints(consolFunLabel, gbc);
        content.add(consolFunLabel);
        gbc.gridy = 1;
        layout.setConstraints(xffLabel, gbc);
        content.add(xffLabel);
        gbc.gridy = 2;
        layout.setConstraints(stepsLabel, gbc);
        content.add(stepsLabel);
        gbc.gridy = 3;
        layout.setConstraints(rowsLabel, gbc);
        content.add(rowsLabel);
        gbc.gridy = 4;
        layout.setConstraints(okButton, gbc);
        okButton.setPreferredSize(cancelButton.getPreferredSize());
        content.add(okButton);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        layout.setConstraints(consolFunCombo, gbc);
        content.add(consolFunCombo);
        gbc.gridy = 1;
        layout.setConstraints(xffField, gbc);
        content.add(xffField);
        gbc.gridy = 2;
        layout.setConstraints(stepsField, gbc);
        content.add(stepsField);
        gbc.gridy = 3;
        layout.setConstraints(rowsField, gbc);
        content.add(rowsField);
        gbc.gridy = 4;
        layout.setConstraints(cancelButton, gbc);
        content.add(cancelButton);
        getRootPane().setDefaultButton(okButton);

        // actions
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    private void ok() {
        arcDef = createArcDef();
        if (arcDef != null) {
            close();
        }
    }

    private void close() {
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    private void cancel() {
        close();
    }

    private ArcDef createArcDef() {
        ConsolFun consolFun = (ConsolFun) consolFunCombo.getSelectedItem();
        double xff;
        try {
            xff = Double.parseDouble(xffField.getText());
            if (xff < 0 || xff >= 1D) {
                throw new NumberFormatException();
            }
        }
        catch (NumberFormatException nfe) {
            Util.error(this, "X-files factor must be a number not less than 0.0 and less than 1.0");
            return null;
        }
        int steps;
        try {
            steps = Integer.parseInt(stepsField.getText());
            if (steps <= 0) {
                throw new NumberFormatException();
            }
        }
        catch (NumberFormatException nfe) {
            Util.error(this, "Number of steps must be a positive integer");
            return null;
        }
        int rows;
        try {
            rows = Integer.parseInt(rowsField.getText());
            if (rows <= 0) {
                throw new NumberFormatException();
            }
        }
        catch (NumberFormatException nfe) {
            Util.error(this, "Number of rows must be a positive integer");
            return null;
        }
        try {
            return new ArcDef(consolFun, xff, steps, rows);
        }
        catch (Exception e) {
            // should not be here ever!
            Util.error(this, e);
            return null;
        }
    }

    ArcDef getArcDef() {
        return arcDef;
    }
}
