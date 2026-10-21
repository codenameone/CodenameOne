package com.codenameone.examples.skirmish.api;

import com.codename1.annotations.Mapped;
import java.util.List;

/// A deck: exactly eight distinct card ids, validated by the shared rules.
@Mapped
public record DeckDto(List<String> cards) {
}
