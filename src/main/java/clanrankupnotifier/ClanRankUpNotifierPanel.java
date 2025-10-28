// ClanRankNotifierPanel.java
package clanrankupnotifier;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class ClanRankUpNotifierPanel extends PluginPanel
{
    private static final int NAME_COLUMN_WIDTH = 65;
    private static final int DAYS_COLUMN_WIDTH = 40;
    private static final int CURRENT_RANK_COLUMN_WIDTH = 50;
    private static final int NEXT_RANK_COLUMN_WIDTH = 50;

    private final JPanel content = new JPanel();
    private final JLabel infoLabel = new JLabel();
    private final JButton runButton = new JButton("Update");

    public ClanRankUpNotifierPanel(Runnable onRunCheck)
    {
        setLayout(new BorderLayout(0, 4));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel headerLabel = new JLabel("Clan Rank Up Notifier");
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 14f));
        headerLabel.setForeground(Color.WHITE);

        runButton.setFocusable(false);
        runButton.setMargin(new Insets(2, 8, 2, 8));
        runButton.addActionListener(e -> {
            setBusy(true);
            try {
                if (onRunCheck != null) onRunCheck.run();
            }
            finally { setBusy(false); }
        });

        top.add(headerLabel, BorderLayout.WEST);
        top.add(runButton, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        infoLabel.setForeground(Color.LIGHT_GRAY);
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.PLAIN, 12f));
        infoLabel.setBorder(new EmptyBorder(2, 2, 2, 2));
        add(infoLabel, BorderLayout.CENTER);

        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(ColorScheme.DARK_GRAY_COLOR);

        content.add(makeHeaderRow());

        content.setAlignmentX(0f);
        add(content, BorderLayout.SOUTH);
    }

    private JComponent makeWrappingNameCell(String text) {
        JTextArea ta = new JTextArea(text);
        ta.setFont(FontManager.getRunescapeSmallFont());
        ta.setForeground(Color.WHITE);
        ta.setOpaque(false);
        ta.setEditable(false);
        ta.setHighlighter(null);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setBorder(new EmptyBorder(0, 4, 1, 4));
        ta.setSize(new Dimension(ClanRankUpNotifierPanel.NAME_COLUMN_WIDTH, Short.MAX_VALUE));
        Dimension pref = ta.getPreferredSize();
        Dimension fixed = new Dimension(ClanRankUpNotifierPanel.NAME_COLUMN_WIDTH, pref.height);
        ta.setMinimumSize(new Dimension(ClanRankUpNotifierPanel.NAME_COLUMN_WIDTH, 0));
        ta.setPreferredSize(fixed);
        ta.setMaximumSize(new Dimension(ClanRankUpNotifierPanel.NAME_COLUMN_WIDTH, Integer.MAX_VALUE));
        ta.setToolTipText(text);
        return ta;
    }

    public void setBusy(boolean busy) {
        SwingUtilities.invokeLater(() -> runButton.setEnabled(!busy));
    }

    public void setInfoText(String text) {
        SwingUtilities.invokeLater(() -> {
            infoLabel.setText(text == null ? "" : text);
            infoLabel.setVisible(text != null && !text.isBlank());
        });
    }

    public void setRows(List<String> lines)
    {
        SwingUtilities.invokeLater(() -> {
            content.removeAll();
            content.add(makeHeaderRow());

            if (lines == null || lines.isEmpty()) {
                content.add(makeInfoRow("No promotions due.\nSomething wrong? Please check the plugin configuration."));
            } else {
                for (String line : lines) content.add(createMemberRow(line));
            }

            content.revalidate();
            content.repaint();
        });
    }

    private JComponent makeInfoRow(String text) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
                new EmptyBorder(6, 8, 6, 8)));

        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        row.add(label);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));

        return row;
    }

    private JPanel makeHeaderRow() {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
                new EmptyBorder(6, 6, 6, 6)));

        row.add(makeHeaderLabel("Name", NAME_COLUMN_WIDTH));
        row.add(makeHeaderLabel("Days", DAYS_COLUMN_WIDTH));
        row.add(makeHeaderLabel("Curr", CURRENT_RANK_COLUMN_WIDTH));
        row.add(makeHeaderLabel("Next", NEXT_RANK_COLUMN_WIDTH));
        row.add(Box.createHorizontalGlue());
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));

        return row;
    }

    private JLabel makeHeaderLabel(String text, int width) {
        JLabel l = new JLabel(text);
        l.setFont(FontManager.getRunescapeSmallFont());
        l.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR.darker());
        l.setBorder(new EmptyBorder(0, 4, 0, 4));
        l.setToolTipText(text);
        fixWidth(l, width);
        return l;
    }

    private void fixWidth(JComponent c, int width) {
        Dimension d = new Dimension(width, c.getPreferredSize().height);
        c.setMinimumSize(new Dimension(width, d.height));
        c.setPreferredSize(new Dimension(width, d.height));
        c.setMaximumSize(new Dimension(width, Integer.MAX_VALUE));
    }

    private JPanel createMemberRow(String text)
    {
        String name = "";
        String days = "";
        String currentRank = "";
        String nextRank = "";

        String[] parts = text.split(",");
        name = parts.length > 0 ? parts[0].trim() : "?";
        days = parts.length > 1 ? parts[1].trim() : "?";
        nextRank =  parts.length > 2 ? parts[2].trim() : "?";
        currentRank = parts.length > 3 ? parts[3].trim() : "?";


        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setOpaque(true);
        row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        row.setBorder(new EmptyBorder(4, 6, 4, 6));

        final Color base = row.getBackground();
        row.addMouseListener(new java.awt.event.MouseAdapter()
        {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { row.setBackground(base.brighter()); }
            @Override public void mouseExited (java.awt.event.MouseEvent e) { row.setBackground(base); }
        });

        JComponent nameLabel = makeWrappingNameCell(name);
        JLabel daysLabel = makeCell(days, DAYS_COLUMN_WIDTH, new Color(100, 225, 255));
        daysLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        JLabel currentRankLabel = makeCell(currentRank, CURRENT_RANK_COLUMN_WIDTH, new Color(255, 230, 110));
        JLabel nextRankLabel = makeCell(nextRank, NEXT_RANK_COLUMN_WIDTH, new Color(140, 255, 160));

        row.add(nameLabel);
        row.add(daysLabel);
        row.add(currentRankLabel);
        row.add(nextRankLabel);
        row.add(Box.createHorizontalGlue());

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(row, BorderLayout.CENTER);
        wrapper.add(new JSeparator(JSeparator.HORIZONTAL), BorderLayout.SOUTH);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, wrapper.getPreferredSize().height));
        return wrapper;
    }

    private JLabel makeCell(String text, int width, Color fg) {
        JLabel l = new JLabel(text);
        l.setFont(FontManager.getRunescapeSmallFont());
        l.setForeground(fg);
        l.setBorder(new EmptyBorder(0, 4, 0, 4));
        fixWidth(l, width);
        l.setToolTipText(text);
        return l;
    }
}
