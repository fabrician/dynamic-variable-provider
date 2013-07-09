/*
 * Copyright (c) 2013 TIBCO Software Inc. All Rights Reserved.
 *
 * Use is subject to the terms of the TIBCO license terms accompanying the download of this code.
 * In most instances, the license terms are contained in a file named license.txt.
 */
package org.fabrican.extension.variable.provider.servlet;

import java.util.HashMap;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static javax.servlet.http.HttpServletResponse.*;
import org.fabrican.extension.variable.provider.dao.RuleSetDAO;
import org.fabrican.extension.variable.provider.rules.RuleSet;
import org.json.JSONObject;

public class RequestsServlet extends HttpServlet {
	private static final String PARAM_PRIMARY = "primary";
	private static final String PARAM_SECONDARY = "secondary";
	
	@Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
		String path = req.getPathInfo();

		String[] pathElements = path.split("/");
		String provider = null;
		for ( String p : pathElements ){
			if ( p.trim().length() > 0 ){
				provider = p.trim();
				break;
			}
		}
		if ( provider != null ){
			try {
				serviceRequest(provider, req, resp);
			} catch(Exception e){
				throw new ServletException("Failed to process variable Request", e);
			}
		} else {
			resp.setStatus(SC_NOT_FOUND);
			return;
		}
			
    }

	private void serviceRequest(String n, HttpServletRequest req, HttpServletResponse resp) throws Exception {
		String pKey = req.getParameter(PARAM_PRIMARY);
		String sKey = req.getParameter(PARAM_SECONDARY);
		RuleSet rs = RuleSetDAO.getInstance().getRuleSetByName(n);
		if ( rs == null ){
			resp.setStatus(SC_NOT_FOUND);
			return;
		} else {
			HashMap<String, String> hm = rs.evaluate(pKey, sKey);
			JSONObject jo = new JSONObject();
			for ( Entry<String, String> e : hm.entrySet() ){
				jo.put(e.getKey(), e.getValue());
			}
			resp.setStatus(SC_OK);
			resp.getOutputStream().write(jo.toString().getBytes("utf-8"));
		}
    }
	

}
