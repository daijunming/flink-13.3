/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.flink.connector.jdbc.dialect;

import org.apache.flink.connector.jdbc.internal.converter.JdbcRowConverter;
import org.apache.flink.connector.jdbc.internal.converter.OracleRowConverter;
import org.apache.flink.table.types.logical.LogicalTypeRoot;
import org.apache.flink.table.types.logical.RowType;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** JDBC dialect for Oracle. */
public class OracleDialect extends AbstractDialect {

    private static final long serialVersionUID = 1L;

    // Define MAX/MIN precision of TIMESTAMP type according to Oracle docs:
    // https://www.techonthenet.com/oracle/datatypes.php
    private static final int MAX_TIMESTAMP_PRECISION = 9;
    private static final int MIN_TIMESTAMP_PRECISION = 1;

    // Define MAX/MIN precision of DECIMAL type according to Mysql docs:
    // https://dev.mysql.com/doc/refman/8.0/en/fixed-point-types.html
    private static final int MAX_DECIMAL_PRECISION = 38;
    private static final int MIN_DECIMAL_PRECISION = 1;

    // jdbc:oracle:thin:@127.0.0.1:1521:ORCL
    @Override
    public boolean canHandle(String url) {
        return url.startsWith("jdbc:oracle:thin:");
    }

    @Override
    public JdbcRowConverter getRowConverter(RowType rowType) {
        return new OracleRowConverter(rowType);
    }

    @Override
    public String getLimitClause(long limit) {
        return "FETCH FIRST " + limit + " ROWS ONLY";
    }

    @Override
    public Optional<String> defaultDriverName() {
        return Optional.of("oracle.jdbc.OracleDriver");
    }

    @Override
    public String quoteIdentifier(String identifier) {
        return identifier;
    }

    @Override
    public Optional<String> getUpsertStatement(
            String tableName, String[] fieldNames, String[] uniqueKeyFields) {

        String sourceFields =
                Arrays.stream(fieldNames)
                        .map(f -> ":" + f + " " + quoteIdentifier(f))
                        .collect(Collectors.joining(", "));

        String onClause =
                Arrays.stream(uniqueKeyFields)
                        .map(f -> "t." + quoteIdentifier(f) + "=s." + quoteIdentifier(f))
                        .collect(Collectors.joining(" and "));

        final Set<String> uniqueKeyFieldsSet =
                Arrays.stream(uniqueKeyFields).collect(Collectors.toSet());
        String updateClause =
                Arrays.stream(fieldNames)
                        .filter(f -> !uniqueKeyFieldsSet.contains(f))
                        .map(f -> "t." + quoteIdentifier(f) + "=s." + quoteIdentifier(f))
                        .collect(Collectors.joining(", "));

        String insertFields =
                Arrays.stream(fieldNames)
                        .map(this::quoteIdentifier)
                        .collect(Collectors.joining(", "));

        String valuesClause =
                Arrays.stream(fieldNames)
                        .map(f -> "s." + quoteIdentifier(f))
                        .collect(Collectors.joining(", "));

        // if we can't divide schema and table-name is risky to call quoteIdentifier(tableName)
        // for example [tbo].[sometable] is ok but [tbo.sometable] is not
        String mergeQuery =
                " MERGE INTO "
                        + tableName
                        + " t "
                        + " USING (SELECT "
                        + sourceFields
                        + " FROM DUAL) s "
                        + " ON ("
                        + onClause
                        + ") "
                        + " WHEN MATCHED THEN UPDATE SET "
                        + updateClause
                        + " WHEN NOT MATCHED THEN INSERT ("
                        + insertFields
                        + ")"
                        + " VALUES ("
                        + valuesClause
                        + ")";

        return Optional.of(mergeQuery);
    }

    @Override
    public String dialectName() {
        return "Oracle";
    }

    @Override
    public int maxDecimalPrecision() {
        return MAX_DECIMAL_PRECISION;
    }

    @Override
    public int minDecimalPrecision() {
        return MIN_DECIMAL_PRECISION;
    }

    @Override
    public int maxTimestampPrecision() {
        return MAX_TIMESTAMP_PRECISION;
    }

    @Override
    public int minTimestampPrecision() {
        return MIN_TIMESTAMP_PRECISION;
    }

    @Override
    public List<LogicalTypeRoot> unsupportedTypes() {

        // TODO: We can't convert BINARY data type to
        // PrimitiveArrayTypeInfo.BYTE_PRIMITIVE_ARRAY_TYPE_INFO in
        // LegacyTypeInfoDataTypeConverter.
        return Arrays.asList(
                LogicalTypeRoot.CHAR,
                LogicalTypeRoot.VARCHAR,
                LogicalTypeRoot.BOOLEAN,
                LogicalTypeRoot.VARBINARY,
                LogicalTypeRoot.DECIMAL,
                LogicalTypeRoot.TINYINT,
                LogicalTypeRoot.SMALLINT,
                LogicalTypeRoot.INTEGER,
                LogicalTypeRoot.BIGINT,
                LogicalTypeRoot.FLOAT,
                LogicalTypeRoot.DOUBLE,
                LogicalTypeRoot.DATE,
                LogicalTypeRoot.TIME_WITHOUT_TIME_ZONE,
                LogicalTypeRoot.TIMESTAMP_WITHOUT_TIME_ZONE,
                LogicalTypeRoot.TIMESTAMP_WITH_LOCAL_TIME_ZONE,
                LogicalTypeRoot.ARRAY);
    }
}
