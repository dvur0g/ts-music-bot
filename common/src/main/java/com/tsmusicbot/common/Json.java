package com.tsmusicbot.common;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The single, shared JSON configuration for the control-API wire format exchanged between {@code
 * backend} and {@code voice-agent}. Both processes must use this mapper (or an equivalently
 * configured one) so the two sides agree on how records and polymorphic types are encoded.
 */
public final class Json {

  private static final ObjectMapper MAPPER =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  private Json() {}

  /** The shared, correctly configured {@link ObjectMapper} for the control-API wire format. */
  public static ObjectMapper mapper() {
    return MAPPER;
  }
}
