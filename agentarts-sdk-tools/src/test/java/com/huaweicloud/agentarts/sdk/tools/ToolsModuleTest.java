package com.huaweicloud.agentarts.sdk.tools;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Tools module: CodeInterpreterClient API parity, CodeSession.
 */
class ToolsModuleTest {

    // ========================
    // Python Parity: CodeInterpreter API methods
    // ========================

    @Nested
    @DisplayName("Python Parity: CodeInterpreterClient API methods")
    class PythonParityTests {

        @Test
        void controlPlaneMethodsExist() throws Exception {
            // Mirrors Python CodeInterpreter control plane methods
            Class<?> cls = CodeInterpreterClient.class;
            assertNotNull(cls.getMethod("createCodeInterpreter", String.class, String.class));
            assertNotNull(cls.getMethod("createCodeInterpreter", String.class));
            assertNotNull(cls.getMethod("listCodeInterpreters", String.class, int.class, int.class));
            assertNotNull(cls.getMethod("listCodeInterpreters"));
            assertNotNull(cls.getMethod("updateCodeInterpreter", String.class, Map.class));
            assertNotNull(cls.getMethod("getCodeInterpreter", String.class));
            assertNotNull(cls.getMethod("deleteCodeInterpreter", String.class));
        }

        @Test
        void dataPlaneSessionMethodsExist() throws Exception {
            Class<?> cls = CodeInterpreterClient.class;
            assertNotNull(cls.getMethod("startSession", String.class, String.class, Integer.class));
            assertNotNull(cls.getMethod("startSession", String.class, String.class));
            assertNotNull(cls.getMethod("getSession", String.class, String.class));
            assertNotNull(cls.getMethod("stopSession"));
        }

        @Test
        void dataPlaneInvokeMethodsExist() throws Exception {
            // Mirrors Python: invoke, execute_code, execute_command, upload_file, upload_files,
            // download_file, download_files, install_packages, clear_context
            Class<?> cls = CodeInterpreterClient.class;
            assertNotNull(cls.getMethod("invoke", String.class, Map.class));
            assertNotNull(cls.getMethod("executeCode", String.class, String.class, boolean.class));
            assertNotNull(cls.getMethod("executeCode", String.class));
            assertNotNull(cls.getMethod("executeCommand", String.class));
            assertNotNull(cls.getMethod("uploadFile", String.class, String.class, String.class));
            assertNotNull(cls.getMethod("uploadFiles", List.class));
            assertNotNull(cls.getMethod("downloadFile", String.class));
            assertNotNull(cls.getMethod("downloadFiles", List.class));
            assertNotNull(cls.getMethod("installPackages", List.class, boolean.class));
            assertNotNull(cls.getMethod("installPackages", List.class));
            assertNotNull(cls.getMethod("clearContext"));
        }

        @Test
        void constructorMatchesPython() throws Exception {
            // Python: __init__(region, data_endpoint=None, auth_type="API_KEY", verify_ssl=True)
            Class<?> cls = CodeInterpreterClient.class;
            assertNotNull(cls.getConstructor(String.class, String.class, String.class, boolean.class));
            assertNotNull(cls.getConstructor(String.class, String.class));
            assertNotNull(cls.getConstructor(String.class));
        }

        @Test
        void propertiesMatchPython() throws Exception {
            // Python: code_interpreter_name (property), session_id (property)
            Class<?> cls = CodeInterpreterClient.class;
            assertNotNull(cls.getMethod("getCodeInterpreterName"));
            assertNotNull(cls.getMethod("setCodeInterpreterName", String.class));
            assertNotNull(cls.getMethod("getSessionId"));
            assertNotNull(cls.getMethod("setSessionId", String.class));
        }

        @Test
        void implementsAutoCloseable() {
            assertTrue(AutoCloseable.class.isAssignableFrom(CodeInterpreterClient.class));
        }
    }

    // ========================
    // CodeSession (Python parity)
    // ========================

    @Nested
    @DisplayName("CodeSession Python parity")
    class CodeSessionTests {

        @Test
        void factoryMethodExists() throws Exception {
            // Python: code_session(region, code_interpreter_name, auth_type="API_KEY", ...)
            assertNotNull(CodeSession.class.getMethod("start", String.class, String.class, String.class, String.class, boolean.class));
            assertNotNull(CodeSession.class.getMethod("start", String.class, String.class, String.class));
        }

        @Test
        void implementsAutoCloseable() {
            // Python: @contextmanager → Java: AutoCloseable
            assertTrue(AutoCloseable.class.isAssignableFrom(CodeSession.class));
        }

        @Test
        void getClientMethodExists() throws Exception {
            assertNotNull(CodeSession.class.getMethod("getClient"));
        }

        @Test
        void getSessionIdMethodExists() throws Exception {
            assertNotNull(CodeSession.class.getMethod("getSessionId"));
        }
    }
}
