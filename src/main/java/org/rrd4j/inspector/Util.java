package org.rrd4j.inspector;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

class Util {
    static void centerOnScreen(Window window) {
        Toolkit t = Toolkit.getDefaultToolkit();
        Dimension screenSize = t.getScreenSize();
        Dimension frameSize = window.getPreferredSize();
        double x = (screenSize.getWidth() - frameSize.getWidth()) / 2;
        double y = (screenSize.getHeight() - frameSize.getHeight()) / 2;
        window.setLocation((int) x, (int) y);
    }

    static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    static void error(Component parent, Exception e) {
        e.printStackTrace();
        error(parent, e.toString());
    }

    private static Vector<Window> windows = new Vector<Window>();
    private static final int WINDOW_POSITION_SHIFT = 20;

    static void placeWindow(Window window) {
        int count = windows.size();
        if (count == 0) {
            centerOnScreen(window);
        }
        else {
            Window last = windows.get(count - 1);
            int x = last.getX() + WINDOW_POSITION_SHIFT;
            int y = last.getY() + WINDOW_POSITION_SHIFT;
            window.setLocation(x, y);
        }
        windows.add(window);
    }

    static void dismissWindow(Window window) {
        windows.remove(window);
        if (windows.size() == 0) {
            System.exit(0);
        }
    }
}
