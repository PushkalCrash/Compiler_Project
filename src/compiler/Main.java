package compiler;

import javax.swing.*;

/**
 * Entry point for the CompilerViz application.
 * Launches the Swing UI with system look-and-feel overrides.
 */
public class Main {
    public static void main(String[] args) {
        // Set light UI defaults before creating the frame
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            UIManager.put("TabbedPane.selected", new java.awt.Color(255, 255, 255));
            UIManager.put("TabbedPane.contentAreaColor", new java.awt.Color(245, 245, 248));
            UIManager.put("TabbedPane.background", new java.awt.Color(235, 237, 245));
            UIManager.put("TabbedPane.foreground", new java.awt.Color(30, 30, 40));
            UIManager.put("TabbedPane.shadow", new java.awt.Color(200, 200, 210));
            UIManager.put("TabbedPane.darkShadow", new java.awt.Color(180, 180, 190));
            UIManager.put("TabbedPane.light", new java.awt.Color(245, 245, 248));
            UIManager.put("TabbedPane.highlight", new java.awt.Color(45, 85, 180));
            UIManager.put("Panel.background", new java.awt.Color(245, 245, 248));
            UIManager.put("ScrollPane.background", new java.awt.Color(255, 255, 255));
            UIManager.put("ScrollBar.thumb", new java.awt.Color(180, 180, 195));
            UIManager.put("ScrollBar.track", new java.awt.Color(240, 240, 245));
            UIManager.put("SplitPane.background", new java.awt.Color(200, 200, 210));
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            CompilerUI frame = new CompilerUI();
            frame.setVisible(true);
        });
    }
}
