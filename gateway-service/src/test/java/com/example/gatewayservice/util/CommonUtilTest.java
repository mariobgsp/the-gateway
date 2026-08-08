package com.example.gatewayservice.util;

import com.example.gatewayservice.exception.definition.InvalidRequestException;
import com.example.gatewayservice.exception.models.CommonException;
import com.example.gatewayservice.models.rqrs.Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CommonUtilTest {

    @Test
    void processRequestParsesPathAndQuery() {
        Map<String, Object> request = CommonUtil.processRequest("gateway-catapi?limit=5&size=medium", null, null);

        Map<String, Object> path = (Map<String, Object>) request.get("path");
        assertEquals("gateway-catapi", path.get("pathName"));

        Map<String, Object> params = (Map<String, Object>) path.get("requestParam");
        assertEquals("5", params.get("limit"));
        assertEquals("medium", params.get("size"));
    }

    @Test
    void processRequestWithoutQuery() {
        Map<String, Object> request = CommonUtil.processRequest("gateway-catapi", null, "body");

        Map<String, Object> path = (Map<String, Object>) request.get("path");
        assertEquals("gateway-catapi", path.get("pathName"));
        assertEquals("body", request.get("requestBody"));
    }

    @Test
    void parseQueryStringHandlesEmptyAndValues() {
        assertTrue(CommonUtil.parseQueryString("").isEmpty());

        Map<String, Object> params = CommonUtil.parseQueryString("a=1&b=2&c=");
        assertEquals(3, params.size());
        assertEquals("1", params.get("a"));
        assertEquals("2", params.get("b"));
        assertEquals("", params.get("c"));
    }

    @Test
    void applyErrorMapsCommonException() {
        Response<Object> rs = new Response<>();
        CommonException ex = new InvalidRequestException("bad input");

        CommonUtil.applyError(rs, ex);

        assertEquals("04", rs.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, rs.getHttpStatus());
    }

    @Test
    void applyErrorMapsGenericException() {
        Response<Object> rs = new Response<>();
        CommonUtil.applyError(rs, new IllegalStateException("boom"));

        assertEquals("99", rs.getCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, rs.getHttpStatus());
    }
}
