package com.dev.gear;

import com.dev.gear.generator.SqlGenerator;
import com.dev.gear.generator.SqlGeneratorFactory;
import com.dev.gear.type.ConditionType;
import com.dev.gear.type.ConnectionType;
import com.dev.gear.type.OrmType;
import com.dev.gear.type.SqlType;
import com.dev.gear.util.ClassChooserUtil;
import com.dev.gear.util.UIHelper;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.*;
import java.util.List;

public class InteractiveSqlGeneratorAction extends AnAction {

    private Stack<SqlGeneratorState> stateStack = new Stack<>();
    private JDialog currentDialog;

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        stateStack.clear();
        showFieldSelectionDialog(project);
    }

    private void showFieldSelectionDialog(Project project) {
        if (currentDialog != null) {
            currentDialog.dispose();
        }

        SqlGeneratorState currentState = stateStack.isEmpty() ? new SqlGeneratorState(null, null) : stateStack.peek();

        JPanel panel = new JPanel(new BorderLayout());

        // Add SQL Type and ORM selection
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JComboBox<SqlType> sqlTypeCombo = new JComboBox<>(SqlType.values());
        JComboBox<OrmType> ormCombo = new JComboBox<>(OrmType.values());
        JButton chooseClassesButton = new JButton("Choose Classes");
        topPanel.add(new JLabel("SQL Type:"));
        topPanel.add(sqlTypeCombo);
        topPanel.add(new JLabel("ORM:"));
        topPanel.add(ormCombo);
        topPanel.add(chooseClassesButton);
        panel.add(topPanel, BorderLayout.NORTH);

        // Field selection table
        String[] columnNames = {"Field", "Type", "Where Include", "Condition", "Connection", "Database Entity Field"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 2 ? Boolean.class : Object.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column >= 2;
            }
        };

        JTable table = UIHelper.createFieldSelectionTable(model);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.add(buttonPanel, BorderLayout.SOUTH);

        chooseClassesButton.addActionListener(e -> {
            ClassChooserUtil.SelectedClasses selectedClasses = ClassChooserUtil.chooseClasses(project);
            if (selectedClasses != null) {
                currentState.selectedClass = selectedClasses.selectedClass;
                currentState.databaseEntityClass = selectedClasses.databaseEntityClass;
                updateFieldSelectionTable(model, table, currentState);
            }
        });

        JButton cancelButton = new JButton("Cancel");
        JButton okButton = new JButton("OK");

        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);

        currentDialog = UIHelper.createDialog("Select Fields, Conditions, and Mappings", panel);

        okButton.addActionListener(e -> {
            List<FieldWithCondition> selectedFields = getSelectedFields(model);

            SqlGeneratorState newState = new SqlGeneratorState(currentState.selectedClass, currentState.databaseEntityClass);
            newState.selectedFields = selectedFields;
            newState.sqlType = (SqlType) sqlTypeCombo.getSelectedItem();
            newState.orm = (OrmType) ormCombo.getSelectedItem();
            stateStack.push(newState);

            currentDialog.dispose();
            showGeneratedSql(project);
        });

        cancelButton.addActionListener(e -> {
            currentDialog.dispose();
            goBack(project);
        });

        currentDialog.setVisible(true);
    }

    private List<FieldWithCondition> getSelectedFields(DefaultTableModel model) {
        List<FieldWithCondition> selectedFields = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            boolean isSelected = (Boolean) model.getValueAt(i, 2);
            if (isSelected) {
                PsiField field = (PsiField) model.getValueAt(i, 0);
                String condition = (String) model.getValueAt(i, 3);
                String connection = (String) model.getValueAt(i, 4);
                String databaseField = (String) model.getValueAt(i, 5);
                selectedFields.add(new FieldWithCondition(field, condition, connection, databaseField));
            }
        }
        return selectedFields;
    }

    private void updateFieldSelectionTable(DefaultTableModel model, JTable table, SqlGeneratorState currentState) {
        model.setRowCount(0);
        if (currentState.selectedClass != null && currentState.databaseEntityClass != null) {
            PsiField[] fields = currentState.selectedClass.getAllFields();
            PsiField[] databaseEntityFields = currentState.databaseEntityClass.getAllFields();
            String[] databaseFieldNames = Arrays.stream(databaseEntityFields)
                    .map(PsiField::getName)
                    .toArray(String[]::new);

            for (PsiField field : fields) {
                String matchingDatabaseField = findMatchingDatabaseField(field.getName(), databaseFieldNames);
                model.addRow(new Object[]{
                        field,
                        field.getType().getPresentableText(),
                        false,
                        ConditionType.EQUALS.getSymbol(),
                        ConnectionType.AND.name(),
                        matchingDatabaseField
                });
            }

            UIHelper.setupTableRenderers(table, databaseFieldNames);
        }
    }

    private String findMatchingDatabaseField(String fieldName, String[] databaseFieldNames) {
        return Arrays.stream(databaseFieldNames)
                .filter(dbFieldName -> dbFieldName.equalsIgnoreCase(fieldName))
                .findFirst()
                .orElse(databaseFieldNames.length > 0 ? databaseFieldNames[0] : "");
    }

    private void goBack(Project project) {
        if (currentDialog != null) {
            currentDialog.dispose();
        }

        if (!stateStack.isEmpty()) {
            stateStack.pop();
            showFieldSelectionDialog(project);
        } else {
            stateStack.clear();
        }
    }

    private void showGeneratedSql(Project project) {
        if (currentDialog != null) {
            currentDialog.dispose();
        }

        SqlGeneratorState currentState = stateStack.peek();
        SqlGenerator sqlGenerator = SqlGeneratorFactory.createSqlGenerator(currentState.orm);
        String generatedSql = sqlGenerator.generateSql(currentState.selectedClass, currentState.selectedFields, currentState.sqlType, currentState.databaseEntityClass);

        JPanel panel = new JPanel(new BorderLayout());
        JTextArea textArea = new JTextArea(generatedSql);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton copyButton = new JButton("Copy to Clipboard");
        copyButton.addActionListener(e -> copyToClipboard(generatedSql));

        JButton backButton = new JButton("Back");
        backButton.addActionListener(e -> {
            currentDialog.dispose();
            goBack(project);
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(copyButton);
        buttonPanel.add(backButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        currentDialog = UIHelper.createDialog("Generated SQL", panel);
        currentDialog.setVisible(true);
    }

    private void copyToClipboard(String text) {
        StringSelection stringSelection = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
        JOptionPane.showMessageDialog(currentDialog, "SQL copied to clipboard!", "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private static class SqlGeneratorState {
        PsiClass selectedClass;
        PsiClass databaseEntityClass;
        List<FieldWithCondition> selectedFields;
        SqlType sqlType;
        OrmType orm;

        SqlGeneratorState(PsiClass selectedClass, PsiClass databaseEntityClass) {
            this.selectedClass = selectedClass;
            this.databaseEntityClass = databaseEntityClass;
        }
    }
}