package org.rrd4j.inspector;

import org.rrd4j.DsType;
import org.rrd4j.core.DsDef;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

class EditDatasourceDialog extends JDialog {
    private static final int FIELD_SIZE = 20;
    private static final String TITLE_NEW = "New datasource";
    private static final String TITLE_EDIT = "Edit datasource";

    private JLabel nameLabel = new JLabel("Datasource name: ");
    private JLabel typeLabel = new JLabel("Datasource type: ");
    private JLabel heartbeatLabel = new JLabel("Heartbeat: ");
    private JLabel minLabel = new JLabel("Min value: ");
    private JLabel maxLabel = new JLabel("Max value: ");

    private JTextField nameField = new JTextField(FIELD_SIZE);
    private JComboBox typeCombo = new JComboBox();
    private JTextField heartbeatField = new JTextField(FIELD_SIZE);
    private JTextField minField = new JTextField(FIELD_SIZE);
    private JTextField maxField = new JTextField(FIELD_SIZE);

    private JButton okButton = new JButton("OK");
    private JButton cancelButton = new JButton("Cancel");

    private DsDef dsDef;

    EditDatasourceDialog(Frame parent, DsDef dsDef) {
        super(parent, dsDef == null ? TITLE_NEW : TITLE_EDIT, true);
        constructUI(dsDef);
        pack();
        Util.centerOnScreen(this);
        setVisible(true);
    }

    protected JRootPane createRootPane() {
        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                setVisible(false);
            }
        };
        JRootPane rootPane = new JRootPane();
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        rootPane.registerKeyboardAction(actionListener, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
        return rootPane;
    }

    private void constructUI(DsDef dsDef) {
        // fill controls
        for (DsType type : DsType.values()) {
            typeCombo.addItem(type);
        }
        typeCombo.setSelectedIndex(0);
        if (dsDef == null) {
            // NEW
            minField.setText("U");
            maxField.setText("U");
        }
        else {
            // EDIT
            nameField.setText(dsDef.getDsName());
            nameField.setEnabled(false);
            typeCombo.setSelectedItem(dsDef.getDsType());
            typeCombo.setEnabled(false);
            heartbeatField.setText("" + dsDef.getHeartbeat());
            minField.setText("" + dsDef.getMinValue());
            maxField.setText("" + dsDef.getMaxValue());
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
        layout.setConstraints(nameLabel, gbc);
        content.add(nameLabel);
        gbc.gridy = 1;
        layout.setConstraints(typeLabel, gbc);
        content.add(typeLabel);
        gbc.gridy = 2;
        layout.setConstraints(heartbeatLabel, gbc);
        content.add(heartbeatLabel);
        gbc.gridy = 3;
        layout.setConstraints(minLabel, gbc);
        content.add(minLabel);
        gbc.gridy = 4;
        layout.setConstraints(maxLabel, gbc);
        content.add(maxLabel);
        gbc.gridy = 5;
        layout.setConstraints(okButton, gbc);
        okButton.setPreferredSize(cancelButton.getPreferredSize());
        content.add(okButton);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        layout.setConstraints(nameField, gbc);
        content.add(nameField);
        gbc.gridy = 1;
        layout.setConstraints(typeCombo, gbc);
        content.add(typeCombo);
        gbc.gridy = 2;
        layout.setConstraints(heartbeatField, gbc);
        content.add(heartbeatField);
        gbc.gridy = 3;
        layout.setConstraints(minField, gbc);
        content.add(minField);
        gbc.gridy = 4;
        layout.setConstraints(maxField, gbc);
        content.add(maxField);
        gbc.gridy = 5;
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
        dsDef = createDsDef();
        if (dsDef != null) {
            close();
        }
    }

    private void close() {
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    private void cancel() {
        close();
    }

    private DsDef createDsDef() {
        String name = nameField.getText();
        if (name == null || name.length() < 1 || name.length() > 20) {
            Util.error(this, "Datasource name must be a non-empty string up to 20 chars long");
            return null;
        }
        DsType type = (DsType) typeCombo.getSelectedItem();
        long heartbeat;
        try {
            heartbeat = Long.parseLong(heartbeatField.getText());
            if (heartbeat <= 0) {
                throw new NumberFormatException();
            }
        }
        catch (NumberFormatException nfe) {
            Util.error(this, "Heartbeat must be a positive integer number");
            return null;
        }
        double min = Double.NaN, max = Double.NaN;
        try {
            min = Double.parseDouble(minField.getText());
        }
        catch (NumberFormatException nfe) {
            // NOP, leave NaN
        }
        try {
            max = Double.parseDouble(maxField.getText());
        }
        catch (NumberFormatException nfe) {
            // NOP, leave NaN
        }
        if (!Double.isNaN(min) && !Double.isNaN(max) && min >= max) {
            Util.error(this, "Min value must be less than max value");
            return null;
        }
        try {
            return new DsDef(name, type, heartbeat, min, max);
        }
        catch (Exception e) {
            // should not be here ever!
            Util.error(this, e);
            return null;
        }
    }

    DsDef getDsDef() {
        return dsDef;
    }
}
