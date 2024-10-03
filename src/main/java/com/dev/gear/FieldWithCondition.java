package com.dev.gear;

import com.intellij.psi.PsiField;

public class FieldWithCondition {
    public final PsiField field;
    public final String condition;
    public final String connection;
    public final String databaseField;

    public FieldWithCondition(PsiField field, String condition, String connection, String databaseField) {
        this.field = field;
        this.condition = condition;
        this.connection = connection;
        this.databaseField = databaseField;
    }

    // Getter methods
    public PsiField getField() {
        return field;
    }

    public String getCondition() {
        return condition;
    }

    public String getConnection() {
        return connection;
    }

    public String getDatabaseField() {
        return databaseField;
    }

    // You might want to override equals and hashCode methods if you plan to use this in collections
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FieldWithCondition that = (FieldWithCondition) o;

        if (!field.equals(that.field)) return false;
        if (!condition.equals(that.condition)) return false;
        if (!connection.equals(that.connection)) return false;
        return databaseField.equals(that.databaseField);
    }

    @Override
    public int hashCode() {
        int result = field.hashCode();
        result = 31 * result + condition.hashCode();
        result = 31 * result + connection.hashCode();
        result = 31 * result + databaseField.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "FieldWithCondition{" +
                "field=" + field.getName() +
                ", condition='" + condition + '\'' +
                ", connection='" + connection + '\'' +
                ", databaseField='" + databaseField + '\'' +
                '}';
    }
}