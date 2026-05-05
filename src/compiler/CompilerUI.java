package compiler;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.List;

/**
 * Main Swing UI for the 6-Phase Compiler Visualizer.
 * Light-themed, modern design with tabbed phase panels.
 */
public class CompilerUI extends JFrame {

    /* -- Theme colours (Light Mode) ----------------------------- */
    private static final Color BG_LIGHT   = new Color(245, 245, 248);
    private static final Color BG_SURFACE = new Color(255, 255, 255);
    private static final Color BG_PANEL   = new Color(250, 250, 252);
    private static final Color FG_TEXT    = new Color(30, 30, 40);
    private static final Color FG_DIM     = new Color(100, 100, 115);
    private static final Color ACCENT     = new Color(45, 85, 180);
    private static final Color GREEN      = new Color(34, 139, 34);
    private static final Color RED        = new Color(200, 40, 40);
    private static final Color YELLOW     = new Color(180, 120, 0);
    private static final Color BORDER     = new Color(200, 200, 210);
    private static final Color HEADER_BG  = new Color(235, 237, 245);

    private static final Font MONO   = new Font("Consolas", Font.BOLD, 15);
    private static final Font MONO_S = new Font("Consolas", Font.BOLD, 14);
    private static final Font TITLE  = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font TITLE_BIG = new Font("Segoe UI", Font.BOLD, 20);

    /* -- Widgets ---------------------------------------------- */
    private JTextArea codeEditor;
    private JTabbedPane tabs;
    private JLabel statusLabel;

    // Phase output widgets
    private JTable tokenTable;
    private DefaultTableModel tokenModel;
    private TreePanel treePanel;
    private JTextArea treeTextArea;
    private JTextArea semanticSymbolArea;
    private JTextArea semanticErrorArea;
    private JTextArea icgArea;
    private JTextArea optOrigArea;
    private JTextArea optOptArea;
    private JTextArea targetArea;

