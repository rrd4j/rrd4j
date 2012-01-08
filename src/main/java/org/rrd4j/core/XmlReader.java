package org.rrd4j.core;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.rrd4j.ConsolFun;

import java.io.File;
import java.io.IOException;

class XmlReader extends DataImporter {

    private Element root;
    private Node[] dsNodes, arcNodes;

    XmlReader(String xmlFilePath) throws IOException {
        root = Util.Xml.getRootElement(new File(xmlFilePath));
        dsNodes = Util.Xml.getChildNodes(root, "ds");
        arcNodes = Util.Xml.getChildNodes(root, "rra");
    }

    String getVersion() {
        return Util.Xml.getChildValue(root, "version");
    }

    long getLastUpdateTime() {
        return Util.Xml.getChildValueAsLong(root, "lastupdate");
    }

    long getStep() {
        return Util.Xml.getChildValueAsLong(root, "step");
    }

    int getDsCount() {
        return dsNodes.length;
    }

    int getArcCount() {
        return arcNodes.length;
    }

    String getDsName(int dsIndex) {
        return Util.Xml.getChildValue(dsNodes[dsIndex], "name");
    }

    String getDsType(int dsIndex) {
        return Util.Xml.getChildValue(dsNodes[dsIndex], "type");
    }

    long getHeartbeat(int dsIndex) {
        return Util.Xml.getChildValueAsLong(dsNodes[dsIndex], "minimal_heartbeat");
    }

    double getMinValue(int dsIndex) {
        return Util.Xml.getChildValueAsDouble(dsNodes[dsIndex], "min");
    }

    double getMaxValue(int dsIndex) {
        return Util.Xml.getChildValueAsDouble(dsNodes[dsIndex], "max");
    }

    double getLastValue(int dsIndex) {
        return Util.Xml.getChildValueAsDouble(dsNodes[dsIndex], "last_ds");
    }

    double getAccumValue(int dsIndex) {
        return Util.Xml.getChildValueAsDouble(dsNodes[dsIndex], "value");
    }

    long getNanSeconds(int dsIndex) {
        return Util.Xml.getChildValueAsLong(dsNodes[dsIndex], "unknown_sec");
    }

    ConsolFun getConsolFun(int arcIndex) {
        return ConsolFun.valueOf(Util.Xml.getChildValue(arcNodes[arcIndex], "cf"));
    }

    double getXff(int arcIndex) {
        Node arc = arcNodes[arcIndex];
        Node params[] = Util.Xml.getChildNodes(arc, "params");
        //RRD4J xml, xff is in the archive definition
        if(params.length == 0) {
            return Util.Xml.getChildValueAsDouble(arc, "xff");
        }
        //RRDTool xml, xff is in the archive definition
        else {
            return Util.Xml.getChildValueAsDouble(params[0], "xff");
        }
    }

    int getSteps(int arcIndex) {
        return Util.Xml.getChildValueAsInt(arcNodes[arcIndex], "pdp_per_row");
    }

    double getStateAccumValue(int arcIndex, int dsIndex) {
        Node cdpNode = Util.Xml.getFirstChildNode(arcNodes[arcIndex], "cdp_prep");
        Node[] dsNodes = Util.Xml.getChildNodes(cdpNode, "ds");
        return Util.Xml.getChildValueAsDouble(dsNodes[dsIndex], "value");
    }

    int getStateNanSteps(int arcIndex, int dsIndex) {
        Node cdpNode = Util.Xml.getFirstChildNode(arcNodes[arcIndex], "cdp_prep");
        Node[] dsNodes = Util.Xml.getChildNodes(cdpNode, "ds");
        return Util.Xml.getChildValueAsInt(dsNodes[dsIndex], "unknown_datapoints");
    }

    int getRows(int arcIndex) {
        Node dbNode = Util.Xml.getFirstChildNode(arcNodes[arcIndex], "database");
        Node[] rows = Util.Xml.getChildNodes(dbNode, "row");
        return rows.length;
    }

    double[] getValues(int arcIndex, int dsIndex) {
        Node dbNode = Util.Xml.getFirstChildNode(arcNodes[arcIndex], "database");
        Node[] rows = Util.Xml.getChildNodes(dbNode, "row");
        double[] values = new double[rows.length];
        for (int i = 0; i < rows.length; i++) {
            Node[] vNodes = Util.Xml.getChildNodes(rows[i], "v");
            Node vNode = vNodes[dsIndex];
            values[i] = Util.parseDouble(vNode.getFirstChild().getNodeValue().trim());
        }
        return values;
    }
}