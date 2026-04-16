package com.smartbi.sql;

import com.smartbi.config.QueryProperties;

public class PromptBuilder {

    private PromptBuilder() {
    }

    /**
     * Builds the final NL2SQL prompt with:
     * - available schema metadata
     * - metric semantic definitions
     * - explicit SQL safety and output constraints
     */
    public static String build(String question, String schema, String metrics, QueryProperties props) {
        return String.format(
                "你是一个数据分析助手，请根据提供的信息生成 MySQL SQL。%n%n"
                        + "【表结构】%n%s%n%n"
                        + "【指标定义】%n%s%n%n"
                        + "【要求】%n"
                        + "1. 只能使用已有表和字段%n"
                        + "2. SQL必须可执行%n"
                        + "3. 只能生成SELECT查询%n"
                        + "4. 必须包含时间过滤（优先使用表中描述为支付时间/创建时间的字段，如 pay_time/created_at；如存在多个时间字段，选最相关的那个）%n"
                        + "5. 必须包含LIMIT，且不超过 %d%n"
                        + "6. 输出只能是一段纯 SQL（以 SELECT 开头），不能包含任何解释、Markdown、代码块（不要```）%n%n"
                        + "用户问题：%n%s%n",
                schema, metrics, props.getMaxLimit(), question
        );
    }
}
