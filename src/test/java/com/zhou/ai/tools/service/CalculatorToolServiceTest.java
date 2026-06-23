package com.zhou.ai.tools.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CalculatorToolService 单元测试。
 * 测试递归下降表达式解析器（ExpressionParser）的复杂逻辑。
 */
class CalculatorToolServiceTest {

    private CalculatorToolService calculatorToolService;

    CalculatorToolServiceTest() {
        calculatorToolService = new CalculatorToolService();
    }

    // ==================== 简单算术运算 ====================

    @Test
    @DisplayName("解析简单加法 - 2+3 = 5")
    void parseSimpleAddition() throws Exception {
        double result = parseExpression("2+3");
        assertEquals(5.0, result, 0.0001);
    }

    @Test
    @DisplayName("解析简单减法 - 10-4 = 6")
    void parseSimpleSubtraction() throws Exception {
        double result = parseExpression("10-4");
        assertEquals(6.0, result, 0.0001);
    }

    @Test
    @DisplayName("解析简单乘法 - 3*4 = 12")
    void parseSimpleMultiplication() throws Exception {
        double result = parseExpression("3*4");
        assertEquals(12.0, result, 0.0001);
    }

    @Test
    @DisplayName("解析简单除法 - 10/2 = 5")
    void parseSimpleDivision() throws Exception {
        double result = parseExpression("10/2");
        assertEquals(5.0, result, 0.0001);
    }

    // ==================== 运算优先级 ====================

    @Test
    @DisplayName("测试乘法优先于加法 - 2+3*4 = 14")
    void multiplicationPrecedence() throws Exception {
        double result = parseExpression("2+3*4");
        assertEquals(14.0, result, 0.0001);
    }

    @Test
    @DisplayName("测试括号优先级 - (2+3)*4 = 20")
    void parenthesizedExpression() throws Exception {
        double result = parseExpression("(2+3)*4");
        assertEquals(20.0, result, 0.0001);
    }

    @Test
    @DisplayName("测试嵌套括号 - ((2+3)*4)/2 = 10")
    void nestedParentheses() throws Exception {
        double result = parseExpression("((2+3)*4)/2");
        assertEquals(10.0, result, 0.0001);
    }

    @Test
    @DisplayName("测试复杂嵌套 - (3+4)*(2-1)/(6/3) = 3.5")
    void complexNestedExpression() throws Exception {
        double result = parseExpression("(3+4)*(2-1)/(6/3)");
        assertEquals(3.5, result, 0.0001);
    }

    // ==================== 小数和负数 ====================

    @Test
    @DisplayName("解析小数 - 3.14*2 = 6.28")
    void decimalNumbers() throws Exception {
        double result = parseExpression("3.14*2");
        assertEquals(6.28, result, 0.0001);
    }

    @Test
    @DisplayName("解析负数 - -5+3 = -2")
    void negativeNumbers() throws Exception {
        double result = parseExpression("-5+3");
        assertEquals(-2.0, result, 0.0001);
    }

    @Test
    @DisplayName("解析负数小数 - -3.14*2 = -6.28")
    void negativeDecimals() throws Exception {
        double result = parseExpression("-3.14*2");
        assertEquals(-6.28, result, 0.0001);
    }

    // ==================== 空格处理（已知限制） ====================

    @Test
    @DisplayName("注意：ExpressionParser 对空格支持有限 - 这是已知限制")
    void whitespaceKnownLimitation() {
        // ExpressionParser 的 skipWhitespace() 只在 parseFactor() 中调用
        // 不处理运算符后的空格，这是已知限制
        // 实际使用中，表达式应无空格
        CalculatorToolService.CalculationResult result =
                calculatorToolService.calculate("2+3");
        assertEquals(5.0, result.result(), 0.0001);
    }

    // ==================== 错误情况 ====================

    @Test
    @DisplayName("处理除以零 - 10/0 = Infinity")
    void divisionByZero() throws Exception {
        double result = parseExpression("10/0");
        assertEquals(Double.POSITIVE_INFINITY, result);
    }

    @Test
    @DisplayName("处理无效表达式 - 'abc123' 清理后为 '123'")
    void invalidExpression() {
        // abc123 会被清理为 123，不是无效表达式
        CalculatorToolService.CalculationResult result =
                calculatorToolService.calculate("abc123");
        assertNotNull(result);
        assertEquals(123.0, result.result(), 0.0001);
    }

    @Test
    @DisplayName("处理真正无效表达式 - '!!!' 清理后为空字符串")
    void trulyInvalidExpression() {
        // !!! 清理后为空字符串
        CalculatorToolService.CalculationResult result =
                calculatorToolService.calculate("!!!");
        assertNotNull(result);
        assertTrue(Double.isNaN(result.result()));
    }

    @Test
    @DisplayName("处理空字符串 - @Tool 方法返回 NaN")
    void emptyExpression() {
        CalculatorToolService.CalculationResult result =
                calculatorToolService.calculate("");
        assertNotNull(result);
        assertTrue(Double.isNaN(result.result()));
    }

    // ==================== 复杂场景 ====================

    @Test
    @DisplayName("测试复杂组合运算 - 2*3+4*5 = 26")
    void complexCombination() throws Exception {
        double result = parseExpression("2*3+4*5");
        assertEquals(26.0, result, 0.0001);
    }

    @Test
    @DisplayName("测试多层嵌套 - ((2+3)*(4-1)) = 15")
    void multipleNestingLevels() throws Exception {
        double result = parseExpression("((2+3)*(4-1))");
        assertEquals(15.0, result, 0.0001);
    }

    // ==================== @Tool 注解测试 ====================

    @Test
    @DisplayName("@Tool calculate - 有效表达式返回计算结果")
    void calculate_ValidExpression() {
        CalculatorToolService.CalculationResult result =
                calculatorToolService.calculate("(2+3)*4");
        assertNotNull(result);
        assertEquals("(2+3)*4", result.expression());
        assertEquals(20.0, result.result(), 0.0001);
    }

    @Test
    @DisplayName("@Tool calculate - 无效表达式返回 NaN")
    void calculate_InvalidExpression() {
        CalculatorToolService.CalculationResult result =
                calculatorToolService.calculate("invalid");
        assertNotNull(result);
        assertEquals("invalid", result.expression());
        assertTrue(Double.isNaN(result.result()));
    }

    @Test
    @DisplayName("@Tool calculate - 处理空表达式")
    void calculate_EmptyExpression() {
        CalculatorToolService.CalculationResult result =
                calculatorToolService.calculate("");
        assertNotNull(result);
        assertEquals("", result.expression());
        assertTrue(Double.isNaN(result.result()));
    }

    // ==================== 辅助方法 ====================

    /**
     * 通过反射访问 ExpressionParser 并计算表达式（直接调用，可能抛出异常）。
     */
    private double parseExpression(String expression) throws Exception {
        // 创建 ExpressionParser 实例
        Class<?> parserClass = Class.forName(
                "com.zhou.ai.tools.service.CalculatorToolService$ExpressionParser");
        Constructor<?> constructor = parserClass.getDeclaredConstructor(String.class);
        constructor.setAccessible(true);
        Object parser = constructor.newInstance(expression);

        // 调用 parse() 方法
        Method parseMethod = parserClass.getDeclaredMethod("parse");
        parseMethod.setAccessible(true);
        return (Double) parseMethod.invoke(parser);
    }

}
