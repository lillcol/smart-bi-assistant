package com.smartbi.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("table_schema")
public class TableSchema {
    /**
     * Physical table name.
     */
    private String tableName;
    /**
     * Physical column name.
     */
    private String columnName;
    /**
     * Column business description for prompt context.
     */
    private String columnDesc;
}
