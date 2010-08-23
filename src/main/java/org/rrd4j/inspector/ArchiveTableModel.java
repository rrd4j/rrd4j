package org.rrd4j.inspector;

import org.rrd4j.core.ArcState;
import org.rrd4j.core.Archive;
import org.rrd4j.core.RrdDb;

import javax.swing.table.AbstractTableModel;
import java.io.File;
import java.util.Date;

class ArchiveTableModel extends AbstractTableModel {
    private static final Object[] DESCRIPTIONS = {
            "consolidation", "xff", "steps", "rows", "accum. value", "NaN steps", "start", "end"
    };
    private static final String[] COLUMN_NAMES = {"description", "value"};

    private File file;
    private Object[] values;
    private int dsIndex = -1, arcIndex = -1;

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
        file = newFile;
        setIndex(-1, -1);
    }

    void setIndex(int newDsIndex, int newArcIndex) {
        if (dsIndex != newDsIndex || arcIndex != newArcIndex) {
            dsIndex = newDsIndex;
            arcIndex = newArcIndex;
            values = null;
            if (dsIndex >= 0 && arcIndex >= 0) {
                try {
                    RrdDb rrd = new RrdDb(file.getAbsolutePath(), true);
                    try {
                        Archive arc = rrd.getArchive(arcIndex);
                        ArcState state = arc.getArcState(dsIndex);
                        values = new Object[]{
                                arc.getConsolFun(),
                                "" + arc.getXff(),
                                "" + arc.getSteps(),
                                "" + arc.getRows(),
                                InspectorModel.formatDouble(state.getAccumValue()),
                                "" + state.getNanSteps(),
                                "" + arc.getStartTime() + " [" + new Date(arc.getStartTime() * 1000L) + "]",
                                "" + arc.getEndTime() + " [" + new Date(arc.getEndTime() * 1000L) + "]"
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
