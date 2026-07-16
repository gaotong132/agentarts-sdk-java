package com.huaweicloud.agentarts.sdk.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UrlUtilsTest {

    @Test
    void encodesPathSeparatorsTraversalAndQueryCharacters() {
        assertEquals("..%2Fagent%3Fx%3D1%20%E4%BD%A0%E5%A5%BD",
                UrlUtils.encodePathSegment("../agent?x=1 你好", "agentId"));
    }

    @Test
    void rejectsMissingSegments() {
        assertThrows(IllegalArgumentException.class,
                () -> UrlUtils.encodePathSegment(null, "id"));
        assertThrows(IllegalArgumentException.class,
                () -> UrlUtils.encodePathSegment("  ", "id"));
    }
}
