package com.dev.gear.util;

import com.dev.gear.type.ConditionType;
import com.dev.gear.type.ConnectionType;
import com.dev.gear.type.OrmType;
import com.dev.gear.type.SqlType;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class UIHelper {

    public static JDialog createDialog(String title, JPanel contentPanel) {
        JDialog dialog = new JDialog((Frame) null, title, true);
        dialog.setContentPane(contentPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        return dialog;
    }

    public static JTable createFieldSelectionTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                if (value instanceof com.intellij.psi.PsiField) {
                    com.intellij.psi.PsiField field = (com.intellij.psi.PsiField) value;
                    value = field.getName();
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });
        return table;
    }

    public static void setupTableRenderers(JTable table, String[] databaseFieldNames) {
        table.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(new JComboBox<>(ConditionType.values())));
        table.getColumnModel().getColumn(4).setCellEditor(new DefaultCellEditor(new JComboBox<>(ConnectionType.values())));
        table.getColumnModel().getColumn(5).setCellEditor(new DefaultCellEditor(new JComboBox<>(databaseFieldNames)));
    }

    public static JPanel createButtonPanel(JButton... buttons) {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        for (JButton button : buttons) {
            buttonPanel.add(button);
        }
        return buttonPanel;
    }

    public static JScrollPane createScrollPane(Component view) {
        return new JBScrollPane(view);
    }

    public static JTextArea createTextArea(String text, boolean editable) {
        JTextArea textArea = new JTextArea(text);
        textArea.setEditable(editable);
        return textArea;
    }

    public static JComboBox<SqlType> createSqlTypeComboBox() {
        return new JComboBox<>(SqlType.values());
    }

    public static JComboBox<OrmType> createOrmTypeComboBox() {
        return new JComboBox<>(OrmType.values());
    }

    public static JPanel createTopPanel(JComboBox<SqlType> sqlTypeCombo, JComboBox<OrmType> ormCombo, JButton chooseClassesButton) {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("SQL Type:"));
        topPanel.add(sqlTypeCombo);
        topPanel.add(new JLabel("ORM:"));
        topPanel.add(ormCombo);
        topPanel.add(chooseClassesButton);
        return topPanel;
    }

    // Private constructor to prevent instantiation
    private UIHelper() {
        throw new AssertionError("UIHelper is a utility class and should not be instantiated");
    }
}