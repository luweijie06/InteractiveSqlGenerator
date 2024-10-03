package com.dev.gear.generator;

import com.dev.gear.FieldWithCondition;
import com.dev.gear.type.SqlType;
import com.intellij.psi.PsiClass;

import java.util.List;

public interface SqlGenerator {
    String generateSql(PsiClass selectedClass, List<FieldWithCondition> selectedFields, SqlType sqlType, PsiClass databaseEntityClass);
}