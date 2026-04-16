package com.smartbi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartbi.entity.TableSchema;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TableSchemaMapper extends BaseMapper<TableSchema> {
    // Uses MyBatis-Plus generic CRUD methods for schema metadata table.
}
