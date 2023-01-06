package com.networknt.apikey;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class ApiKeyConfigTest {
    static final Logger logger = LoggerFactory.getLogger(ApiKeyConfigTest.class);
    static final ApiKeyConfig config = ApiKeyConfig.load();

    @Test
    public void testDecryption() {
        logger.debug("apiKey for /test2 = " + config.getPathPrefixAuths().get(2).getApiKey());
        Assert.assertEquals("password", config.getPathPrefixAuths().get(2).getApiKey());
    }

    @Test
    public void testPathPrefixAuths() {
        List<ApiKey> auths = config.getPathPrefixAuths();
        Assert.assertEquals(3, auths.size());
        ApiKey apiKey1 = auths.get(0);
        ApiKey apiKey2 = auths.get(1);
        ApiKey apiKey3 = auths.get(2);
        Assert.assertEquals("/test1", apiKey1.getPathPrefix());
        Assert.assertEquals("x-gateway-apikey", apiKey1.getHeaderName());
        Assert.assertEquals("x-apikey", apiKey3.getHeaderName());
        Assert.assertEquals("abcdefg", apiKey1.getApiKey());
    }

}
