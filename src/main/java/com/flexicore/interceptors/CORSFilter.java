package com.flexicore.interceptors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flexicore.constants.Constants;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class CORSFilter implements ContainerResponseFilter, ContainerRequestFilter {
    private static final Logger logger = Logger.getLogger("application");
    private static final ObjectMapper objectMapper = new ObjectMapper();
    public static final String INTERNAL_DATA = "internalData";


    @Override
    public void filter(final ContainerRequestContext requestContext,
                       final ContainerResponseContext cres) {
        MultivaluedMap<String, Object> headers = cres.getHeaders();
        List<Object> acao = headers.get("Access-Control-Allow-Origin");
        if (acao == null || acao.isEmpty()) {
            headers.add("Access-Control-Allow-Origin", Constants.HTTP_ACCESS_CONTROL_ALLOW_ORIGIN);

        }
        List<String> list = requestContext.getHeaders().get("Access-Control-Request-Headers");
        if (list != null) {
            List<Object> acah = headers.get("Access-Control-Allow-Headers");
            if (acah == null || acah.isEmpty()) {
                headers.add("Access-Control-Allow-Headers", StringUtils.join(list, ','));

            }

        }
        List<Object> acac = headers.get("Access-Control-Allow-Credentials");
        if (acac == null || acac.isEmpty()) {
            headers.add("Access-Control-Allow-Credentials", "true");

        }
        List<Object> acam = headers.get("Access-Control-Allow-Methods");
        if (acam == null || acam.isEmpty()) {
            headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");

        }
        List<Object> acma = headers.get("Access-Control-Max-Age");
        if (acma == null || acma.isEmpty()) {
            headers.add("Access-Control-Max-Age", "1209600");

        }

        if (Constants.enableHTTPErrorLog && cres.getStatusInfo() != null && !Response.Status.Family.SUCCESSFUL.equals(cres.getStatusInfo().getFamily())) {
            logger.severe("Request :" + System.lineSeparator() + getRequestString(requestContext) + System.lineSeparator() + "Ended with error response: " + System.lineSeparator() + getResponseString(cres));
        }

    }

    private String getRequestString(ContainerRequestContext request) {
        StringBuilder s = new StringBuilder(request.getMethod() + " " + request.getUriInfo().getAbsolutePath() + System.lineSeparator());
        for (Map.Entry<String, List<String>> entry : request.getHeaders().entrySet()) {
            s.append(entry.getKey()).append("=").append(entry.getValue().parallelStream().collect(Collectors.joining(","))).append(System.lineSeparator());
        }
        String str = (String) request.getProperty(INTERNAL_DATA);

        if (str != null) {
            if (Constants.maxHTTPLoggingBodyCharLength > 0) {
                str = str.substring(0, Math.min(str.length(), Constants.maxHTTPLoggingBodyCharLength));
            }

            if (!str.isEmpty()) {
                s.append("Body:").append(System.lineSeparator());
                s.append(str);
            }
            request.removeProperty(INTERNAL_DATA);

        }


        return s.toString();
    }

    private String getResponseString(ContainerResponseContext response) {
        StringBuilder s = new StringBuilder("Status: " + response.getStatus()).append(System.lineSeparator());
        for (Map.Entry<String, List<Object>> entry : response.getHeaders().entrySet()) {
            s.append(entry.getKey()).append("=").append(entry.getValue().parallelStream().map(f -> f + "").collect(Collectors.joining(","))).append(System.lineSeparator());
        }
        if (response.getEntity() != null) {
            try {
                String str = objectMapper.writeValueAsString(response.getEntity());
                s.append("Body:").append(System.lineSeparator());
                s.append(str);
            } catch (JsonProcessingException e) {
                logger.log(Level.SEVERE, "failed writing response body", e);
            }

        }

        return s.toString();
    }

    private String getEntryString(Map.Entry<String, List<Object>> e) {
        return e.getKey() + "=[" + getValueString(e) + "]";
    }

    private String getValueString(Map.Entry<String, List<Object>> e) {
        return e.getValue().parallelStream().map(f -> f + "").collect(Collectors.joining(","));
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        if (Constants.enableHTTPErrorLog) {
            InputStream entityStream = containerRequestContext.getEntityStream();
            if (entityStream != null) {
                byte[] data=IOUtils.toByteArray(entityStream);
                String body = new String(data);

                containerRequestContext.setEntityStream(new ByteArrayInputStream(data));
                if (!body.isEmpty()) {
                    containerRequestContext.setProperty(INTERNAL_DATA, body);

                }
            }
        }
    }
}
