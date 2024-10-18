package com.dev.gear.generator;

import com.dev.gear.FieldWithCondition;
import com.dev.gear.type.SqlType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MyBatisPlusSqlGenerator implements SqlGenerator {

    @Override
    public String generateSql(PsiClass selectedClass, List<FieldWithCondition> selectedFields, SqlType sqlType, PsiClass databaseEntityClass) {
        StringBuilder java = new StringBuilder();
        String methodName = sqlType.name().toLowerCase() + databaseEntityClass.getName();
        Set<String> databaseFieldNames = Arrays.stream(databaseEntityClass.getAllFields())
                .map(PsiField::getName)
                .collect(Collectors.toSet());

        java.append("public ");

        switch (sqlType) {
            case SELECT:
            case SELECT_PAGE:
                java.append(generateSelectMethod(methodName, selectedClass, databaseEntityClass, selectedFields, sqlType));
                break;
            case UPDATE:
                java.append(generateUpdateMethod(methodName, selectedClass, databaseEntityClass, databaseFieldNames, selectedFields));
                break;
            case DELETE:
                java.append(generateDeleteMethod(methodName, selectedClass, databaseEntityClass, selectedFields));
                break;
            case INSERT:
                java.append(generateInsertMethod(methodName, selectedClass, selectedFields));
                break;
        }

        return java.toString();
    }

    private String generateSelectMethod(String methodName, PsiClass selectedClass, PsiClass databaseEntityClass, 
                                        List<FieldWithCondition> selectedFields, SqlType sqlType) {
        StringBuilder method = new StringBuilder();
        method.append("List<").append(databaseEntityClass.getName()).append("> ");
        method.append(methodName).append("(").append(selectedClass.getName()).append(" entity) {\n");
        method.append("    if (entity == null) {\n");
        method.append("        throw new IllegalArgumentException(\"Entity must not be null\");\n");
        method.append("    }\n");
        if (sqlType != SqlType.SELECT_PAGE) {
            method.append(generateFieldValidations(selectedFields, sqlType));
        }

        method.append("    return this.lambdaQuery()\n");
        method.append(generateMybatisPlusWhereClause(selectedFields, databaseEntityClass));
        method.append("        .list();\n");
        method.append("}");
        return method.toString();
    }

    private String generateUpdateMethod(String methodName, PsiClass selectedClass, PsiClass databaseEntityClass,
                                        Set<String> databaseFieldNames, List<FieldWithCondition> selectedFields) {
        StringBuilder method = new StringBuilder();
        method.append("boolean ").append(methodName).append("(").append(selectedClass.getName()).append(" entity) {\n");
        method.append("    if (entity == null) {\n");
        method.append("        throw new IllegalArgumentException(\"Entity must not be null\");\n");
        method.append("    }\n");
        method.append(generateFieldValidations(selectedFields, SqlType.UPDATE));
        method.append("    return this.lambdaUpdate()\n");

        for (PsiField field : selectedClass.getAllFields()) {
            String fieldName = field.getName();
            String typeName = field.getType().getPresentableText();
            if (databaseFieldNames.contains(fieldName)) {
                method.append("        .set(");
                method.append(generateSetCondition(fieldName, typeName));
                method.append(", ")
                        .append(databaseEntityClass.getName()).append("::get").append(capitalize(fieldName))
                        .append(", entity.get").append(capitalize(fieldName)).append("())\n");
            }
        }
        method.append(generateMybatisPlusWhereClause(selectedFields, databaseEntityClass));
        method.append("        .update(new ").append(databaseEntityClass.getName()).append("());\n");
        method.append("}");
        return method.toString();
    }

    private String generateDeleteMethod(String methodName, PsiClass selectedClass, PsiClass databaseEntityClass, 
                                        List<FieldWithCondition> selectedFields) {
        StringBuilder method = new StringBuilder();
        method.append("boolean ").append(methodName).append("(").append(selectedClass.getName()).append(" entity) {\n");
        method.append("    if (entity == null) {\n");
        method.append("        throw new IllegalArgumentException(\"Entity must not be null\");\n");
        method.append("    }\n");
        method.append(generateFieldValidations(selectedFields, SqlType.DELETE));
        method.append("    return this.lambdaUpdate()\n");
        method.append(generateMybatisPlusWhereClause(selectedFields, databaseEntityClass));
        method.append("        .remove();\n");
        method.append("}");
        return method.toString();
    }

    private String generateInsertMethod(String methodName, PsiClass selectedClass, List<FieldWithCondition> selectedFields) {
        StringBuilder method = new StringBuilder();
        method.append("boolean ").append(methodName).append("(").append(selectedClass.getName()).append(" entity) {\n");
        method.append("    if (entity == null) {\n");
        method.append("        throw new IllegalArgumentException(\"Entity must not be null\");\n");
        method.append("    }\n");
        method.append(generateFieldValidations(selectedFields, SqlType.INSERT));
        method.append("    return this.save(entity);\n");
        method.append("}");
        return method.toString();
    }

    private String generateFieldValidations(List<FieldWithCondition> selectedFields, SqlType sqlType) {
        StringBuilder validations = new StringBuilder();
        if (!sqlType.equals(SqlType.SELECT) && !sqlType.equals(SqlType.SELECT_PAGE)) {
            for (FieldWithCondition fwc : selectedFields) {
                validations.append(generateFieldValidation(fwc));
            }
        } else {
            validations.append("    if (");
            List<String> fieldChecks = selectedFields.stream()
                    .map(this::generateFieldCheck)
                    .collect(Collectors.toList());
            validations.append(String.join(" &&\n            ", fieldChecks));
            validations.append(") {\n");
            validations.append("        throw new IllegalArgumentException(\"At least one search criteria must be provided\");\n");
            validations.append("    }\n");
        }
        return validations.toString();
    }

    private String generateFieldValidation(FieldWithCondition fwc) {
        String fieldName = fwc.getField().getName();
        String typeName = fwc.getField().getType().getPresentableText();
        StringBuilder validation = new StringBuilder("    if (");
        
        if (typeName.equals("String")) {
            validation.append("StringUtils.isEmpty(entity.get").append(capitalize(fieldName)).append("())");
        } else if (typeName.contains("List") || typeName.contains("Set") || typeName.contains("Collection")) {
            validation.append("CollectionUtils.isEmpty(entity.get").append(capitalize(fieldName)).append("())");
        } else {
            validation.append("entity.get").append(capitalize(fieldName)).append("() == null");
        }
        
        validation.append(") {\n");
        validation.append("        throw new IllegalArgumentException(\"")
                .append(fieldName).append(" must not be null or empty\");\n");
        validation.append("    }\n");
        return validation.toString();
    }

    private String generateFieldCheck(FieldWithCondition fwc) {
        String fieldName = fwc.getField().getName();
        String typeName = fwc.getField().getType().getPresentableText();
        StringBuilder fieldCheck = new StringBuilder();
        
        if (typeName.equals("String")) {
            fieldCheck.append("StringUtils.isEmpty(entity.get").append(capitalize(fieldName)).append("())");
        } else if (typeName.contains("List") || typeName.contains("Set") || typeName.contains("Collection")) {
            fieldCheck.append("CollectionUtils.isEmpty(entity.get").append(capitalize(fieldName)).append("())");
        } else {
            fieldCheck.append("entity.get").append(capitalize(fieldName)).append("() == null");
        }
        
        return fieldCheck.toString();
    }

    private String generateMybatisPlusWhereClause(List<FieldWithCondition> selectedFields, PsiClass databaseEntityClass) {
        StringBuilder whereClause = new StringBuilder();
        for (int i = 0; i < selectedFields.size(); i++) {
            FieldWithCondition fwc = selectedFields.get(i);
            String fieldName = fwc.getField().getName();
            String condition = fwc.getCondition();
            String typeName = fwc.getField().getType().getPresentableText();

            if (i > 0 && fwc.getConnection().equalsIgnoreCase("OR")) {
                whereClause.append("        .or()\n");
            }

            whereClause.append("        .").append(getConditionMethod(condition)).append("(");
            whereClause.append(generateFieldCondition(fieldName, typeName));
            whereClause.append(", ")
                    .append(databaseEntityClass.getName())
                    .append("::get")
                    .append(capitalize(fwc.getDatabaseField()))
                    .append(", entity.get")
                    .append(capitalize(fieldName))
                    .append("())\n");
        }
        return whereClause.toString();
    }

    private String generateSetCondition(String fieldName, String typeName) {
        StringBuilder condition = new StringBuilder();
        if (typeName.equals("String")) {
            condition.append("StringUtils.isNotEmpty(entity.get").append(capitalize(fieldName)).append("())");
        } else if (typeName.contains("List") || typeName.contains("Set") || typeName.contains("Collection")) {
            condition.append("CollectionUtils.isNotEmpty(entity.get").append(capitalize(fieldName)).append("())");
        } else {
            condition.append("entity.get").append(capitalize(fieldName)).append("() != null");
        }
        return condition.toString();
    }

    private String generateFieldCondition(String fieldName, String typeName) {
        return generateSetCondition(fieldName, typeName);
    }

    private String getConditionMethod(String condition) {
        switch (condition.toLowerCase()) {
            case "=": return "eq";
            case "!=": return "ne";
            case "<": return "lt";
            case "<=": return "le";
            case ">": return "gt";
            case ">=": return "ge";
            case "like": return "like";
            case "in": return "in";
            default: return "eq";
        }
    }

    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}