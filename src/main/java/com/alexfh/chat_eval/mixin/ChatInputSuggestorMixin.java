package com.alexfh.chat_eval.mixin;

import com.alexfh.chat_eval.evaluation.Evaluator;
import com.alexfh.chat_eval.evaluation.message.MessageSegment;
import com.alexfh.chat_eval.evaluation.message.MessageSegmentType;
import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.parser.ParseException;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Mixin(ChatInputSuggestor.class)
public class ChatInputSuggestorMixin
{
    @Final
    @Shadow
    TextFieldWidget textField;

    @Shadow
    private CompletableFuture<Suggestions> pendingSuggestions;

    @Final
    @Shadow
    private List<OrderedText> messages;

    @Shadow
    private int width;

    @Final
    @Shadow
    TextRenderer textRenderer;

    @Shadow
    public void show(boolean narrateFirstSuggestion)
    {
    }

    @Inject(at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screen/ChatInputSuggestor;suggestingWhenEmpty:Z"), method = "refresh()V", cancellable = true)
    private void refreshCommand(CallbackInfo info)
    {
        this.refresh(info);
    }

    @Inject(at = @At(value = "INVOKE", target = "Ljava/lang/String;substring(II)Ljava/lang/String;"), method = "refresh()V", cancellable = true)
    private void refreshNormal(CallbackInfo info)
    {
        this.refresh(info);
    }

    private void refresh(CallbackInfo info)
    {
        String message = this.textField.getText();
        List<MessageSegment> parseResult = Evaluator.parseMessage(message);
        int cursor = this.textField.getCursor();
        Optional<MessageSegment> cursorExpressionOptional = this.getExpressionAtCursor(cursor, parseResult);
        if (cursorExpressionOptional.isEmpty())
        {
            this.pendingSuggestions = Suggestions.empty();
            return;
        }
        MessageSegment cursorExpression = cursorExpressionOptional.get();
        StringRange expressionRange = cursorExpression.range();
        String expressionString = expressionRange.get(message);
        try
        {
            String evaluatedString = Evaluator.evaluateExpression(expressionString);
            List<Suggestion> suggestions = new ArrayList<>();
            suggestions.add(new Suggestion(StringRange.between(
                expressionRange.getStart() - 1, expressionRange.getEnd() + 1), evaluatedString, () -> "tooltip"));
            this.pendingSuggestions = CompletableFuture.completedFuture(Suggestions.create(message, suggestions));
            this.show(false);
        }
        catch (EvaluationException | ParseException e)
        {
            this.pendingSuggestions = Suggestions.empty();
            String errorMessage = e.getMessage();
            this.messages.add(OrderedText.styledForwardsVisitedString(errorMessage, Style.EMPTY));
            this.width = this.textRenderer.getWidth(errorMessage);
        }
        info.cancel();
    }

    private Optional<MessageSegment> getExpressionAtCursor(int cursor, List<MessageSegment> parseResult)
    {
        /*
        0 1 2 3 4 5 <- cursor
        |`|f|o|o|`|
         0 1 2 3 4  <- index
         */
        Iterator<MessageSegment> iterator = parseResult.iterator();
        MessageSegment messageSegment;
        while (iterator.hasNext() && (messageSegment = iterator.next()).range().getStart() <= cursor)
        {
            MessageSegmentType messageSegmentType = messageSegment.type();
            if (messageSegmentType.equals(MessageSegmentType.EXPRESSION) &&
                cursor <= messageSegment.range().getEnd() + 1)
            {
                return Optional.of(messageSegment);
            }
        }
        return Optional.empty();
    }
}