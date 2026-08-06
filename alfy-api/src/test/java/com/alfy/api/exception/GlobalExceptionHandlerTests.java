package com.alfy.api.exception;

import com.alfy.api.common.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.context.annotation.Import;
import org.springframework.util.unit.DataSize;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest
@AutoConfigureMockMvc
@Import(GlobalExceptionHandlerTests.ExceptionTestController.class)
class GlobalExceptionHandlerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MultipartProperties multipartProperties;

    @Test
    void multipartLimitsMatchMediaUploadLimit() {
        org.junit.jupiter.api.Assertions.assertEquals(
                DataSize.ofMegabytes(30),
                multipartProperties.getMaxFileSize()
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                DataSize.ofMegabytes(35),
                multipartProperties.getMaxRequestSize()
        );
    }

    @Test
    void businessExceptionUsesUnifiedJsonAndNotFoundStatus() throws Exception {
        mockMvc.perform(get("/test/business-exception"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value("测试数据不存在"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void oversizedUploadUsesUnifiedJsonAndPayloadTooLargeStatus() throws Exception {
        mockMvc.perform(get("/test/max-upload-size"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.PAYLOAD_TOO_LARGE.getCode()))
                .andExpect(jsonPath("$.message").value("单个文件不能超过 30MB"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @RestController
    static class ExceptionTestController {

        @GetMapping("/test/business-exception")
        void throwBusinessException() {
            throw new BusinessException(ErrorCode.NOT_FOUND, "测试数据不存在");
        }

        @GetMapping("/test/max-upload-size")
        void throwMaxUploadSizeExceededException() {
            throw new MaxUploadSizeExceededException(30L * 1024 * 1024);
        }
    }
}
