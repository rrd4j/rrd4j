package org.rrd4j.inspector;

import org.rrd4j.core.RrdDb;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.File;

class MainTreeModel extends DefaultTreeModel {
    private static final DefaultMutableTreeNode INVALID_NODE =
            new DefaultMutableTreeNode("No valid RRD file specified");

    MainTreeModel() {
        super(INVALID_NODE);
    }

    boolean setFile(File newFile) {
        try {
            RrdDb rrd = new RrdDb(newFile.getAbsolutePath(), true);
            try {
                DefaultMutableTreeNode root = new DefaultMutableTreeNode(new RrdNode(rrd));
                int dsCount = rrd.getRrdDef().getDsCount();
                int arcCount = rrd.getRrdDef().getArcCount();
                for (int dsIndex = 0; dsIndex < dsCount; dsIndex++) {
                    DefaultMutableTreeNode dsNode =
                            new DefaultMutableTreeNode(new RrdNode(rrd, dsIndex));
                    for (int arcIndex = 0; arcIndex < arcCount; arcIndex++) {
                        DefaultMutableTreeNode arcNode =
                                new DefaultMutableTreeNode(new RrdNode(rrd, dsIndex, arcIndex));
                        dsNode.add(arcNode);
                    }
                    root.add(dsNode);
                }
                setRoot(root);
            }
            finally {
                rrd.close();
            }
            return true;
        }
        catch (Exception e) {
            setRoot(INVALID_NODE);
            Util.error(null, e);
        }
        return false;
    }
}
