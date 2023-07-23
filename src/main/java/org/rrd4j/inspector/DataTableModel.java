package org.rrd4j.inspector;

import org.rrd4j.core.Archive;
import org.rrd4j.core.Robin;
import org.rrd4j.core.RrdDb;

import javax.swing.table.AbstractTableModel;
import java.io.File;
import java.util.Date;

class DataTableModel extends AbstractTableModel {
    private static final String[] COLUMN_NAMES = {"timestamp", "date", "value"};

    private File file;
    private Object[][] values;
    private int dsIndex = -1, arcIndex = -1;

    /**
     * <p>getRowCount.</p>
     *
     * @return a int.
     */
    public int getRowCount() {
        if (values == null) {
            return 0;
        }
        else {
            return values.length;
        }
    }

    /**
     * <p>getColumnCount.</p>
     *
     * @return a int.
     */
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    /** {@inheritDoc} */
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (values == null) {
            return "--";
        }
        return values[rowIndex][columnIndex];
    }

    /** {@inheritDoc} */
    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 2;
    }

    /** {@inheritDoc} */
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        assert columnIndex == 2 : "Column " + columnIndex + " is not editable!";
        double value;
        try {
            value = Double.parseDouble(aValue.toString());
        }
        catch (NumberFormatException nfe) {
            value = Double.NaN;
        }
        if (dsIndex >= 0 && arcIndex >= 0 && file != null) {
            try (RrdDb rrd = RrdDb.getBuilder().setPath(file.getAbsolutePath()).build()){
                Robin robin = rrd.getArchive(arcIndex).getRobin(dsIndex);
                robin.setValue(rowIndex, value);
                values[rowIndex][2] = InspectorModel.formatDouble(robin.getValue(rowIndex));
            } catch (Exception e) {
                Util.error(null, e);
            }
        }
    }

    void setFile(File newFile) {
        file = newFile;
        setIndex(-1, -1);
    }

    void setIndex(int newDsIndex, int newArcIndex) {
        if (dsIndex != newDsIndex || arcIndex != newArcIndex) {
            dsIndex = newDsIndex;
            arcIndex = newArcIndex;
            values = null;
            if (dsIndex >= 0 && arcIndex >= 0) {
                try (RrdDb rrd = RrdDb.getBuilder().setPath(file.getAbsolutePath()).readOnly().build()) {
                    Archive arc = rrd.getArchive(arcIndex);
                    Robin robin = arc.getRobin(dsIndex);
                    long start = arc.getStartTime();
                    long step = arc.getArcStep();
                    double[] robinValues = robin.getValues();
                    values = new Object[robinValues.length][];
                    for (int i = 0; i < robinValues.length; i++) {
                        long timestamp = start + i * step;
                        String date = new Date(timestamp * 1000L).toString();
                        String value = InspectorModel.formatDouble(robinValues[i]);
                        values[i] = new Object[]{
                                Long.toString(timestamp), date, value
                        };
                    }
                } catch (Exception e) {
                    Util.error(null, e);
                }
            }
            fireTableDataChanged();
        }
    }
}
