package com.smartbi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("metric_definition")
public class MetricDefinition {
    /**
     * Primary key.
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * Stable metric code used in semantic prompt.
     */
    private String metricCode;
    /**
     * Human-readable metric name.
     */
    private String metricName;
    /**
     * Optional metric SQL template/snippet.
     */
    private String metricSql;
    /**
     * Business definition for this metric.
     */
    private String description;
}
