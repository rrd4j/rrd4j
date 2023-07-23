package org.rrd4j.inspector;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import org.rrd4j.core.ArcDef;
import org.rrd4j.core.Archive;
import org.rrd4j.core.Datasource;
import org.rrd4j.core.DsDef;
import org.rrd4j.core.Robin;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.data.LinearInterpolator;
import org.rrd4j.data.Variable;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphDef;
import org.rrd4j.graph.RrdGraphInfo;

class GraphFrame extends JFrame {
    private static final Color COLOR = Color.RED;
    private static final int WIDTH = 400, HEIGHT = 240;
    private int deltaWidth = 0, deltaHeight = 0;

    private Color color = COLOR;
    private GraphPanel graphPanel = new GraphPanel();
    private JComboBox<GraphComboItem> graphCombo = new JComboBox<>();
    private RrdGraph rrdGraph;

    private String sourcePath;
    private int dsIndex, arcIndex;

    private int counted = 0;

    GraphFrame(String sourcePath, int dsIndex, int arcIndex) {
        this.sourcePath = sourcePath;
        this.dsIndex = dsIndex;
        this.arcIndex = arcIndex;
        fillGraphCombo();
        constructUI();
        pack();
        //createRrdGraph();
        Util.placeWindow(this);
        setVisible(true);
    }

    private void createRrdGraph() {
        System.out.println("Creating graph...");
        try (RrdDb rrdDb = RrdDb.getBuilder().setPath(sourcePath).readOnly().build()) {
            RrdDef rrdDef;
            long[] timestamps;
            double[] values;
            String dsName;
            long t1, t2;
            Datasource ds = rrdDb.getDatasource(dsIndex);
            Archive arc = rrdDb.getArchive(arcIndex);
            Robin robin = arc.getRobin(dsIndex);
            dsName = ds.getName();
            t1 = arc.getStartTime();
            t2 = arc.getEndTime();
            long step = arc.getArcStep();
            int count = robin.getSize();
            timestamps = new long[count];
            for (int i = 0; i < count; i++) {
                timestamps[i] = t1 + i * step;
            }
            values = robin.getValues();
            rrdDef = rrdDb.getRrdDef();
            RrdGraphDef rrdGraphDef = new RrdGraphDef(t1, t2);
            rrdGraphDef.setImageFormat("png");
            rrdGraphDef.setTitle(rrdDef.getDsDefs()[dsIndex].dump() + " " +
                    rrdDef.getArcDefs()[arcIndex].dump());
            LinearInterpolator linearInterpolator = new LinearInterpolator(timestamps, values);
            linearInterpolator.setInterpolationMethod(LinearInterpolator.Method.LINEAR);
            rrdGraphDef.datasource(dsName, linearInterpolator);
            rrdGraphDef.area(dsName, color, dsName + "\\r");
            rrdGraphDef.comment("START: " + new Date(t1 * 1000L) + "\\r");
            rrdGraphDef.comment("END: " + new Date(t2 * 1000L) + "\\r");
            rrdGraphDef.datasource("max", dsName, new Variable.MAX());
            rrdGraphDef.datasource("min", dsName, new Variable.MIN());
            rrdGraphDef.gprint("max", "%f");
            rrdGraphDef.gprint("min", "%f");
            int width = graphPanel.getWidth(), height = graphPanel.getHeight();
            rrdGraphDef.setWidth(width + deltaWidth);
            rrdGraphDef.setHeight(height + deltaHeight);
            rrdGraphDef.setFilename("/tmp/try" + counted++ + ".png");
            rrdGraph = new RrdGraph(rrdGraphDef);

            if (deltaWidth == 0 && deltaHeight == 0) {
                RrdGraphInfo info = rrdGraph.getRrdGraphInfo();
                deltaWidth = graphPanel.getWidth() - info.getWidth();
                deltaHeight = graphPanel.getHeight() - info.getHeight();
                if (deltaWidth != 0 && deltaHeight != 0) {
                    createRrdGraph(); // recursion is divine!
                }
            }
        }
        catch (Exception e) {
            Util.error(this, e);
        }
    }

