package org.greenplum.pxf.service.rest;

import com.google.common.base.Charsets;
import org.greenplum.pxf.api.model.RequestContext;
import org.greenplum.pxf.service.RequestParser;
import org.greenplum.pxf.service.controller.ReadService;
import org.greenplum.pxf.service.controller.WriteService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({PxfResource.class, PxfLegacyResource.class})
public class PxfResourceIT {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private RequestParser<MultiValueMap<String, String>> mockParser;

    @MockBean
    private ReadService mockReadService;

    @MockBean
    private WriteService mockWriteService;

    @Mock
    private RequestContext mockContext;

    @BeforeAll
    public static void init() {
        System.setProperty("pxf.logdir", "/tmp");
    }

    @Test
    public void testReadEndpoint() throws Exception {
        StreamingResponseBody mockReadResponse = outputStream -> outputStream.write("Hello from read!".getBytes(Charsets.UTF_8));
        when(mockParser.parseRequest(any(), eq(RequestContext.RequestType.READ_BRIDGE))).thenReturn(mockContext);
        when(mockReadService.getReadResponse(mockContext)).thenReturn(mockReadResponse);

        mvc.perform(get("/pxf/read"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello from read!"));
    }

    @Test
    public void testWriteEndpoint() throws Exception {
        when(mockParser.parseRequest(any(), eq(RequestContext.RequestType.WRITE_BRIDGE))).thenReturn(mockContext);
        when(mockWriteService.getWriteResponse(same(mockContext), any())).thenReturn("Hello from write!");

        mvc.perform(post("/pxf/write"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello from write!"));
    }

    @Test
    public void testLegacyBridgeEndpoint() throws Exception {
        //TODO: legacy endpoint should throw 500 exception with a hint, validate error message
        StreamingResponseBody mockReadResponse = outputStream -> outputStream.write("Hello from read!".getBytes(Charsets.UTF_8));
        when(mockParser.parseRequest(any(), eq(RequestContext.RequestType.READ_BRIDGE))).thenReturn(mockContext);
        when(mockReadService.getReadResponse(mockContext)).thenReturn(mockReadResponse);

        mvc.perform(get("/pxf/v15/Bridge"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello from read!"));
    }

    @Test
    public void testLegacyWritableEndpoint() throws Exception {
        //TODO: legacy endpoint should throw 500 exception with a hint, validate error message
        when(mockParser.parseRequest(any(), eq(RequestContext.RequestType.WRITE_BRIDGE))).thenReturn(mockContext);
        when(mockWriteService.getWriteResponse(same(mockContext), any())).thenReturn("Hello from write!");

        mvc.perform(post("/pxf/v15/Writable/stream"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello from write!"));
    }

}
