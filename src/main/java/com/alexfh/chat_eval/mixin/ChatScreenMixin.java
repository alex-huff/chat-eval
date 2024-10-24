package com.alexfh.chat_eval.mixin;

import com.alexfh.chat_eval.ChatEval;
import com.alexfh.chat_eval.evaluation.Evaluator;
import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.parser.ParseException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin
{
    @ModifyVariable(at = @At(value = "INVOKE", target = "Ljava/lang/String;startsWith(Ljava/lang/String;)Z"), method = "sendMessage(Ljava/lang/String;Z)Z", argsOnly = true)
    public String evaluateChatText(String chatText)
    {
        try
        {
            return Evaluator.evaluateMessage(chatText);
        }
        catch (EvaluationException | ParseException e)
        {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            MutableText errorText = MutableText.of(PlainTextContent.of("Error evaluating message: " + e.getMessage()))
                .styled(style -> style.withColor(Formatting.RED));
            minecraftClient.inGameHud.getChatHud().addMessage(errorText);
            return ChatEval.getConfig().shouldCancelMessages ? null : chatText;
        }
    }

    @Inject(at = @At(value = "INVOKE", target = "Ljava/lang/String;startsWith(Ljava/lang/String;)Z", shift = At.Shift.AFTER), method = "sendMessage(Ljava/lang/String;Z)Z", cancellable = true)
    public void sendMessage(String chatText, boolean addToHistory, CallbackInfoReturnable<Boolean> cir)
    {
        if (chatText == null)
        {
            cir.setReturnValue(true);
        }
    }
}