package com.alexfh.chat_eval;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatEval implements ClientModInitializer
{
    public static final Logger LOGGER = LoggerFactory.getLogger("chat-eval");

    @Override
    public void onInitializeClient()
    {
        ChatEval.LOGGER.info("Hello Fabric world!");
    }
}