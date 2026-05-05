package compiler;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom JPanel that draws an AST as a visual tree using Java 2D graphics.
 * Nodes are rendered as rounded rectangles connected by lines.
 */
public class TreePanel extends JPanel {

    private ASTNode root;
    private final Map<ASTNode, Rectangle> nodePositions = new HashMap<>();

    // Layout constants
    private static final int NODE_H  = 24;
    private static final int H_GAP   = 6;
    private static final int V_GAP   = 30;
    private static final int PAD_X   = 8;
    private static final int PAD_Y   = 3;
    private static final Font NODE_FONT = new Font("Consolas", Font.PLAIN, 11);

    // Colors (light theme)
    private static final Color BG          = new Color(255, 255, 255);
    private static final Color NODE_FILL   = new Color(230, 238, 250);
    private static final Color NODE_BORDER = new Color(45, 85, 180);
    private static final Color LINE_COLOR  = new Color(160, 170, 190);
    private static final Color TEXT_COLOR  = new Color(30, 30, 40);

    public TreePanel() {
        setBackground(BG);
    }

    public void setAST(ASTNode root) {
        this.root = root;
        nodePositions.clear();
        if (root != null) {
            FontMetrics fm = getFontMetrics(NODE_FONT);
            int treeWidth  = computeWidth(root, fm);
            int treeHeight = computeHeight(root);
            setPreferredSize(new Dimension(treeWidth + 60, treeHeight + 60));
            layoutNode(root, 30, 30, treeWidth, fm);
        }
        revalidate();
        repaint();
    }

    /* ── layout computation ─────────────────────────────── */
    private int computeWidth(ASTNode node, FontMetrics fm) {
        int labelW = fm.stringWidth(node.getLabel()) + PAD_X * 2;
        if (node.getChildren().isEmpty()) return Math.max(labelW, 60);
        int childrenW = 0;
        for (int i = 0; i < node.getChildren().size(); i++) {
            if (i > 0) childrenW += H_GAP;
            childrenW += computeWidth(node.getChildren().get(i), fm);
        }
        return Math.max(labelW, childrenW);
    }

    private int computeHeight(ASTNode node) {
        if (node.getChildren().isEmpty()) return NODE_H;
        int maxChildH = 0;
        for (ASTNode c : node.getChildren()) maxChildH = Math.max(maxChildH, computeHeight(c));
        return NODE_H + V_GAP + maxChildH;
    }

    private void layoutNode(ASTNode node, int x, int y, int availableW, FontMetrics fm) {
        int labelW = fm.stringWidth(node.getLabel()) + PAD_X * 2;
        int nodeW = Math.max(labelW, 50);
        int nodeX = x + (availableW - nodeW) / 2;
        nodePositions.put(node, new Rectangle(nodeX, y, nodeW, NODE_H));

        if (node.getChildren().isEmpty()) return;

        // Distribute children across available width
        int totalChildW = 0;
        int[] childWidths = new int[node.getChildren().size()];
        for (int i = 0; i < node.getChildren().size(); i++) {
            childWidths[i] = computeWidth(node.getChildren().get(i), fm);
            totalChildW += childWidths[i];
        }
        totalChildW += H_GAP * (node.getChildren().size() - 1);

        int childX = x + (availableW - totalChildW) / 2;
        int childY = y + NODE_H + V_GAP;
        for (int i = 0; i < node.getChildren().size(); i++) {
            layoutNode(node.getChildren().get(i), childX, childY, childWidths[i], fm);
            childX += childWidths[i] + H_GAP;
        }
    }

    /* ── painting ───────────────────────────────────────── */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (root == null) {
            g.setColor(TEXT_COLOR);
            g.setFont(NODE_FONT);
            g.drawString("Parse tree will appear here after compilation.", 30, 40);
            return;
        }
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawEdges(g2, root);
        drawNodes(g2, root);
    }

    private void drawEdges(Graphics2D g2, ASTNode node) {
        Rectangle parentR = nodePositions.get(node);
        if (parentR == null) return;
        g2.setColor(LINE_COLOR);
        g2.setStroke(new BasicStroke(1.5f));
        for (ASTNode child : node.getChildren()) {
            Rectangle childR = nodePositions.get(child);
            if (childR == null) continue;
            int x1 = parentR.x + parentR.width / 2;
            int y1 = parentR.y + parentR.height;
            int x2 = childR.x + childR.width / 2;
            int y2 = childR.y;
            g2.drawLine(x1, y1, x2, y2);
            drawEdges(g2, child);
        }
    }

    private void drawNodes(Graphics2D g2, ASTNode node) {
        Rectangle r = nodePositions.get(node);
        if (r == null) return;
        // Fill
        g2.setColor(NODE_FILL);
        g2.fill(new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 10, 10));
        // Border
        g2.setColor(NODE_BORDER);
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 10, 10));
        // Label
        g2.setColor(TEXT_COLOR);
        g2.setFont(NODE_FONT);
        FontMetrics fm = g2.getFontMetrics();
        String label = node.getLabel();
        int textX = r.x + (r.width - fm.stringWidth(label)) / 2;
        int textY = r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(label, textX, textY);
        // Recurse
        for (ASTNode c : node.getChildren()) drawNodes(g2, c);
    }
}
