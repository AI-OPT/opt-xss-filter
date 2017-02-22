package com.ai.net.xss.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ai.net.xss.util.CollectionUtil;
import com.ai.net.xss.util.StringUtil;
import com.ai.net.xss.wrapper.XssRequestWrapper;
import com.alibaba.fastjson.JSON;

public class XSSFilter implements Filter {

    private static Logger log=LoggerFactory.getLogger(XSSFilter.class);
    
    private static final String IGNORE_PATH="ignorePath";  //可放行的请求路径
    private static final String IGNORE_PARAM_VALUE="ignoreParamValue";//可放行的参数值

    private List<String> ignorePathList;//可放行的请求路径列表
    private List<String> ignoreParamValueList;//可放行的参数值列表
    
    //默认放行单点登录的登出响应(响应中包含samlp:LogoutRequest标签，直接放行)
    private static final String CAS_LOGOUT_RESPONSE_TAG="samlp:LogoutRequest";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    	log.debug("XSS fiter [XSSFilter] init start ...");
    	String ignorePaths = filterConfig.getInitParameter(IGNORE_PATH);
        String ignoreParamValues = filterConfig.getInitParameter(IGNORE_PARAM_VALUE);
        if (!StringUtil.isBlank(ignorePaths)) {
        	String[] ignorePathArr = ignorePaths.split(",");
        	ignorePathList=Arrays.asList(ignorePathArr);
        }        
        if (!StringUtil.isBlank(ignoreParamValues)) {
        	String[] ignoreParamValueArr = ignoreParamValues.split(",");
        	ignoreParamValueList=Arrays.asList(ignoreParamValueArr);
        	//默认放行单点登录的登出响应(响应中包含samlp:LogoutRequest标签，直接放行)
        	if(!ignoreParamValueList.contains(CAS_LOGOUT_RESPONSE_TAG)){
        		ignoreParamValueList.add(CAS_LOGOUT_RESPONSE_TAG);
        	}
        }
        else{
        	//默认放行单点登录的登出响应(响应中包含samlp:LogoutRequest标签，直接放行)
        	ignoreParamValueList=new ArrayList<String>();
        	ignoreParamValueList.add(CAS_LOGOUT_RESPONSE_TAG);
        }
        log.debug("ignorePathList="+JSON.toJSONString(ignorePathList));
        log.debug("ignoreParamValueList="+JSON.toJSONString(ignoreParamValueList));
        log.debug("XSS fiter [XSSFilter] init end");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        log.debug("XSS fiter [XSSFilter] starting");
        // 判断uri是否包含项目名称
        String uriPath = ((HttpServletRequest) request).getRequestURI();
        if (isIgnorePath(uriPath)) {
        	log.debug("ignore xssfilter,path["+uriPath+"] pass through XssFilter, go ahead...");
            chain.doFilter(request, response);
            return;
        } else {
        	log.debug("has xssfiter path["+uriPath+"] need XssFilter, go to XssRequestWrapper");
            chain.doFilter(new XssRequestWrapper((HttpServletRequest) request,ignoreParamValueList), response);
        }
        log.debug("XSS fiter [XSSFilter] stop");
    }

    @Override
    public void destroy() {
        log.debug("XSS fiter [XSSFilter] destroy");
    }

    private boolean isIgnorePath(String servletPath) {
    	if(StringUtil.isBlank(servletPath)){
    		return true;
    	}
    	if (CollectionUtil.isEmpty(ignorePathList))
        {
        	return false;
        }
        else{
        	for(String ignorePath:ignorePathList){
        		if(!StringUtil.isBlank(ignorePath)&&servletPath.contains(ignorePath.trim())){
        			return true;
        		}
        	}
        }
        
        return false;
    }

}
