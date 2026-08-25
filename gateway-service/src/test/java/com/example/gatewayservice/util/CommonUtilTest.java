package com.example.gatewayservice.util;

import com.example.gatewayservice.exception.definition.InvalidRequestException;
import com.example.gatewayservice.exception.models.CommonException;
import com.example.gatewayservice.models.rqrs.Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.example.gatewayservice.models.rqrs.ForwardRequest;
import org.springframework.http.HttpHeaders;

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

    @Test
    void toForwardRequestTolerantDecode() {
        ForwardRequest fr = CommonUtil.toForwardRequest("gateway-catapi?limit=5&foo", new HttpHeaders(), null);
        assertEquals("gateway-catapi", fr.getPathName());
        assertEquals("5", fr.getQueryParams().get("limit"));
        assertEquals("", fr.getQueryParams().get("foo"));
    }

    @Test
    void toForwardRequestDecodesEncoded() {
        // container (Tomcat) already decoded %20->space before @PathVariable, so input is "a b=c+d" not "a%20b=c%2Bd"
        ForwardRequest fr = CommonUtil.toForwardRequest("gateway-catapi?a b=c+d", null, null);
        assertEquals("c+d", fr.getQueryParams().get("a b"));
    }

    @Test
    void toForwardRequestDuplicateKeyKeepLast() {
        ForwardRequest fr = CommonUtil.toForwardRequest("gateway-catapi?a=1&a=2", null, null);
        assertEquals("2", fr.getQueryParams().get("a"));
    }

    @Test
    void toForwardRequestTrailingPercent() {
        ForwardRequest fr = CommonUtil.toForwardRequest("gateway-catapi?a=foo%", null, null);
        assertEquals("foo%", fr.getQueryParams().get("a"));
    }

    @Test
    void toForwardRequestNoQuery() {
        ForwardRequest fr = CommonUtil.toForwardRequest("gateway-catapi", null, "body");
        assertTrue(fr.getQueryParams().isEmpty());
        assertEquals("body", fr.getBody());
    }
}
