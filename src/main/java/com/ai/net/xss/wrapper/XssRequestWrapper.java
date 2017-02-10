package com.ai.net.xss.wrapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.lang.StringEscapeUtils;
import org.owasp.validator.html.AntiSamy;
import org.owasp.validator.html.CleanResults;
import org.owasp.validator.html.Policy;
import org.owasp.validator.html.PolicyException;
import org.owasp.validator.html.ScanException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ai.net.xss.util.CollectionUtil;
import com.ai.net.xss.util.StringUtil;
import com.alibaba.fastjson.JSON;

/**
 * HttpServletRequest的封装器
 * Date: 2017年2月9日 <br>
 * Copyright (c) 2017 asiainfo.com <br>
 * 
 * @author
 */
public class XssRequestWrapper extends HttpServletRequestWrapper {

    private static Logger log=LoggerFactory.getLogger(XssRequestWrapper.class);
    private List<String> ignoreParamValueList;
    private static final String ANTISAMY_SLASHDOT_XML = "antisamy-slashdot-1.4.4.xml";
    private static Policy policy = null;

    static {
        log.info(" start read XSS configfile [" + ANTISAMY_SLASHDOT_XML + "]");
        InputStream inputStream = XssRequestWrapper.class.getClassLoader().getResourceAsStream(ANTISAMY_SLASHDOT_XML);
        try {
            policy = Policy.getInstance(inputStream);
            log.info("read XSS configfile [" + ANTISAMY_SLASHDOT_XML + "] success");
        } catch (PolicyException e) {
            log.error("read XSS configfile [" + ANTISAMY_SLASHDOT_XML + "] fail , reason:", e);
        }
        finally{
        	if(inputStream!=null){
        		try {
					inputStream.close();
				} catch (IOException e) {
					log.error("close XSS configfile [" + ANTISAMY_SLASHDOT_XML + "] fail , reason:", e);
				}
        	}
        }
    }

    public XssRequestWrapper(HttpServletRequest request,List<String> ignoreParamValueList) {
        super(request);
        this.ignoreParamValueList=ignoreParamValueList;
    }

    /**
     * 获取参数Map
     */
    @SuppressWarnings("rawtypes")
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> request_map = super.getParameterMap();
        Iterator iterator = request_map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry me = (Map.Entry) iterator.next();
            log.info(me.getKey()+":");
            String[] values = (String[]) me.getValue();
            for (int i = 0; i < values.length; i++) {
            	log.info(values[i]);
                values[i] = xssClean(values[i]);
            }
        }
        return request_map;
    }

    /**
     * 获取参数值
     */
    public String[] getParameterValues(String paramString) {
        String[] arrayOfString1 = super.getParameterValues(paramString);
        if (arrayOfString1 == null)
            return null;
        int i = arrayOfString1.length;
        String[] arrayOfString2 = new String[i];
        for (int j = 0; j < i; j++)
            arrayOfString2[j] = xssClean(arrayOfString1[j]);
        return arrayOfString2;
    }

    /**
     * 获取参数
     */
    public String getParameter(String paramString) {
        String str = super.getParameter(paramString);
        if (str == null)
            return null;
        return xssClean(str);
    }

    /**
     * 获取报文头信息
     */
    public String getHeader(String paramString) {
        String str = super.getHeader(paramString);
        if (str == null)
            return null;
        return xssClean(str);
    }

    /**
     * 
     * @param paramValue
     * @return
     * @author
     */
    private String xssClean(String paramValue) {
        AntiSamy antiSamy = new AntiSamy();
        log.info("ignoreParamValueList="+JSON.toJSONString(ignoreParamValueList));
        try {
        	log.info("raw value before xssClean: " + paramValue);
        	if(isIgnoreParamValue(paramValue)){
        		log.info("ignore the xssClean,keep the raw paramValue: " + paramValue);
        		return paramValue;
        	}
        	else{
                final CleanResults cr = antiSamy.scan(paramValue, policy);
                String str = StringEscapeUtils.escapeHtml(cr.getCleanHTML());
                str = str.replaceAll((antiSamy.scan("&nbsp;", policy)).getCleanHTML(), "");
                str = StringEscapeUtils.unescapeHtml(str);
                str = str.replaceAll("&quot;", "\"");
                log.info("xssfilter value after xssClean：" + str);
                return str;
        	}
            
        } catch (ScanException e) {
            log.error("scan failed ，parmter is [" + paramValue + "]", e);
        } catch (PolicyException e) {
            log.error("antisamy convert failed ，parmter is [" + paramValue + "]", e);
        }
        return paramValue;
    }
    
    /**
     * 判断白名单
     * @param paramValue
     * @return
     * @author
     */
    private boolean isIgnoreParamValue(String paramValue) {
    	if(StringUtil.isBlank(paramValue)){
    		return true;
    	}
        if (CollectionUtil.isEmpty(ignoreParamValueList))
        {
        	return false;
        }
        else {
        	for(String ignoreParamValue:ignoreParamValueList){
        		if(paramValue.contains(ignoreParamValue)){
        			return true;
        		}
        	}
        }        
        return false;
    }

}