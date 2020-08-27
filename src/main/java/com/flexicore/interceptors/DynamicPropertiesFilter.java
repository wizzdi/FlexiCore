package com.flexicore.interceptors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class DynamicPropertiesFilter implements Filter {

    private static final Logger logger= LoggerFactory.getLogger(DynamicPropertiesFilter.class);
private static  final TypeReference<Map<String, Object>> ref = new TypeReference<Map<String, Object>>() {};

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        boolean doFilter = servletRequest instanceof HttpServletRequest;
        boolean propertiesFiltered=false;
        Map<String, ?> requestBody=null;
        if(doFilter&&servletRequest.getInputStream()!=null){
            HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
            servletRequest=new MultiReadHttpServletRequest(httpServletRequest);
            try {
                requestBody = objectMapper.readValue(servletRequest.getInputStream(), ref);
            }
            catch (Exception e){
                logger.debug("error while deserializing request body",e);
            }
            propertiesFiltered=requestBody!=null&&requestBody.containsKey("propertiesFilter");
            if(propertiesFiltered){
                servletResponse=new ContentCachingResponseWrapper((HttpServletResponse) servletResponse);

            }

        }
        filterChain.doFilter(servletRequest, servletResponse);
        if(propertiesFiltered){
            try {
                if(requestBody.containsKey("propertiesFilter")){
                    ContentCachingResponseWrapper servletResponseWrapper= (ContentCachingResponseWrapper) servletResponse;

                    Object propertiesFilter = requestBody.get("propertiesFilter");
                    if(propertiesFilter instanceof Map){
                        Map<String, Object> filter= (Map<String, Object>) propertiesFilter;
                        Map<String,Object> responseBody=objectMapper.readValue(servletResponseWrapper.getContentAsByteArray(),ref);
                        manualRetroCycle(responseBody);
                        servletResponseWrapper.resetBuffer();
                        Map<String,Object> filtered=filterProperties(responseBody,filter);
                        String s=objectMapper.writeValueAsString(filtered);
                        servletResponseWrapper.getOutputStream().print(s);
                        servletResponseWrapper.copyBodyToResponse();



                    }

                }
            }
            catch (Exception e){
                logger.error("Failed setting properties dynamically",e);
            }
        }



    }

    private void manualRetroCycle(Map<String, Object> responseBody) {
        Map<String,?> refs=getRefs(responseBody);
        manualRetroCycle(responseBody,refs);
    }

    private void manualRetroCycle(Object object, Map<String, ?> refs) {
        if(object instanceof Map){
            Map<String,Object> responseBody= (Map<String, Object>) object;
            for (Map.Entry<String, Object> stringEntry : responseBody.entrySet()) {
                Object val = stringEntry.getValue();
                if(val instanceof String ){
                    String s= (String) val;
                    if(s.length() > 30){
                        Object o=refs.get(s);
                        if(o!=null){
                            stringEntry.setValue(o);
                        }
                    }

                }
                else{
                    manualRetroCycle(val,refs);

                }

            }
        }
        if(object instanceof List){
            List<Object> list= (List<Object>) object;
            for (int i = 0; i < list.size(); i++) {
                Object val=list.get(i);
                if(val instanceof String ){
                    String s= (String) val;
                    if(s.length() > 30){
                        Object o=refs.get(s);
                        if(o!=null){
                            list.set(i,o);
                        }
                    }

                }
                else{
                    manualRetroCycle(val,refs);

                }
            }
        }

    }

    private Map<String, ?> getRefs(Map<String, ?> responseBody) {
        Map<String, Object> refs=new HashMap<>();
        getRefs(responseBody,refs);
        return refs;
    }

    private void getRefs(Object object, Map<String, Object> refs) {
        if(object instanceof Map){
            Map<String,Object> responseBody= (Map<String, Object>) object;
            String jsonId= (String) responseBody.get("json-id");
            if(jsonId!=null){
                refs.put(jsonId,responseBody);
            }
            for (Object value : responseBody.values()) {
                if(value instanceof Map){
                    getRefs( value,refs);
                }
                if(value instanceof List){
                    for (Object o : (List) value) {
                        getRefs(o,refs);
                    }
                }
            }
        }

    }

    private Map<String,Object> filterProperties(Map<String, Object> responseBody, Map<String, Object> filter) {
        Map<String,Object> filteredOut=responseBody.entrySet().stream().filter(f->filterKey(f,filter)).collect(Collectors.toMap(f->f.getKey(),f->f.getValue()));
        for (Map.Entry<String, Object> stringEntry : filter.entrySet()) {
            if( stringEntry.getValue() instanceof Map){
                Map<String,Object> inner= (Map<String, Object>) stringEntry.getValue();

                if(stringEntry.getValue() instanceof Map){
                    Object val=filteredOut.get(stringEntry.getKey());
                    if(val instanceof Map){
                        Map<String, ?> stringMap = filterProperties((Map<String, Object>) val, inner);
                        stringEntry.setValue(stringMap);
                    }
                    if(val instanceof List){
                        List<Object> list= (List<Object>) val;
                        for (int i = 0; i < list.size(); i++) {
                            Object o=list.get(i);
                            if(o instanceof Map){
                                Map<String, ?> e = filterProperties((Map<String, Object>) o, inner);
                                list.set(i, e);
                            }
                        }
                    }

                }
            }

        }
        return filteredOut;
    }

    private boolean filterKey(Map.Entry<String, ?> f, Map<String, ?> filter) {
        String key = f.getKey();
        Object o = filter.get(key);
        return (o instanceof Map )|| (o instanceof Boolean && (Boolean) o);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void destroy() {

    }

}