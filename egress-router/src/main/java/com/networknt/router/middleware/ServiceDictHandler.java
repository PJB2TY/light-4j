package com.networknt.router.middleware;

import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.httpstring.HttpStringConstants;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import com.networknt.utility.StringUtils;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Find service Ids using a combination of path prefix and request method.
 * 
 * @author Daniel Zhao
 *
 */

@SuppressWarnings("unchecked")
public class ServiceDictHandler implements MiddlewareHandler {
	private static final Logger logger = LoggerFactory.getLogger(ServiceDictHandler.class);
	protected static final String INTERNAL_KEY_FORMAT = "%s %s";
	
    public static final String CONFIG_NAME = "serviceDict";
    public static final String DELIMITOR = "@";
    protected volatile HttpHandler next;
    protected ServiceDictConfig config;

    static final String STATUS_INVALID_REQUEST_PATH = "ERR10007";

    public ServiceDictHandler() {
        logger.info("ServiceDictHandler is constructed");
        config = ServiceDictConfig.load();
    }

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
        String[] serviceEntry = null;
        HeaderValues serviceUrlHeader = exchange.getRequestHeaders().get(HttpStringConstants.SERVICE_URL);
        String serviceUrl = serviceUrlHeader != null ? serviceUrlHeader.peekFirst() : null;
        if (serviceUrl == null) {
            HeaderValues serviceIdHeader = exchange.getRequestHeaders().get(HttpStringConstants.SERVICE_ID);
            String serviceId = serviceIdHeader != null ? serviceIdHeader.peekFirst() : null;
            if(serviceId == null) {
                String requestPath = exchange.getRequestURI();
                String httpMethod = exchange.getRequestMethod().toString().toLowerCase();
                serviceEntry = HandlerUtils.findServiceEntry(toInternalKey(httpMethod, requestPath), config.getMapping());
                if(serviceEntry == null) {
                    setExchangeStatus(exchange, STATUS_INVALID_REQUEST_PATH, requestPath);
                    return;
                } else {
                    exchange.getRequestHeaders().put(HttpStringConstants.SERVICE_ID, serviceEntry[1]);
                }
            }
        }
        Map<String, Object> auditInfo = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
        if(auditInfo == null) {
            // AUDIT_INFO is created for light-gateway to populate the endpoint as the OpenAPI handlers might not be available.
            auditInfo = new HashMap<>();
            auditInfo.put(Constants.ENDPOINT_STRING, serviceEntry[0]);
            exchange.putAttachment(AttachmentConstants.AUDIT_INFO, auditInfo);
        }
        Handler.next(exchange, next);
	}

    private static String toInternalKey(String key) {
    	String[] tokens = StringUtils.trimToEmpty(key).split(DELIMITOR);
    	
    	if (tokens.length ==2) {
    		return toInternalKey(tokens[1], tokens[0]);
    	}
    	
    	logger.warn("Invalid key {}", key);
    	return key;
    }
    
    protected static String toInternalKey(String method, String path) {
    	return String.format(INTERNAL_KEY_FORMAT, method, HandlerUtils.normalisePath(path));
    }

	@Override
    public HttpHandler getNext() {
        return next;
    }

    @Override
    public MiddlewareHandler setNext(final HttpHandler next) {
        Handlers.handlerNotNull(next);
        this.next = next;
        return this;
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(ServiceDictHandler.class.getName(), config.getMappedConfig(), null);
    }

    @Override
    public void reload() {
        config.reload();
    }
}