    public CompilerUI() {
        setTitle("Implementation of 6 Phases of Compiler");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1300, 850);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_LIGHT);
        setLayout(new BorderLayout(0, 0));

        add(createHeader(), BorderLayout.NORTH);
        add(createMainContent(), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);
    }

    /* --------------------------------------------------------- */
    /*  HEADER                                                    */
    /* --------------------------------------------------------- */
    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER),
            BorderFactory.createEmptyBorder(12, 20, 12, 20)
        ));

        JLabel title = new JLabel("Implementation of 6 Phases of Compiler");
        title.setFont(TITLE_BIG);
        title.setForeground(ACCENT);
        header.add(title, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton compileBtn = styledButton("Compile", ACCENT);
        compileBtn.addActionListener(e -> compile());
        JButton clearBtn = styledButton("Clear", RED);
        clearBtn.addActionListener(e -> clearAll());
        JButton sampleBtn = styledButton("Load Sample", GREEN);
        sampleBtn.addActionListener(e -> loadSample());

        btnPanel.add(sampleBtn);
        btnPanel.add(compileBtn);
        btnPanel.add(clearBtn);
        header.add(btnPanel, BorderLayout.EAST);
        return header;
    }

    /* --------------------------------------------------------- */
    /*  MAIN CONTENT                                              */
    /* --------------------------------------------------------- */
    private JSplitPane createMainContent() {
        // Left: code editor
        JPanel editorPanel = new JPanel(new BorderLayout());
        editorPanel.setBackground(BG_LIGHT);

        JLabel editorLabel = sectionLabel("Source Code");
        editorPanel.add(editorLabel, BorderLayout.NORTH);

        codeEditor = new JTextArea();
        codeEditor.setFont(MONO);
        codeEditor.setBackground(BG_SURFACE);
        codeEditor.setForeground(FG_TEXT);
        codeEditor.setCaretColor(ACCENT);
        codeEditor.setTabSize(4);
        codeEditor.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        codeEditor.setText("// Enter your source code here\n// Then click Compile\n");

        JScrollPane editorScroll = lightScroll(codeEditor);
        editorPanel.add(editorScroll, BorderLayout.CENTER);

        // Right: tabs for each phase
        tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(TITLE);
        tabs.setBackground(BG_LIGHT);
        tabs.setForeground(FG_TEXT);

        tabs.addTab("1. Lexical",      createLexicalTab());
        tabs.addTab("2. Syntax",       createSyntaxTab());
        tabs.addTab("3. Semantic",     createSemanticTab());
        tabs.addTab("4. Intermediate", createICGTab());
        tabs.addTab("5. Optimized",    createOptimizerTab());
        tabs.addTab("6. Target Code",  createTargetTab());

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editorPanel, tabs);
        split.setDividerLocation(400);
        split.setDividerSize(5);
        split.setBackground(BORDER);
        split.setBorder(null);
        return split;
    }

    /* -- Tab builders ----------------------------------------- */
    private JPanel createLexicalTab() {
        JPanel panel = lightPanel();
        panel.setLayout(new BorderLayout());
        panel.add(sectionLabel("Token Stream (Lexical Analysis)"), BorderLayout.NORTH);

        tokenModel = new DefaultTableModel(new String[]{"#", "Token Type", "Value", "Category", "Line"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tokenTable = new JTable(tokenModel);
        styleTable(tokenTable);

        panel.add(lightScroll(tokenTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createSyntaxTab() {
        JPanel panel = lightPanel();
        panel.setLayout(new BorderLayout());
        panel.add(sectionLabel("Parse Tree (Syntax Analysis)"), BorderLayout.NORTH);

        treePanel = new TreePanel();
        JScrollPane treeScroll = lightScroll(treePanel);

        treeTextArea = lightTextArea();
        JScrollPane textScroll = lightScroll(treeTextArea);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, treeScroll, textScroll);
        split.setDividerLocation(350);
        split.setDividerSize(4);
        split.setBackground(BORDER);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createSemanticTab() {
        JPanel panel = lightPanel();
        panel.setLayout(new BorderLayout());
        panel.add(sectionLabel("Semantic Analysis"), BorderLayout.NORTH);

        semanticSymbolArea = lightTextArea();
        semanticErrorArea  = lightTextArea();

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(BG_LIGHT);
        top.add(sectionLabel("Symbol Table"), BorderLayout.NORTH);
        top.add(lightScroll(semanticSymbolArea), BorderLayout.CENTER);

        JPanel bot = new JPanel(new BorderLayout());
        bot.setBackground(BG_LIGHT);
        bot.add(sectionLabel("Errors / Warnings"), BorderLayout.NORTH);
        bot.add(lightScroll(semanticErrorArea), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bot);
        split.setDividerLocation(300);
        split.setDividerSize(4);
        split.setBackground(BORDER);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createICGTab() {
        JPanel panel = lightPanel();
        panel.setLayout(new BorderLayout());
        panel.add(sectionLabel("Three-Address Code (Intermediate Code)"), BorderLayout.NORTH);
        icgArea = lightTextArea();
        panel.add(lightScroll(icgArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createOptimizerTab() {
        JPanel panel = lightPanel();
        panel.setLayout(new BorderLayout());
        panel.add(sectionLabel("Code Optimization"), BorderLayout.NORTH);

        optOrigArea = lightTextArea();
        optOptArea  = lightTextArea();

        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(BG_LIGHT);
        left.add(sectionLabel("Original TAC"), BorderLayout.NORTH);
        left.add(lightScroll(optOrigArea), BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(BG_LIGHT);
        right.add(sectionLabel("Optimized TAC"), BorderLayout.NORTH);
        right.add(lightScroll(optOptArea), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setDividerLocation(380);
        split.setDividerSize(4);
        split.setBackground(BORDER);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createTargetTab() {
        JPanel panel = lightPanel();
        panel.setLayout(new BorderLayout());
        panel.add(sectionLabel("Target Code (Machine-Level Assembly)"), BorderLayout.NORTH);
        targetArea = lightTextArea();
        panel.add(lightScroll(targetArea), BorderLayout.CENTER);
        return panel;
    }

    /* --------------------------------------------------------- */
    /*  FOOTER                                                    */
    /* --------------------------------------------------------- */
    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(BG_SURFACE);
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(2, 0, 0, 0, BORDER),
            BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));
        statusLabel = new JLabel("Ready -- Enter source code and click Compile");
        statusLabel.setFont(MONO_S);
        statusLabel.setForeground(FG_DIM);
        footer.add(statusLabel, BorderLayout.WEST);

        JLabel credit = new JLabel("6 Phases of Compiler  ");
        credit.setFont(MONO_S);
        credit.setForeground(BORDER);
        footer.add(credit, BorderLayout.EAST);
        return footer;
    }

    /* --------------------------------------------------------- */
    /*  COMPILE PIPELINE                                          */
    /* --------------------------------------------------------- */
    private void compile() {
        String source = codeEditor.getText().trim();
        if (source.isEmpty()) { status("No source code to compile.", RED); return; }

        try {
            // Phase 1: Lexical Analysis
            status("Phase 1: Lexical Analysis...", YELLOW);
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.tokenize();
            fillTokenTable(tokens);

            if (!lexer.getErrors().isEmpty()) {
                status("Lexer errors found.", RED);
                return;
            }

            // Phase 2: Syntax Analysis
            status("Phase 2: Syntax Analysis...", YELLOW);
            Parser parser = new Parser(tokens);
            ASTNode ast = parser.parse();
            treePanel.setAST(ast);
            treeTextArea.setText(ast.toTreeString(""));

            if (!parser.getErrors().isEmpty()) {
                treeTextArea.append("\n-- ERRORS --\n");
                for (String e : parser.getErrors()) treeTextArea.append("  ERROR: " + e + "\n");
                status("Syntax errors found.", RED);
                return;
            }

            // Phase 3: Semantic Analysis
            status("Phase 3: Semantic Analysis...", YELLOW);
            SemanticAnalyzer sem = new SemanticAnalyzer();
            sem.analyze(ast);
            fillSemanticOutput(sem);

            if (!sem.getErrors().isEmpty()) {
                status("Semantic errors found.", RED);
                return;
            }

            // Phase 4: Intermediate Code Generation
            status("Phase 4: Intermediate Code Generation...", YELLOW);
            IntermediateCodeGen icg = new IntermediateCodeGen();
            icg.generate(ast);
            icgArea.setText(icg.toListing());

            // Phase 5: Code Optimization
            status("Phase 5: Code Optimization...", YELLOW);
            CodeOptimizer opt = new CodeOptimizer();
            opt.optimize(icg.getCode());
            optOrigArea.setText(icg.toListing());
            optOptArea.setText(opt.toListing());

            // Phase 6: Target Code Generation
            status("Phase 6: Target Code Generation...", YELLOW);
            TargetCodeGen target = new TargetCodeGen();
            target.generate(opt.getOptimized());
            targetArea.setText(target.toListing());

            status("Compilation successful -- All 6 phases complete!", GREEN);
            tabs.setSelectedIndex(0);

        } catch (Exception ex) {
            status("Error: " + ex.getMessage(), RED);
            ex.printStackTrace();
        }
    }

    /* -- fill helpers ----------------------------------------- */
    private void fillTokenTable(List<Token> tokens) {
        tokenModel.setRowCount(0);
        int idx = 1;
        for (Token t : tokens) {
            if (t.getType() == Token.Type.EOF) continue;
            tokenModel.addRow(new Object[]{idx++, t.getType(), t.getValue(), t.getCategory(), t.getLine()});
        }
    }

    private void fillSemanticOutput(SemanticAnalyzer sem) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-15s %-10s %-15s %-6s%n", "Name", "Type", "Scope", "Line"));
        sb.append("-".repeat(50)).append("\n");
        for (SemanticAnalyzer.Symbol s : sem.getSymbolTable()) {
            sb.append(String.format("%-15s %-10s %-15s %-6d%n", s.name, s.type, s.scope, s.line));
        }
        semanticSymbolArea.setText(sb.toString());

        StringBuilder eb = new StringBuilder();
        if (sem.getErrors().isEmpty() && sem.getWarnings().isEmpty()) {
            eb.append("No semantic errors or warnings.\n");
        }
        for (String e : sem.getErrors())   eb.append("ERROR: ").append(e).append("\n");
        for (String w : sem.getWarnings()) eb.append("WARNING: ").append(w).append("\n");
        semanticErrorArea.setText(eb.toString());
    }

    /* -- clear ------------------------------------------------ */
    private void clearAll() {
        codeEditor.setText("");
        tokenModel.setRowCount(0);
        treePanel.setAST(null);
        treeTextArea.setText("");
        semanticSymbolArea.setText("");
        semanticErrorArea.setText("");
        icgArea.setText("");
        optOrigArea.setText("");
        optOptArea.setText("");
        targetArea.setText("");
        status("Cleared.", FG_DIM);
    }

    /* -- sample code ------------------------------------------ */
    private void loadSample() {
        codeEditor.setText(
            "#include <stdio.h>\n" +
            "int main() {\n" +
            "    int i = 1;\n" +
            "    while(i <= 5) {\n" +
            "        printf(\"Value: %d\\n\", i);\n" +
            "        i++;\n" +
            "    }\n" +
            "    printf(\"Loop ended\\n\");\n" +
            "    return 0;\n" +
            "}\n"
        );
        status("Sample code loaded -- Click Compile", ACCENT);
    }

    /* --------------------------------------------------------- */
    /*  STYLING HELPERS                                           */
    /* --------------------------------------------------------- */
    private void status(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
    }

    private static JButton styledButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btn.setForeground(Color.WHITE);
        btn.setBackground(color);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(9, 22, 9, 22));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(color.brighter()); }
            public void mouseExited(java.awt.event.MouseEvent e)  { btn.setBackground(color); }
        });
        return btn;
    }

    private static JPanel lightPanel() {
        JPanel p = new JPanel();
        p.setBackground(BG_LIGHT);
        return p;
    }

    private static JTextArea lightTextArea() {
        JTextArea ta = new JTextArea();
        ta.setFont(MONO);
        ta.setBackground(BG_SURFACE);
        ta.setForeground(FG_TEXT);
        ta.setCaretColor(ACCENT);
        ta.setEditable(false);
        ta.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return ta;
    }

    private static JScrollPane lightScroll(Component comp) {
        JScrollPane sp = new JScrollPane(comp);
        sp.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        sp.getViewport().setBackground(BG_SURFACE);
        return sp;
    }

    private static JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(TITLE);
        l.setForeground(ACCENT);
        l.setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6));
        return l;
    }

    private void styleTable(JTable table) {
        table.setFont(MONO_S);
        table.setBackground(BG_SURFACE);
        table.setForeground(FG_TEXT);
        table.setGridColor(BORDER);
        table.setRowHeight(30);
        table.setSelectionBackground(new Color(200, 215, 245));
        table.setSelectionForeground(FG_TEXT);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setBackground(BG_SURFACE);
        renderer.setForeground(FG_TEXT);
        renderer.setFont(MONO_S);
        renderer.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        JTableHeader header = table.getTableHeader();
        header.setFont(TITLE);
        header.setBackground(HEADER_BG);
        header.setForeground(ACCENT);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER));

        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(140);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setPreferredWidth(50);
    }
}
