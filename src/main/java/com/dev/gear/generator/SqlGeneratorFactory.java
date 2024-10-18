package com.dev.gear.generator;

import com.dev.gear.type.OrmType;

public class SqlGeneratorFactory {

    public static SqlGenerator createSqlGenerator(OrmType ormType) {
        switch (ormType) {
            case MYBATIS:
                return new MyBatisSqlGenerator();
            case MYBATIS_PLUS:
                return new MyBatisPlusSqlGenerator();
            case JPA:
                return new JpaSqlGenerator();
            default:
                throw new IllegalArgumentException("Unsupported ORM type: " + ormType);
        }
    }

    private SqlGeneratorFactory() {
        throw new AssertionError("SqlGeneratorFactory is a utility class and should not be instantiated");
    }
}