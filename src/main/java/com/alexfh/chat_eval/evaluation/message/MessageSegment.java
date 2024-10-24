package com.alexfh.chat_eval.evaluation.message;

import com.mojang.brigadier.context.StringRange;

public record MessageSegment(MessageSegmentType type, StringRange range)
{
}