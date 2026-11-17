package com.wayfarer.travel.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public interface ChatModel {
    JsonNode reply(ArrayNode messages, boolean toolsAllowed);
}
