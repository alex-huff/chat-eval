package com.alexfh.chat_eval;

import com.alexfh.chat_eval.config.ChatEvalConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatEval implements ClientModInitializer
{
    public static final String modID = "chat-eval";
    public static final Logger LOGGER = LoggerFactory.getLogger("chat-eval");
    private static ConfigHolder<ChatEvalConfig> configHolder;

    public static ChatEvalConfig getConfig()
    {
        return configHolder.getConfig();
    }

    @Override
    public void onInitializeClient()
    {
        AutoConfig.register(ChatEvalConfig.class, GsonConfigSerializer::new);
        configHolder = AutoConfig.getConfigHolder(ChatEvalConfig.class);
    }
}