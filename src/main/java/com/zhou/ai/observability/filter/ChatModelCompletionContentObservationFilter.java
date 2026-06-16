package com.zhou.ai.observability.filter;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationFilter;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.content.Content;
import org.springframework.ai.observation.ObservabilityHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 自定义观测过滤器：将 prompt 和 completion 内容添加到 Trace 的 high-cardinality 指标中。
 * 来自 Langfuse 官方示例，确保 Langfuse UI 中能显示完整的对话内容。
 */
@Component
public class ChatModelCompletionContentObservationFilter implements ObservationFilter {

    @Override
    public Observation.Context map(Observation.Context context) {
        if (!(context instanceof ChatModelObservationContext chatModelObservationContext)) {
            return context;
        }

        var prompts = processPrompts(chatModelObservationContext);
        var completions = processCompletion(chatModelObservationContext);

        chatModelObservationContext.addHighCardinalityKeyValue(new KeyValue() {
            @Override
            public String getKey() {
                return "gen_ai.prompt";
            }

            @Override
            public String getValue() {
                return ObservabilityHelper.concatenateStrings(prompts);
            }
        });

        chatModelObservationContext.addHighCardinalityKeyValue(new KeyValue() {
            @Override
            public String getKey() {
                return "gen_ai.completion";
            }

            @Override
            public String getValue() {
                return ObservabilityHelper.concatenateStrings(completions);
            }
        });

        return chatModelObservationContext;
    }

    private List<String> processPrompts(ChatModelObservationContext chatModelObservationContext) {
        return CollectionUtils.isEmpty((chatModelObservationContext.getRequest()).getInstructions())
                ? List.of()
                : (chatModelObservationContext.getRequest()).getInstructions().stream()
                    .map(Content::getText).toList();
    }

    private List<String> processCompletion(ChatModelObservationContext context) {
        if (context.getResponse() != null && (context.getResponse()).getResults() != null
                && !CollectionUtils.isEmpty((context.getResponse()).getResults())) {
            return !StringUtils.hasText((context.getResponse()).getResult().getOutput().getText())
                    ? List.of()
                    : (context.getResponse()).getResults().stream()
                        .filter(generation -> generation.getOutput() != null
                                && StringUtils.hasText(generation.getOutput().getText()))
                        .map(generation -> generation.getOutput().getText())
                        .toList();
        } else {
            return List.of();
        }
    }
}
