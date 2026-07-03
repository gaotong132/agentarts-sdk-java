package com.huaweicloud.agentarts.sdk.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for {@link JsonUtils}.
 */
class JsonUtilsTest {

    // ============================================================
    // MAPPER singleton
    // ============================================================

    @Nested
    class Mapper {

        @Test
        void mapperIsNotNull() {
            assertNotNull(JsonUtils.MAPPER);
        }

        @Test
        void mapperIsSameInstance() {
            ObjectMapper a = JsonUtils.MAPPER;
            ObjectMapper b = JsonUtils.MAPPER;
            assertSame(a, b);
        }

        @Test
        void mapperIgnoresUnknownProperties() throws Exception {
            String json = "{\"unknown_field\": 42, \"name\": \"test\"}";
            TestPojo pojo = JsonUtils.MAPPER.readValue(json, TestPojo.class);
            assertEquals("test", pojo.name);
        }

        static class TestPojo {
            public String name;
        }
    }

    // ============================================================
    // extractErrorMessage
    // ============================================================

    @Nested
    class ExtractErrorMessage {

        @Test
        void returnsNullForNull() {
            assertNull(JsonUtils.extractErrorMessage(null));
        }

        @Test
        void returnsNullForEmpty() {
            assertNull(JsonUtils.extractErrorMessage(""));
        }

        @Test
        void returnsNullForInvalidJson() {
            assertNull(JsonUtils.extractErrorMessage("not json"));
        }

        @Test
        void returnsNullForEmptyJsonObject() {
            assertNull(JsonUtils.extractErrorMessage("{}"));
        }

        @Test
        void extractsErrorField() {
            String json = "{\"error\": \"Unauthorized\"}";
            assertEquals("Unauthorized", JsonUtils.extractErrorMessage(json));
        }

        @Test
        void extractsMessageField() {
            String json = "{\"message\": \"Not found\"}";
            assertEquals("Not found", JsonUtils.extractErrorMessage(json));
        }

        @Test
        void extractsErrorMsgField() {
            String json = "{\"error_msg\": \"Forbidden\"}";
            assertEquals("Forbidden", JsonUtils.extractErrorMessage(json));
        }

        @Test
        void errorFieldTakesPrecedenceOverMessage() {
            // When both "error" and "message" exist, should combine them
            String json = "{\"error\": \"ERR001\", \"message\": \"Something failed\"}";
            String result = JsonUtils.extractErrorMessage(json);
            assertEquals("ERR001: Something failed", result);
        }

        @Test
        void errorFieldAloneWhenMessageIsMissing() {
            String json = "{\"error\": \"ERR001\", \"other\": \"value\"}";
            assertEquals("ERR001", JsonUtils.extractErrorMessage(json));
        }

        @Test
        void messageFieldTakesPrecedenceOverErrorMsg() {
            // "message" is checked before "error_msg"
            String json = "{\"message\": \"msg text\", \"error_msg\": \"error_msg text\"}";
            assertEquals("msg text", JsonUtils.extractErrorMessage(json));
        }

        @Test
        void ignoresNonTextualErrorField() {
            // If "error" is an object, not a string, skip to "message"
            String json = "{\"error\": {\"code\": 500}, \"message\": \"Server error\"}";
            assertEquals("Server error", JsonUtils.extractErrorMessage(json));
        }

        @Test
        void ignoresNonTextualMessageField() {
            String json = "{\"message\": 42, \"error_msg\": \"Fallback\"}";
            assertEquals("Fallback", JsonUtils.extractErrorMessage(json));
        }

        @Test
        void ignoresNonTextualErrorMsgField() {
            String json = "{\"error_msg\": [1, 2, 3]}";
            assertNull(JsonUtils.extractErrorMessage(json));
        }

        @Test
        void handlesJsonArray() {
            // JSON arrays are valid but have no named fields
            assertNull(JsonUtils.extractErrorMessage("[1, 2, 3]"));
        }

        @Test
        void handlesPlainStringValue() {
            // A JSON string literal is valid but not an object
            assertNull(JsonUtils.extractErrorMessage("\"hello\""));
        }
    }

    // ============================================================
    // toJson
    // ============================================================

    @Nested
    class ToJson {

        @Test
        void serializesSimpleMap() {
            String json = JsonUtils.toJson(Map.of("key", "value"));
            assertTrue(json.contains("\"key\""));
            assertTrue(json.contains("\"value\""));
        }

        @Test
        void serializesNull() throws Exception {
            String json = JsonUtils.toJson(null);
            assertEquals("null", json);
        }

        @Test
        void serializesEmptyObject() {
            String json = JsonUtils.toJson(Map.of());
            assertEquals("{}", json);
        }

        @Test
        void serializesPojo() {
            Mapper.TestPojo pojo = new Mapper.TestPojo();
            pojo.name = "test";
            String json = JsonUtils.toJson(pojo);
            assertTrue(json.contains("\"name\":\"test\""));
        }

        @Test
        void serializesNumbers() {
            String json = JsonUtils.toJson(Map.of("count", 42, "rate", 3.14));
            assertTrue(json.contains("42"));
            assertTrue(json.contains("3.14"));
        }

        @Test
        void serializesBooleans() {
            String json = JsonUtils.toJson(Map.of("flag", true));
            assertTrue(json.contains("true"));
        }

        @Test
        void serializesNestedObjects() {
            Map<String, Object> nested = Map.of("inner", Map.of("value", "deep"));
            String json = JsonUtils.toJson(nested);
            assertTrue(json.contains("inner"));
            assertTrue(json.contains("deep"));
        }
    }

    // ============================================================
    // isBlank / isNotBlank
    // ============================================================

    @Nested
    class BlankChecks {

        @Test
        void nullIsBlank() {
            assertTrue(JsonUtils.isBlank(null));
            assertFalse(JsonUtils.isNotBlank(null));
        }

        @Test
        void emptyIsBlank() {
            assertTrue(JsonUtils.isBlank(""));
            assertFalse(JsonUtils.isNotBlank(""));
        }

        @Test
        void nonEmptyIsNotBlank() {
            assertFalse(JsonUtils.isBlank("hello"));
            assertTrue(JsonUtils.isNotBlank("hello"));
        }

        @Test
        void whitespaceIsNotBlank() {
            // Whitespace-only strings are NOT blank (consistent with Python truthiness)
            assertFalse(JsonUtils.isBlank(" "));
            assertTrue(JsonUtils.isNotBlank(" "));
        }

        @Test
        void singleCharIsNotBlank() {
            assertFalse(JsonUtils.isBlank("a"));
            assertTrue(JsonUtils.isNotBlank("a"));
        }

        @Test
        void blankAndIsNotBlankAreConsistent() {
            String[] cases = {null, "", " ", "abc", "null", "\t", "\n"};
            for (String s : cases) {
                assertEquals(!JsonUtils.isBlank(s), JsonUtils.isNotBlank(s),
                        "isBlank and isNotBlank should be inverses for: " + s);
            }
        }
    }
}
