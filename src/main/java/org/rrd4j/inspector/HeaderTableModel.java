package org.rrd4j.inspector;

import org.rrd4j.core.Header;
import org.rrd4j.core.RrdDb;

import javax.swing.table.AbstractTableModel;
import java.io.File;
import java.util.Date;

class HeaderTableModel extends AbstractTableModel {
    private static final Object[] DESCRIPTIONS = {
            "path", "signature", "step", "last timestamp",
            "datasources", "archives", "size"
    };
    private static final String[] COLUMN_NAMES = {"description", "value"};

    private Object[] values;

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

    void setFile(File newFile) {
        try {
            values = null;
            String path = newFile.getAbsolutePath();
            RrdDb rrd = new RrdDb(path, true);
            try {
                Header header = rrd.getHeader();
                String signature = header.getSignature();
                String step = "" + header.getStep();
                String lastTimestamp = header.getLastUpdateTime() + " [" +
                        new Date(header.getLastUpdateTime() * 1000L) + "]";
                String datasources = "" + header.getDsCount();
                String archives = "" + header.getArcCount();
                String size = rrd.getRrdBackend().getLength() + " bytes";
                values = new Object[]{
                        path, signature, step, lastTimestamp, datasources, archives, size
                };
            }
            finally {
                rrd.close();
            }
            fireTableDataChanged();
        }
        catch (Exception e) {
            Util.error(null, e);
        }
    }
}