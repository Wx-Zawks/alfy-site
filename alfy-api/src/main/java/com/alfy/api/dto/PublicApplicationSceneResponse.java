package com.alfy.api.dto;

/** Fields directly consumable by alfy-web application scene cards. */
public record PublicApplicationSceneResponse(
        Long id,
        String key,
        String name,
        String slogan,
        String summary,
        String coverImageUrl
) {
}
