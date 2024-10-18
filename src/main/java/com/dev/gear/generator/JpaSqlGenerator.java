package com.dev.gear.generator;

import com.dev.gear.FieldWithCondition;
import com.dev.gear.type.SqlType;
import com.intellij.psi.PsiClass;
import java.util.List;
import java.util.stream.Collectors;

public class JpaSqlGenerator implements SqlGenerator {

    @Override
    public String generateSql(PsiClass selectedClass, List<FieldWithCondition> selectedFields, SqlType sqlType, PsiClass databaseEntityClass) {
        StringBuilder java = new StringBuilder();
        String methodName = "get" + sqlType.name().toLowerCase() + "Specification";

        java.append("public static Specification<").append(databaseEntityClass.getName()).append("> ")
                .append(methodName).append("(").append(selectedClass.getName()).append(" entity) {\n")
                .append("    if (entity == null) {\n")
                .append("        throw new IllegalArgumentException(\"Entity must not be null\");\n")
                .append("    }\n")
                .append(generateValidations(selectedFields, sqlType))
                .append("    return (root, query, cb) -> {\n")
                .append("        List<Predicate> predicates = new ArrayList<>();\n\n");

        java.append(generatePredicates(selectedFields));

        java.append("        return cb.and(predicates.toArray(new Predicate[0]));\n")
                .append("    };\n")
                .append("}");

        return java.toString();
    }

    private String generateValidations(List<FieldWithCondition> selectedFields, SqlType sqlType) {
        StringBuilder validations = new StringBuilder();
        switch (sqlType) {
            case SELECT:
            case SELECT_PAGE:
                validations.append(generateSelectValidation(selectedFields, sqlType));
                break;
            case UPDATE:
            case DELETE:
            case INSERT:
                validations.append(generateNonSelectValidation(selectedFields, sqlType));
                break;
        }
        return validations.toString();
    }

    private String generateSelectValidation(List<FieldWithCondition> selectedFields, SqlType sqlType) {
        if (SqlType.SELECT_PAGE == sqlType) {
            return "";
        }
        StringBuilder validation = new StringBuilder("    if (");
        List<String> fieldChecks = selectedFields.stream()
                .map(this::generateFieldCheck)
                .collect(Collectors.toList());
        validation.append(String.join(" &&\n        ", fieldChecks));
        validation.append(") {\n");
        validation.append("        throw new IllegalArgumentException(\"At least one search criteria must be provided\");\n");
        validation.append("    }\n");
        return validation.toString();
    }

    private String generateNonSelectValidation(List<FieldWithCondition> selectedFields, SqlType sqlType) {
        if (SqlType.SELECT_PAGE == sqlType) {
            return "";
        }
        StringBuilder validation = new StringBuilder();
        for (FieldWithCondition fwc : selectedFields) {
            validation.append(generateFieldValidation(fwc));
        }
        return validation.toString();
    }

    private String generateFieldCheck(FieldWithCondition fwc) {
        String fieldName = fwc.getField().getName();
        String typeName = fwc.getField().getType().getPresentableText();
        StringBuilder fieldCheck = new StringBuilder();
        fieldCheck.append("(");
        if (typeName.equals("String")) {
            fieldCheck.append("StringUtils.isEmpty(entity.get").append(capitalize(fieldName)).append("())");
        } else if (typeName.contains("List") || typeName.contains("Set") || typeName.contains("Collection")) {
            fieldCheck.append("CollectionUtils.isEmpty(entity.get").append(capitalize(fieldName)).append("())");
        } else {
            fieldCheck.append("entity.get").append(capitalize(fieldName)).append("() == null");
        }
        fieldCheck.append(")");
        return fieldCheck.toString();
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

    private String generatePredicates(List<FieldWithCondition> selectedFields) {
        StringBuilder predicates = new StringBuilder();
        for (FieldWithCondition fwc : selectedFields) {
            String fieldName = fwc.getField().getName();
            String condition = fwc.getCondition();
            String typeName = fwc.getField().getType().getPresentableText();

            predicates.append("        if (");

            // Add type-specific validation
            if (typeName.equals("String")) {
                predicates.append("StringUtils.isNotEmpty(entity.get").append(capitalize(fieldName)).append("())");
            } else if (typeName.contains("List") || typeName.contains("Set") || typeName.contains("Collection")) {
                predicates.append("CollectionUtils.isNotEmpty(entity.get").append(capitalize(fieldName)).append("())");
            } else {
                predicates.append("entity.get").append(capitalize(fieldName)).append("() != null");
            }

            predicates.append(") {\n")
                    .append("            predicates.add(").append(getJpaPredicateMethod(condition, "root", fieldName, typeName)).append(");\n")
                    .append("        }\n");
        }
        return predicates.toString();
    }


    private String getJpaPredicateMethod(String condition, String root, String fieldName, String typeName) {
        switch (condition.toLowerCase()) {
            case "=": return "cb.equal(" + root + ".get(\"" + fieldName + "\"), entity.get" + capitalize(fieldName) + "())";
            case "!=": return "cb.notEqual(" + root + ".get(\"" + fieldName + "\"), entity.get" + capitalize(fieldName) + "())";
            case "<": return "cb.lessThan(" + root + ".get(\"" + fieldName + "\"), entity.get" + capitalize(fieldName) + "())";
            case "<=": return "cb.lessThanOrEqualTo(" + root + ".get(\"" + fieldName + "\"), entity.get" + capitalize(fieldName) + "())";
            case ">": return "cb.greaterThan(" + root + ".get(\"" + fieldName + "\"), entity.get" + capitalize(fieldName) + "())";
            case ">=": return "cb.greaterThanOrEqualTo(" + root + ".get(\"" + fieldName + "\"), entity.get" + capitalize(fieldName) + "())";
            case "like":
                if (typeName.equals("String")) {
                    return "cb.like(" + root + ".get(\"" + fieldName + "\"), \"%\" + entity.get" + capitalize(fieldName) + "() + \"%\")";
                } else {
                    throw new IllegalArgumentException("LIKE operation is only applicable to String fields");
                }
            case "in":
                if (typeName.contains("List") || typeName.contains("Set") || typeName.contains("Collection")) {
                    return root + ".get(\"" + fieldName + "\").in(entity.get" + capitalize(fieldName) + "())";
                } else {
                    throw new IllegalArgumentException("IN operation is only applicable to Collection fields");
                }
            default: return "cb.equal(" + root + ".get(\"" + fieldName + "\"), entity.get" + capitalize(fieldName) + "())";
        }
    }

    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}