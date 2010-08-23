package org.rrd4j.inspector;

import java.io.File;
import java.text.DecimalFormat;

class InspectorModel {
    private MainTreeModel mainTreeModel = new MainTreeModel();
    private HeaderTableModel generalTableModel = new HeaderTableModel();
    private DatasourceTableModel datasourceTableModel = new DatasourceTableModel();
    private ArchiveTableModel archiveTableModel = new ArchiveTableModel();
    private DataTableModel dataTableModel = new DataTableModel();
    private File file;
    private boolean ok = false;

    MainTreeModel getMainTreeModel() {
        return mainTreeModel;
    }

    HeaderTableModel getGeneralTableModel() {
        return generalTableModel;
    }

    DatasourceTableModel getDatasourceTableModel() {
        return datasourceTableModel;
    }

    DataTableModel getDataTableModel() {
        return dataTableModel;
    }

    ArchiveTableModel getArchiveTableModel() {
        return archiveTableModel;
    }

    void setFile(File file) {
        this.file = file;
        this.ok = mainTreeModel.setFile(file);
        generalTableModel.setFile(file);
        datasourceTableModel.setFile(file);
        archiveTableModel.setFile(file);
        dataTableModel.setFile(file);
    }

    void refresh() {
        setFile(file);
    }

    void selectModel(int dsIndex, int arcIndex) {
        datasourceTableModel.setIndex(dsIndex);
        archiveTableModel.setIndex(dsIndex, arcIndex);
        dataTableModel.setIndex(dsIndex, arcIndex);
    }

    File getFile() {
        return file;
    }

    boolean isOk() {
        return ok;
    }

    private static String DOUBLE_FORMAT = "0.0000000000E00";
    private static final DecimalFormat df = new DecimalFormat(DOUBLE_FORMAT);

    static String formatDouble(double x, String nanString) {
        if (Double.isNaN(x)) {
            return nanString;
        }
        return df.format(x);
    }

    static String formatDouble(double x) {
        return formatDouble(x, "" + Double.NaN);
    }
}
