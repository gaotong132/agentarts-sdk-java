package com.huaweicloud.agentarts.sdk.tools;

import com.huaweicloud.agentarts.sdk.tools.model.CreateCodeInterpreterRequest;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Tools module: CodeInterpreterClient API parity, CodeSession.
 */
class ToolsModuleTest {

    // ========================
    // API verification: CodeInterpreter API methods
    // ========================

    @Nested
    @DisplayName("API verification: CodeInterpreterClient API methods")
    class PythonParityTests {

        @Test
        void controlPlaneMethodsExist() throws Exception {
            // CodeInterpreter control plane methods
            Class<?> cls = CodeInterpreterClient.class;
            assertNotNull(cls.getMethod("createCodeInterpreter", CreateCodeInterpreterRequest.class));
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
            // Data plane: invoke, execute_code, execute_command, upload_file, upload_files,
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
    // CodeSession wrapper verification
    // ========================

    @Nested
    @DisplayName("CodeSession wrapper verification")
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

    // ========================
    // CodeInterpreterClient input validation (Python alignment)
    // ========================

    @Nested
    @DisplayName("CodeInterpreterClient input validation")
    class ValidationTests {

        private CodeInterpreterClient client;

        @BeforeEach
        void setUp() {
            client = new CodeInterpreterClient("cn-southwest-2", "http://localhost:9999", "API_KEY", false);
        }

        @AfterEach
        void tearDown() {
            client.close();
        }

        @Test
        void createRejectsInvalidName() {
            // Python: regex [a-z][a-z0-9-]{0,38}[a-z0-9]$
            assertThrows(IllegalArgumentException.class,
                    () -> client.createCodeInterpreter(new CreateCodeInterpreterRequest()
                            .withName("INVALID").withAuthType("IAM")));
            assertThrows(IllegalArgumentException.class,
                    () -> client.createCodeInterpreter(new CreateCodeInterpreterRequest()
                            .withName("a").withAuthType("IAM")));
            assertThrows(IllegalArgumentException.class,
                    () -> client.createCodeInterpreter(new CreateCodeInterpreterRequest()
                            .withName("has spaces").withAuthType("IAM")));
        }

        @Test
        void createRejectsApiKeyWithoutKeyName() {
            // Python: API_KEY auth_type requires api_key_name
            assertThrows(IllegalArgumentException.class,
                    () -> client.createCodeInterpreter(new CreateCodeInterpreterRequest()
                            .withName("valid-name").withAuthType("API_KEY")));
        }

        @Test
        void executeCodeRejectsInvalidLanguage() {
            // Python: valid_languages = ["python"]
            assertThrows(IllegalArgumentException.class,
                    () -> client.executeCode("print(1)", "javascript", false));
        }

        @Test
        void executeCommandRejectsInvalidFormat() {
            // Python: regex ^[a-zA-Z0-9_\-\.=\s\/\.:]+$
            assertThrows(IllegalArgumentException.class,
                    () -> client.executeCommand("rm -rf /; echo pwned"));
            assertThrows(IllegalArgumentException.class,
                    () -> client.executeCommand("cmd & background"));
            assertThrows(IllegalArgumentException.class,
                    () -> client.executeCommand("echo $HOME"));
        }

        @Test
        void uploadFileRejectsInvalidPath() {
            // Python: absolute path must start with /home/user
            assertThrows(IllegalArgumentException.class,
                    () -> client.uploadFile("/etc/passwd", "content", "desc"));
        }

        @Test
        void downloadFileRejectsInvalidPath() {
            // Python: path must start with /home/user
            assertThrows(IllegalArgumentException.class,
                    () -> client.downloadFile("/etc/passwd"));
        }

        @Test
        void installPackagesRejectsEmptyList() {
            assertThrows(IllegalArgumentException.class,
                    () -> client.installPackages(List.of()));
        }

        @Test
        void installPackagesRejectsInjectionChars() {
            // Python: rejects ;, &, |, `, $
            assertThrows(IllegalArgumentException.class,
                    () -> client.installPackages(List.of("numpy;rm -rf /")));
            assertThrows(IllegalArgumentException.class,
                    () -> client.installPackages(List.of("pkg&bg")));
            assertThrows(IllegalArgumentException.class,
                    () -> client.installPackages(List.of("$evil")));
        }

        @Test
        void invokeRejectsWithoutSession() {
            // Python: "No Code Interpreter exists, use create_code_interpreter method first"
            assertThrows(IllegalStateException.class,
                    () -> client.invoke("execute_code", Map.of("code", "print(1)")));
        }
    }
}
