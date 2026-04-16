package com.smartbi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartbi.entity.MetricDefinition;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MetricDefinitionMapper extends BaseMapper<MetricDefinition> {
    // Uses MyBatis-Plus generic CRUD methods for semantic metric table.
}
