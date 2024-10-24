package com.alexfh.chat_eval.evaluation;

import com.alexfh.chat_eval.ChatEval;
import com.alexfh.chat_eval.config.ChatEvalConfig;
import com.alexfh.chat_eval.evaluation.message.MessageSegment;
import com.alexfh.chat_eval.evaluation.message.MessageSegmentType;
import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.Expression;
import com.ezylang.evalex.config.ExpressionConfiguration;
import com.ezylang.evalex.data.EvaluationValue;
import com.ezylang.evalex.parser.ParseException;
import com.mojang.brigadier.context.StringRange;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Evaluator
{
    private static Map<String, EvaluationValue> defaultConstants;

    static
    {
        BigDecimal k = BigDecimal.valueOf(1_000);
        BigDecimal m = k.multiply(k);
        BigDecimal b = m.multiply(k);
        BigDecimal t = b.multiply(k);
        Evaluator.defaultConstants = new HashMap<>(ExpressionConfiguration.StandardConstants);
        Evaluator.defaultConstants.put("k", EvaluationValue.numberValue(k));
        Evaluator.defaultConstants.put("m", EvaluationValue.numberValue(m));
        Evaluator.defaultConstants.put("b", EvaluationValue.numberValue(b));
        Evaluator.defaultConstants.put("t", EvaluationValue.numberValue(t));
    }

    public static String evaluateExpression(String expressionString) throws EvaluationException, ParseException
    {
        ExpressionConfiguration expressionConfiguration = ExpressionConfiguration.builder()
            .decimalPlacesResult(ChatEval.getConfig().defaultDecimalPlaces).defaultConstants(Evaluator.defaultConstants)
            .arraysAllowed(false).structuresAllowed(false).build();
        Expression expression = new Expression(expressionString, expressionConfiguration);
        EvaluationValue evaluationValue = expression.evaluate();
        return evaluationValue.isNullValue() ? "null" : evaluationValue.getStringValue();
    }

    public static String evaluateMessage(String message) throws EvaluationException, ParseException
    {
        List<MessageSegment> parseResults = Evaluator.parseMessage(message);
        StringBuilder evaluatedMessageBuilder = new StringBuilder();
        for (MessageSegment messageSegment : parseResults)
        {
            String text = messageSegment.range().get(message);
            switch (messageSegment.type())
            {
                case LITERAL -> evaluatedMessageBuilder.append(text);
                case EXPRESSION ->
                {
                    String evaluatedString = Evaluator.evaluateExpression(text);
                    evaluatedMessageBuilder.append(evaluatedString);
                }
            }
        }
        return evaluatedMessageBuilder.toString();
    }

    public static List<MessageSegment> parseMessage(String message)
    {
        int numConsecutiveBackslashes = 0;
        int expressionStart = 0;
        int literalStart = 0;
        boolean inExpression = false;
        List<MessageSegment> segments = new ArrayList<>();
        ChatEvalConfig chatEvalConfig = ChatEval.getConfig();
        String chars = chatEvalConfig.chars;
        char expressionChar = !chars.isEmpty() ? chars.charAt(0) : '`';
        char escapeChar = chars.length() >= 2 ? chars.charAt(1) : '\\';
        for (int i = 0; i < message.length(); i++)
        {
            char c = message.charAt(i);
            if (inExpression)
            {
                if (c == expressionChar)
                {
                    inExpression = false;
                    segments.add(new MessageSegment(MessageSegmentType.EXPRESSION, new StringRange(expressionStart, i)));
                    literalStart = i + 1;
                }
                continue;
            }
            // c not in expression

            // c is escape character
            if (c == escapeChar)
            {
                numConsecutiveBackslashes++;
                continue;
            }

            // c is not expression character and not escape character
            if (c != expressionChar)
            {
                numConsecutiveBackslashes = 0;
                continue;
            }

            // c is expression character
            int numBackslashesToAdd = numConsecutiveBackslashes / 2;
            int numBackslashesToRemove = numConsecutiveBackslashes - numBackslashesToAdd;
            int literalEnd = i - numBackslashesToRemove;
            if (literalStart != literalEnd)
            {
                segments.add(new MessageSegment(MessageSegmentType.LITERAL, new StringRange(literalStart, literalEnd)));
            }
            if (numConsecutiveBackslashes % 2 == 1)
            {
                literalStart = i;
            }
            else
            {
                inExpression = true;
                expressionStart = i + 1;
            }
            numConsecutiveBackslashes = 0;
        }
        if (inExpression)
        {
            // treat unterminated expression as literal
            segments.add(new MessageSegment(MessageSegmentType.LITERAL, new StringRange(
                expressionStart - 1, message.length())));
        }
        else if (literalStart < message.length())
        {
            segments.add(new MessageSegment(MessageSegmentType.LITERAL, new StringRange(literalStart, message.length())));
        }
        return segments;
    }
}