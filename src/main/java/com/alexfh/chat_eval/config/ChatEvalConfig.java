package com.alexfh.chat_eval.config;

import com.alexfh.chat_eval.ChatEval;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = ChatEval.modID)
public class ChatEvalConfig implements ConfigData
{
    @ConfigEntry.Category("category.chat-eval")
    public boolean shouldCancelMessages = true;

    @ConfigEntry.Category("category.chat-eval")
    public int defaultDecimalPlaces = 10;

    @ConfigEntry.Category("category.chat-eval")
    public String chars = "`\\";
}