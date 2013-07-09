/*
 * Copyright (c) 2013 TIBCO Software Inc. All Rights Reserved.
 *
 * Use is subject to the terms of the TIBCO license terms accompanying the download of this code.
 * In most instances, the license terms are contained in a file named license.txt.
 */
package org.fabrican.extension.variable.provider.servlet;

import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fabrican.extension.variable.provider.dao.RuleSetDAO;
import org.fabrican.extension.variable.provider.rules.RuleSet;
import org.fabrican.extension.variable.provider.rules.RuleSetJSONHelper;
import org.fabrican.extension.variable.provider.util.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;


public class ManagementServlet extends HttpServlet{
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            resp.setHeader("Cache-Control", "no-cache");
            Pair<String, String> params = gePathParams(req);
            String cmd = params.left();
            String name = params.right();
            RuleSetDAO dao = RuleSetDAO.getInstance();
            switch (cmd) {
            case "config":
                JSONObject jo = new JSONObject();
                jo.put("connected", dao.connected());
                if (dao.getDriver() != null) {
                    jo.put("driver", dao.getDriver());
                }
                if (dao.getUrl() != null) {
                    jo.put("url", dao.getUrl());
                }
                if (dao.getUser() != null) {
                    jo.put("user", dao.getUser());
                }
                if (dao.getPass() != null) {
                    jo.put("pass", dao.getPass());
                }
                resp.setStatus(200);
                output(resp, jo.toString());
                return;
            case "providers":
                name = name.trim();
                if ( name.equals("") ){
                    resp.setStatus(200);
                    output(resp, getListJSON(dao).toString());
                } else {
                    RuleSet rs = dao.getRuleSetByName(name);
                    if ( rs == null ){
                        resp.setStatus(404);
                    } else {
                        resp.setStatus(200);
                        output(resp, RuleSetJSONHelper.toJSON(rs).toString());
                    }
                }
                return;
            }
        } catch (JSONException e) {
            resp.setStatus(500);
            output(resp, e.toString());
        } catch (SQLException e) {
            resp.setStatus(500);
            output(resp, e.toString());
        } catch (InterruptedException e) {
            resp.setStatus(500);
            output(resp, e.toString());
        } finally {

        }
        resp.setStatus(400);
        output(resp, "unrecognized request");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Pair<String, String> params = gePathParams(req);
            String cmd = params.left();
            String name = params.right();
            RuleSetDAO dao = RuleSetDAO.getInstance();
            resp.setHeader("Cache-Control", "no-cache");
            switch (cmd) {
            case "config":
                InputStreamReader reader = new InputStreamReader(req.getInputStream());
                JSONObject jo = new JSONObject(new JSONTokener(reader));
                String driver = null,
                url = null,
                user = null,
                pass = null;
                if (jo.has("driver")) {
                    driver = jo.getString("driver");
                }
                if (jo.has("url")) {
                    url = jo.getString("url");
                }
                if (jo.has("user")) {
                    user = jo.getString("user");
                }
                if (jo.has("pass")) {
                    pass = jo.getString("pass");
                }
                if (driver == null || url == null) {
                    resp.setStatus(400);
                    output(resp,"missing paramter \"driver\" or \"url\"");
                } else {
                    try {
                        if (dao.resetConnection(driver, url, user, pass)) {
                            resp.setStatus(200);
                        } else {
                            resp.setStatus(400);
                            output(resp,"config failed");
                        }
                    } catch (Exception ex) {
                        resp.setStatus(500);
                        output(resp, ex.toString());
                    }
                }
                return;
            case "providers":
                name = name.trim();
                if ( name.equals("") ){ // add
                    RuleSet r = RuleSetJSONHelper.parse(req.getInputStream());
                    if (dao.addRuleSet(r)){
                        resp.setStatus(201);
                    } else {
                        resp.setStatus(400);
                        output(resp, "add failed.");
                    }                    
                } else {
                    RuleSet r = RuleSetJSONHelper.parse(req.getInputStream());
                    if (dao.updateRuleSet(r)){
                        resp.setStatus(200);
                    } else {
                        resp.setStatus(409);
                        output(resp, "update failed.");
                    }                    
                }                
                return;
            case "command":
                name = name.trim();
                if ( name.equals("copy") ){
                    reader = new InputStreamReader(req.getInputStream());
                    jo = new JSONObject(new JSONTokener(reader));
                    String from = null;
                    String to = null;
                    if ( jo.has("copyFrom") ){
                        from = jo.getString("copyFrom");
                    }
                    if ( jo.has("copyTo") ){
                        to = jo.getString("copyTo");
                    }
                    if ( from == null || to == null || from.equals(to) ){
                        resp.setStatus(400);
                        output(resp, "parameter error");
                        return;
                    }
                    RuleSet rFrom = dao.getRuleSetByName(from);
                    if ( rFrom == null ){
                        resp.setStatus(400);
                        output(resp, "rule not found");
                        return;
                    } else {
                        rFrom.setName(to);
                        if ( dao.addRuleSet(rFrom) ){
                            resp.setStatus(201);
                        } else {
                            resp.setStatus(400);
                            output(resp, "rule not added, " + to);
                        }
                    }
                    return;
                } else if ( name.equals("destroyCache") ){
                    dao.destroyCache();
                    resp.setStatus(201);
                    return;
                }
            }
        } catch (JSONException ex) {
            resp.setStatus(400);
            output(resp,ex.toString());
        } catch (SQLException e) {
            resp.setStatus(500);
            output(resp, e.toString());
        } catch (InterruptedException e) {
            resp.setStatus(500);
            output(resp, e.toString());
        }
        resp.setStatus(400);
        output(resp, "unrecognized request");
    }
    

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Pair<String, String> params = gePathParams(req);
        String cmd = params.left();
        String name = params.right();
        RuleSetDAO dao = RuleSetDAO.getInstance();
        try {
            switch (cmd) {
            case "providers":
                if (name == null || name.trim().length() == 0) {
                    resp.setStatus(400);
                    output(resp, "missing provider identity");
                } else {

                    if (dao.removeRuleSet(name)) {
                        resp.setStatus(200);
                        //output(resp, getListJSON(dao).toString());
                    } else {
                        resp.setStatus(404);
                        output(resp, "failed to delete");
                    }
                }
                return;
            }
        } catch (InterruptedException ex) {
            resp.setStatus(500);
            output(resp, ex.toString());
        } catch (SQLException e) {
            resp.setStatus(500);
            output(resp, e.toString());
        //} catch (JSONException e) {
        //    resp.setStatus(500);
        //    output(resp, e.toString());
        }
        resp.setStatus(400);
        output(resp, "unrecognized request");
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        // TODO Auto-generated method stub
        RuleSetDAO dao = RuleSetDAO.getInstance();
        dao.init();
        super.init(config);
    }
    
    private JSONArray getListJSON(RuleSetDAO dao) throws SQLException, InterruptedException, JSONException{
        List<RuleSet> rl = dao.listRules();
        JSONArray ja = new JSONArray();
        for (RuleSet r : rl) {
            ja.put(RuleSetJSONHelper.toJSON(r));
        }
        return ja;
    }


    private Pair<String, String> gePathParams(HttpServletRequest req){
        Pair<String, String> rst = new Pair<>("", "");
        String path = req.getPathInfo();
        int step = 0;
        if ( path != null ){
            for (String s : path.split("/") ){
                if ( s != null && s.trim().length() > 0 ){
                    if ( step == 0 ){
                        rst.setLeft(s.trim());
                        step++;
                    } else if ( step == 1 ){
                        rst.setRight(s.trim());
                        break;
                    }
                }
            }
        }
        return rst;
    }
    
    private void output(HttpServletResponse resp, String message) throws IOException {
        resp.getOutputStream().write(message.getBytes("utf-8"));
    }
}
