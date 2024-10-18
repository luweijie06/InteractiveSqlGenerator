package com.dev.gear.util;

import com.dev.gear.type.ConditionType;
import com.dev.gear.type.ConnectionType;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Arrays;

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
        table.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(createConditionComboBox()));
        table.getColumnModel().getColumn(4).setCellEditor(new DefaultCellEditor(new JComboBox<>(ConnectionType.values())));
        table.getColumnModel().getColumn(5).setCellEditor(new DefaultCellEditor(new JComboBox<>(databaseFieldNames)));
    }

    private static JComboBox<String> createConditionComboBox() {
        String[] conditionSymbols = Arrays.stream(ConditionType.values())
                .map(ConditionType::getSymbol)
                .toArray(String[]::new);
        return new JComboBox<>(conditionSymbols);
    }


    // Private constructor to prevent instantiation
    private UIHelper() {
        throw new AssertionError("UIHelper is a utility class and should not be instantiated");
    }
}