    private void fillGraphCombo() {
        try (RrdDb rrdDb = RrdDb.getBuilder().setPath(sourcePath).readOnly().build()) {
            RrdDef rrdDef = rrdDb.getRrdDef();
            final DsDef[] dsDefs = rrdDef.getDsDefs();
            final ArcDef[] arcDefs = rrdDef.getArcDefs();
            GraphComboItem[] items = new GraphComboItem[rrdDef.getDsCount() * rrdDef.getArcCount()];
            int selectedItem = -1;
            for (int i = 0, k = 0; i < rrdDef.getDsCount(); i++) {
                for (int j = 0; j < rrdDef.getArcCount(); k++, j++) {
                    String description = dsDefs[i].dump() + " " + arcDefs[j].dump();
                    items[k] = new GraphComboItem(description, i, j);
                    if (i == dsIndex && j == arcIndex) {
                        selectedItem = k;
                    }
                }
            }
            graphCombo.setModel(new DefaultComboBoxModel<GraphComboItem>(items));
            graphCombo.setSelectedIndex(selectedItem);
        } catch (Exception e) {
            Util.error(this, e);
        }
    }

    private void constructUI() {
        setTitle(new File(sourcePath).getName());
        JPanel content = (JPanel) getContentPane();
        content.setLayout(new BorderLayout(0, 0));
        content.add(graphCombo, BorderLayout.NORTH);
        graphPanel.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        content.add(graphPanel, BorderLayout.CENTER);
        JPanel southPanel = new JPanel();
        southPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JButton colorButton = new JButton("Change graph color");
        southPanel.add(colorButton);
        colorButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                changeColor();
            }
        });
        JButton saveButton = new JButton("Save graph");
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveGraph();
            }
        });
        southPanel.add(Box.createHorizontalStrut(3));
        southPanel.add(saveButton);
        content.add(southPanel, BorderLayout.SOUTH);
        // EVENT HANDLERS
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                closeWindow();
            }
        });
        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                createRrdGraph();
                graphPanel.repaint();
            }
        });
        graphCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    GraphComboItem item = (GraphComboItem) e.getItem();
                    dsIndex = item.getDsIndex();
                    arcIndex = item.getArcIndex();
                    createRrdGraph();
                    graphPanel.repaint();
                }
            }
        });
    }

    private void closeWindow() {
        Util.dismissWindow(this);
    }

    private void changeColor() {
        final JColorChooser picker = new JColorChooser(color);
        ActionListener okListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                color = picker.getColor();
                createRrdGraph();
                repaint();
            }
        };
        JColorChooser.createDialog(this, "Select color", true, picker, okListener, null).setVisible(true);
    }

    private void saveGraph() {
        JFileChooser chooser = new JFileChooser();
        FileFilter filter = new FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || f.getAbsolutePath().toLowerCase().endsWith(".png");
            }

            public String getDescription() {
                return "PNG images";
            }
        };
        chooser.setFileFilter(filter);
        int returnVal = chooser.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                File selectedFile = chooser.getSelectedFile();
                String path = selectedFile.getAbsolutePath();
                if (!path.toLowerCase().endsWith(".png")) {
                    path += ".png";
                    selectedFile = new File(path);
                }
                if (selectedFile.exists()) {
                    // ask user to overwrite
                    String message = "File [" + selectedFile.getName() +
                            "] already exists. Do you want to overwrite it?";
                    int answer = JOptionPane.showConfirmDialog(this,
                            message, "File exists", JOptionPane.YES_NO_OPTION);
                    if (answer == JOptionPane.NO_OPTION) {
                        return;
                    }
                }
                String absolutePath = selectedFile.getAbsolutePath();
                byte[] data = rrdGraph.getRrdGraphInfo().getBytes();
                try (RandomAccessFile f = new RandomAccessFile(absolutePath, "rw")) {
                    f.write(data);
                }
            }
            catch (IOException e) {
                Util.error(this, "Could not save graph to file:\n" + e);
            }
        }
    }

    final class GraphPanel extends JPanel {
        public void paintComponent(Graphics g) {
            if (rrdGraph != null) rrdGraph.render(g);
        }
    }

    class GraphComboItem {
        private String description;
        private int dsIndex, arcIndex;

        GraphComboItem(String description, int dsIndex, int arcIndex) {
            this.description = description;
            this.dsIndex = dsIndex;
            this.arcIndex = arcIndex;
        }

        public String toString() {
            return description;
        }

        int getDsIndex() {
            return dsIndex;
        }

        int getArcIndex() {
            return arcIndex;
        }
    }
}
