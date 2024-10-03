package com.dev.gear.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ClassChooserUtil {

    public static class SelectedClasses {
        public PsiClass selectedClass;
        public PsiClass databaseEntityClass;

        public SelectedClasses(PsiClass selectedClass, PsiClass databaseEntityClass) {
            this.selectedClass = selectedClass;
            this.databaseEntityClass = databaseEntityClass;
        }
    }

    public static SelectedClasses chooseClasses(Project project) {
        ClassChooserDialog dialog = new ClassChooserDialog(project);
        dialog.show();
        return dialog.getSelectedClasses();
    }

    private static List<PsiClass> findMatchingClasses(Project project, String className, boolean fuzzyMatch) {
        List<PsiClass> result = new ArrayList<>();
        PsiManager psiManager = PsiManager.getInstance(project);

        VirtualFile[] contentRoots = ProjectRootManager.getInstance(project).getContentRoots();
        for (VirtualFile root : contentRoots) {
            Collection<VirtualFile> java = FilenameIndex.getAllFilesByExt(project, "java", GlobalSearchScope.projectScope(project));

            for (VirtualFile file : java) {
                PsiFile psiFile = psiManager.findFile(file);
                if (psiFile instanceof PsiJavaFile) {
                    PsiJavaFile javaFile = (PsiJavaFile) psiFile;
                    for (PsiClass psiClass : javaFile.getClasses()) {
                        if (psiClass.getName() != null) {
                            if (fuzzyMatch) {
                                if (psiClass.getName().toLowerCase().contains(className.toLowerCase())) {
                                    result.add(psiClass);
                                }
                            } else {
                                if (psiClass.getName().equalsIgnoreCase(className)) {
                                    result.add(psiClass);
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    private static class ClassChooserDialog extends DialogWrapper {
        private JTextField selectedClassSearchField;
        private JTextField databaseEntityClassSearchField;
        private JBList<PsiClass> selectedClassList;
        private JBList<PsiClass> databaseEntityClassList;
        private DefaultListModel<PsiClass> selectedClassListModel;
        private DefaultListModel<PsiClass> databaseEntityClassListModel;
        private Project project;
        private JComboBox<String> matchTypeComboBox;

        protected ClassChooserDialog(Project project) {
            super(project);
            this.project = project;
            init();
            setTitle("Choose Classes");
        }

        @Override
        protected JComponent createCenterPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;

            // Match type combo box
            matchTypeComboBox = new JComboBox<>(new String[]{"Fuzzy", "Exact"});
            matchTypeComboBox.addActionListener(e -> {
                updateClassList(selectedClassSearchField, selectedClassListModel);
                updateClassList(databaseEntityClassSearchField, databaseEntityClassListModel);
            });
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 2;
            panel.add(matchTypeComboBox, gbc);

            // Selected Class
            gbc.gridy++;
            gbc.gridwidth = 1;
            panel.add(new JLabel("Selected Class:"), gbc);

            selectedClassSearchField = new JTextField();
            selectedClassSearchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                public void changedUpdate(javax.swing.event.DocumentEvent e) { updateClassList(selectedClassSearchField, selectedClassListModel); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { updateClassList(selectedClassSearchField, selectedClassListModel); }
                public void insertUpdate(javax.swing.event.DocumentEvent e) { updateClassList(selectedClassSearchField, selectedClassListModel); }
            });
            gbc.gridy++;
            panel.add(selectedClassSearchField, gbc);

            selectedClassListModel = new DefaultListModel<>();
            selectedClassList = new JBList<>(selectedClassListModel);
            selectedClassList.setCellRenderer(new ClassListCellRenderer());
            gbc.gridy++;
            gbc.weighty = 1.0;
            panel.add(new JBScrollPane(selectedClassList), gbc);

            // Database Entity Class
            gbc.gridy++;
            gbc.weighty = 0.0;
            panel.add(new JLabel("Database Entity Class:"), gbc);

            databaseEntityClassSearchField = new JTextField();
            databaseEntityClassSearchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                public void changedUpdate(javax.swing.event.DocumentEvent e) { updateClassList(databaseEntityClassSearchField, databaseEntityClassListModel); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { updateClassList(databaseEntityClassSearchField, databaseEntityClassListModel); }
                public void insertUpdate(javax.swing.event.DocumentEvent e) { updateClassList(databaseEntityClassSearchField, databaseEntityClassListModel); }
            });
            gbc.gridy++;
            panel.add(databaseEntityClassSearchField, gbc);

            databaseEntityClassListModel = new DefaultListModel<>();
            databaseEntityClassList = new JBList<>(databaseEntityClassListModel);
            databaseEntityClassList.setCellRenderer(new ClassListCellRenderer());
            gbc.gridy++;
            gbc.weighty = 1.0;
            panel.add(new JBScrollPane(databaseEntityClassList), gbc);

            return panel;
        }

        private void updateClassList(JTextField searchField, DefaultListModel<PsiClass> listModel) {
            String searchText = searchField.getText();
            listModel.clear();
            if (!searchText.isEmpty()) {
                boolean fuzzyMatch = matchTypeComboBox.getSelectedItem().equals("Fuzzy");
                List<PsiClass> matchingClasses = findMatchingClasses(project, searchText, fuzzyMatch);
                for (PsiClass psiClass : matchingClasses) {
                    listModel.addElement(psiClass);
                }
            }
        }

        public SelectedClasses getSelectedClasses() {
            if (isOK()) {
                return new SelectedClasses(
                    selectedClassList.getSelectedValue(),
                    databaseEntityClassList.getSelectedValue()
                );
            }
            return null;
        }

        @Override
        protected void doOKAction() {
            if (selectedClassList.getSelectedValue() == null || databaseEntityClassList.getSelectedValue() == null) {
                JOptionPane.showMessageDialog(getContentPanel(), 
                    "Please select both a Selected Class and a Database Entity Class.", 
                    "Invalid Selection", JOptionPane.ERROR_MESSAGE);
                return;
            }
            super.doOKAction();
        }
    }

    private static class ClassListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof PsiClass) {
                PsiClass psiClass = (PsiClass) value;
                setText(psiClass.getQualifiedName());
            }
            return c;
        }
    }
}