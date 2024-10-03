package com.dev.gear.generator;

import com.dev.gear.FieldWithCondition;
import com.dev.gear.type.SqlType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MyBatisSqlGenerator implements SqlGenerator {

    @Override
    public String generateSql(PsiClass selectedClass, List<FieldWithCondition> selectedFields, SqlType sqlType, PsiClass databaseEntityClass) {
        String tableName = camelToSnakeCase(databaseEntityClass.getName());
        Set<String> databaseFieldNames = Arrays.stream(databaseEntityClass.getAllFields())
                .map(PsiField::getName)
                .collect(Collectors.toSet());

        StringBuilder xml = new StringBuilder();
        String methodName = sqlType.name().toLowerCase() + selectedClass.getName();

        switch (sqlType) {
            case SELECT:
            case SELECT_PAGE:
                xml.append(generateSelectSql(methodName, databaseEntityClass, tableName, databaseFieldNames, selectedFields, sqlType));
                break;
            case UPDATE:
                xml.append(generateUpdateSql(methodName, selectedClass, tableName, databaseFieldNames, selectedFields));
                break;
            case DELETE:
                xml.append(generateDeleteSql(methodName, tableName, selectedFields));
                break;
            case INSERT:
                xml.append(generateInsertSql(methodName, tableName, selectedFields));
                break;
        }

        return xml.toString();
    }

    private String generateSelectSql(String methodName, PsiClass databaseEntityClass, String tableName, 
                                     Set<String> databaseFieldNames, List<FieldWithCondition> selectedFields, SqlType sqlType) {
        StringBuilder xml = new StringBuilder();
        xml.append("<select id=\"").append(methodName).append("\" ");
        xml.append("resultType=\"").append(databaseEntityClass.getQualifiedName()).append("\">\n");
        xml.append("    SELECT ");
        xml.append(String.join(", ", databaseFieldNames));
        xml.append("\n    FROM ").append(tableName);
        xml.append("\n    <where>\n");
        xml.append(generateWhereClause(selectedFields, sqlType));
        xml.append("    </where>\n");
        xml.append("</select>");
        return xml.toString();
    }

    private String generateUpdateSql(String methodName, PsiClass selectedClass, String tableName, 
                                     Set<String> databaseFieldNames, List<FieldWithCondition> selectedFields) {
        StringBuilder xml = new StringBuilder();
        xml.append("<update id=\"").append(methodName).append("\">\n");
        xml.append("    UPDATE ").append(tableName).append("\n");
        xml.append("    <set>\n");

        for (PsiField field : selectedClass.getAllFields()) {
            String fieldName = field.getName();
            String typeName = field.getType().getPresentableText();

            if (databaseFieldNames.contains(fieldName)) {
                String databaseField = camelToSnakeCase(fieldName);
                xml.append(generateUpdateSetClause(fieldName, typeName, databaseField));
            }
        }

        xml.append("    </set>\n");
        xml.append("    <where>\n");
        xml.append(generateWhereClause(selectedFields, SqlType.UPDATE));
        xml.append("    </where>\n");
        xml.append("</update>");
        return xml.toString();
    }

    private String generateDeleteSql(String methodName, String tableName, List<FieldWithCondition> selectedFields) {
        StringBuilder xml = new StringBuilder();
        xml.append("<delete id=\"").append(methodName).append("\">\n");
        xml.append("    <if test=\"");
        for (int i = 0; i < selectedFields.size(); i++) {
            if (i > 0) xml.append(" or ");
            xml.append(selectedFields.get(i).field.getName()).append(" != null");
        }
        xml.append("\">\n");
        xml.append("        DELETE FROM ").append(tableName).append("\n");
        xml.append("        <where>\n");
        xml.append(generateWhereClause(selectedFields, SqlType.DELETE));
        xml.append("        </where>\n");
        xml.append("    </if>\n");
        xml.append("</delete>");
        return xml.toString();
    }

    private String generateInsertSql(String methodName, String tableName, List<FieldWithCondition> selectedFields) {
        StringBuilder xml = new StringBuilder();
        xml.append("<insert id=\"").append(methodName).append("\">\n");
        xml.append("    INSERT INTO ").append(tableName).append("\n");
        xml.append("    <trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\">\n");
        for (FieldWithCondition fwc : selectedFields) {
            xml.append(generateInsertColumnClause(fwc));
        }
        xml.append("    </trim>\n");
        xml.append("    <trim prefix=\"VALUES (\" suffix=\")\" suffixOverrides=\",\">\n");
        for (FieldWithCondition fwc : selectedFields) {
            xml.append(generateInsertValueClause(fwc));
        }
        xml.append("    </trim>\n");
        xml.append("</insert>");
        return xml.toString();
    }

    private String generateWhereClause(List<FieldWithCondition> selectedFields, SqlType sqlType) {
        StringBuilder whereClause = new StringBuilder();
        if (!SqlType.SELECT_PAGE.equals(sqlType)) {
            whereClause.append(generateOuterIfCondition(selectedFields, sqlType));
        }
        for (FieldWithCondition fwc : selectedFields) {
            whereClause.append(generateFieldCondition(fwc));
        }
        return whereClause.toString();
    }

    private String generateOuterIfCondition(List<FieldWithCondition> selectedFields, SqlType sqlType) {
        StringBuilder condition = new StringBuilder("            <if test=\"");
        List<String> conditions = new ArrayList<>();
        for (FieldWithCondition fwc : selectedFields) {
            conditions.add(generateFieldNullOrEmptyCheck(fwc));
        }

        String joinOperator = (sqlType.equals(SqlType.DELETE) || sqlType.equals(SqlType.UPDATE)) ? " or " : " and ";

        if (conditions.size() == 1) {
            condition.append(conditions.get(0));
        } else {
            condition.append("\n");
            for (int i = 0; i < conditions.size(); i++) {
                condition.append("            (").append(conditions.get(i)).append(")");
                if (i < conditions.size() - 1) {
                    condition.append(joinOperator).append("\n");
                }
            }
            condition.append("\n        ");
        }

        condition.append("\">\n");
        condition.append("                1=0 <!-- If all fields are null or empty, return empty result -->\n");
        condition.append("            </if>\n");
        return condition.toString();
    }

    private String generateFieldNullOrEmptyCheck(FieldWithCondition fwc) {
        String fieldName = fwc.field.getName();
        String typeName = fwc.field.getType().getPresentableText();

        StringBuilder condition = new StringBuilder(fieldName).append(" == null");
        if (typeName.equals("String")) {
            condition.append(" or ").append(fieldName).append(" == ''");
        } else if (typeName.contains("List") || typeName.contains("Set") || typeName.contains("Collection")) {
            condition.append(" or ").append(fieldName).append(".isEmpty()");
        }
        return condition.toString();
    }

    private String generateFieldCondition(FieldWithCondition fwc) {
        String fieldName = fwc.field.getName();
        String databaseField = camelToSnakeCase(fwc.databaseField);
        String condition = fwc.condition;
        String typeName = fwc.field.getType().getPresentableText();

        StringBuilder fieldCondition = new StringBuilder();
        fieldCondition.append("            <if test=\"").append(fieldName).append(" != null");

        if (typeName.equals("String")) {
            fieldCondition.append(" and ").append(fieldName).append(" != ''");
        } else if (typeName.contains("List") || typeName.contains("Set") || typeName.contains("Collection")) {
            fieldCondition.append(" and !").append(fieldName).append(".isEmpty()");
        }

        fieldCondition.append("\">\n");
        fieldCondition.append("                ").append(fwc.connection).append(" ");

        if (condition.equalsIgnoreCase("LIKE")) {
            fieldCondition.append(databaseField).append(" LIKE CONCAT('%', #{").append(fieldName).append("}, '%')\n");
        } else if (condition.equalsIgnoreCase("in")) {
            fieldCondition.append(databaseField).append(" IN\n");
            fieldCondition.append("                <foreach item=\"item\" index=\"index\" collection=\"").append(fieldName).append("\"\n");
            fieldCondition.append("                         open=\"(\" separator=\",\" close=\")\">\n");
            fieldCondition.append("                    #{item}\n");
            fieldCondition.append("                </foreach>\n");
        } else {
            fieldCondition.append(databaseField).append(" ").append(escapeXmlSpecialChars(condition)).append(" #{").append(fieldName).append("}\n");
        }
        fieldCondition.append("            </if>\n");

        return fieldCondition.toString();
    }

    private String generateUpdateSetClause(String fieldName, String typeName, String databaseField) {
        StringBuilder clause = new StringBuilder();
        clause.append("        <if test=\"").append(fieldName).append(" != null");
        if (typeName.equals("String")) {
            clause.append(" and ").append(fieldName).append(" != ''");
        } else if (typeName.contains("List") || typeName.contains("Set") || typeName.contains("Collection")) {
            clause.append(" and !").append(fieldName).append(".isEmpty()");
        }
        clause.append("\">\n");
        clause.append("            ").append(databaseField).append(" = #{").append(fieldName).append("},\n");
        clause.append("        </if>\n");
        return clause.toString();
    }

    private String generateInsertColumnClause(FieldWithCondition fwc) {
        String fieldName = fwc.field.getName();
        String databaseField = camelToSnakeCase(fwc.databaseField);
        String typeName = fwc.field.getType().getPresentableText();

        StringBuilder clause = new StringBuilder();
        clause.append("        <if test=\"").append(fieldName).append(" != null");
        if (typeName.equals("String")) {
            clause.append(" and ").append(fieldName).append(" != ''");
        } else if (typeName.contains("List") || typeName.contains("Set") || typeName.contains("Collection")) {
            clause.append(" and !").append(fieldName).append(".isEmpty()");
        }
        clause.append("\">\n");
        clause.append("            ").append(databaseField).append(",\n");
        clause.append("        </if>\n");
        return clause.toString();
    }

    private String generateInsertValueClause(FieldWithCondition fwc) {
        String fieldName = fwc.field.getName();
        String typeName = fwc.field.getType().getPresentableText();

        StringBuilder clause = new StringBuilder();
        clause.append("        <if test=\"").append(fieldName).append(" != null");
        if (typeName.equals("String")) {
            clause.append(" and ").append(fieldName).append(" != ''");
        } else if (typeName.contains("List") || typeName.contains("Set") || typeName.contains("Collection")) {
            clause.append(" and !").append(fieldName).append(".isEmpty()");
        }
        clause.append("\">\n");
        clause.append("            #{").append(fieldName).append("},\n");
        clause.append("        </if>\n");
        return clause.toString();
    }

    private String escapeXmlSpecialChars(String input) {
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String camelToSnakeCase(String str) {
        return str.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}