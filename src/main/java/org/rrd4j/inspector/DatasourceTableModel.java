package org.rrd4j.inspector;

import org.rrd4j.core.Datasource;
import org.rrd4j.core.RrdDb;

import javax.swing.table.AbstractTableModel;
import java.io.File;

class DatasourceTableModel extends AbstractTableModel {
    private static final Object[] DESCRIPTIONS = {
            "name", "type", "heartbeat", "min value",
            "max value", "last value", "accum. value", "NaN seconds"
    };
    private static final String[] COLUMN_NAMES = {
            "description", "value"
    };

    private File file;
    private Object[] values;
    private int dsIndex = -1;

    public int getRowCount() {
        return DESCRIPTIONS.length;
    }

    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return DESCRIPTIONS[rowIndex];
        }
        else if (columnIndex == 1) {
            if (values != null) {
                return values[rowIndex];
            }
            else {
                return "--";
            }
        }
        return null;
    }

    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    void setFile(File newFile) {
        file = newFile;
        setIndex(-1);
    }

    void setIndex(int newDsIndex) {
        if (dsIndex != newDsIndex) {
            dsIndex = newDsIndex;
            values = null;
            if (dsIndex >= 0) {
                try {
                    RrdDb rrd = new RrdDb(file.getAbsolutePath(), true);
                    try {
                        Datasource ds = rrd.getDatasource(dsIndex);
                        values = new Object[]{
                                ds.getName(),
                                ds.getType(),
                                "" + ds.getHeartbeat(),
                                InspectorModel.formatDouble(ds.getMinValue()),
                                InspectorModel.formatDouble(ds.getMaxValue()),
                                InspectorModel.formatDouble(ds.getLastValue()),
                                InspectorModel.formatDouble(ds.getAccumValue()),
                                "" + ds.getNanSeconds()
                        };
                    }
                    finally {
                        rrd.close();
                    }
                }
                catch (Exception e) {
                    Util.error(null, e);
                }
            }
            fireTableDataChanged();
        }
    }
}