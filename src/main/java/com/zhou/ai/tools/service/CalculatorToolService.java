package com.zhou.ai.tools.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 数学计算工具服务。
 * 使用 @Tool 注解声明工具能力，Spring AI 自动注册为可调用工具。
 */
@Component
public class CalculatorToolService {

    @Tool(description = "计算数学表达式，支持加减乘除和括号")
    public CalculationResult calculate(
            @ToolParam(description = "数学表达式，如 (3+5)*12") String expression) {
        try {
            double result = evaluateExpression(expression);
            return new CalculationResult(expression, result);
        } catch (Exception e) {
            return new CalculationResult(expression, Double.NaN);
        }
    }

    /**
     * 简单的数学表达式求值器（支持 +, -, *, /, 括号）。
     */
    private double evaluateExpression(String expression) {
        String sanitized = expression.replaceAll("[^0-9+\\-*/().\\s]", "");
        return new ExpressionParser(sanitized).parse();
    }

    /**
     * 计算结果记录。
     *
     * @param expression 数学表达式
     * @param result     计算结果
     */
    public record CalculationResult(String expression, double result) {
    }

    /**
     * 递归下降表达式解析器。
     */
    private static final class ExpressionParser {

        private final String input;
        private int pos;

        ExpressionParser(String input) {
            this.input = input;
            this.pos = 0;
        }

        double parse() {
            double result = parseTerm();
            while (pos < input.length()) {
                char c = input.charAt(pos);
                if (c == '+') {
                    pos++;
                    result += parseTerm();
                } else if (c == '-') {
                    pos++;
                    result -= parseTerm();
                } else {
                    break;
                }
            }
            return result;
        }

        private double parseTerm() {
            double result = parseFactor();
            while (pos < input.length()) {
                char c = input.charAt(pos);
                if (c == '*') {
                    pos++;
                    result *= parseFactor();
                } else if (c == '/') {
                    pos++;
                    result /= parseFactor();
                } else {
                    break;
                }
            }
            return result;
        }

        private double parseFactor() {
            skipWhitespace();
            if (pos < input.length() && input.charAt(pos) == '(') {
                pos++;
                double result = parse();
                pos++;
                return result;
            }
            int start = pos;
            if (pos < input.length() && input.charAt(pos) == '-') {
                pos++;
            }
            while (pos < input.length()
                    && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.')) {
                pos++;
            }
            return Double.parseDouble(input.substring(start, pos));
        }

        private void skipWhitespace() {
            while (pos < input.length() && input.charAt(pos) == ' ') {
                pos++;
            }
        }
    }
}
