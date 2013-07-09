package org.fabrican.extension.variable.provider.rules;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/*
 * Copyright (c) 2013 TIBCO Software Inc. All Rights Reserved.
 *
 * Use is subject to the terms of the TIBCO license terms accompanying the download of this code.
 * In most instances, the license terms are contained in a file named license.txt.
 */
public class RuleSetJSONHelper {
    public static final String KEY_NAME = "name";
    public static final String KEY_DESC = "desc";
    public static final String KEY_VER = "version";
    public static final String KEY_MATCH = "match";
    public static final String KEY_RULES = "rules";
    
    public static final String KEY_V_PKEY = "prim";
    public static final String KEY_V_SKEY = "sec";
    public static final String KEY_V_NAME = "name";
    public static final String KEY_V_VALUE = "value";
    
    
    
    public static JSONObject toJSON(RuleSet rs) throws JSONException{
        JSONObject o = new JSONObject();
        o.put(KEY_NAME, rs.getName());
        if( rs.getDesc() != null ){
            o.put(KEY_DESC, rs.getDesc());
        }
        if( rs.getVersion() != null ){
            o.put(KEY_VER, rs.getVersion().intValue());
        }
        if ( ! (rs instanceof SmallRuleSet) ){
            o.put(KEY_MATCH, rs.isMatchCase());
            List<Rule> rl = rs.getRules();
            if ( rl != null ){
                JSONArray ja = new JSONArray();
                for (Rule r : rl ){
                    JSONObject jr = new JSONObject();
                    if ( r.getPKey() != null ){
                        jr.put(KEY_V_PKEY, r.getPKey());
                    }
                    if ( r.getSKey() != null ){
                        jr.put(KEY_V_SKEY, r.getSKey());
                    }
                    if ( r.getVName() != null ){
                        jr.put(KEY_V_NAME, r.getVName());
                    }
                    if ( r.getVValue() != null ){
                        jr.put(KEY_V_VALUE, r.getVValue());
                    }
                    ja.put(jr);
                }
                o.put(KEY_RULES, ja);
            }
        }
        return o;
    }
    
    public static RuleSet parse(InputStream jsonDataStream) throws JSONException, UnsupportedEncodingException{
        Reader reader = new InputStreamReader(jsonDataStream, "utf-8");
        JSONObject o = new JSONObject( new JSONTokener(reader) );
        RuleSet rst = new RuleSet();
        String name = null;
        rst.setName(o.getString(KEY_NAME));
        if ( o.has(KEY_DESC) ){
            rst.setDesc(o.getString(KEY_DESC));
        }
        if ( o.has(KEY_VER) ){
            rst.setVersion(o.getInt(KEY_VER));
        }
        if ( o.has(KEY_MATCH) ){
            rst.setMatchCase(o.getBoolean(KEY_MATCH));
        }
        if ( o.has(KEY_RULES) ){
            LinkedList<Rule> rules = new LinkedList<>();
            JSONArray jrules = o.getJSONArray(KEY_RULES);
            for ( int i = 0; i < jrules.length(); i++ ){
                JSONObject jr = jrules.getJSONObject(i);
                Rule r = new Rule();
                if ( jr.has(KEY_V_PKEY) ){
                    r.setPKey(jr.getString(KEY_V_PKEY));
                }
                if ( jr.has(KEY_V_SKEY) ){
                    r.setSKey(jr.getString(KEY_V_SKEY));
                }
                if ( jr.has(KEY_V_NAME) ){
                    r.setVName(jr.getString(KEY_V_NAME));
                }
                if ( jr.has(KEY_V_VALUE) ){
                    r.setVValue(jr.getString(KEY_V_VALUE));
                }
                rules.add(r);
            }
            rst.setRules(rules);
        }
        return rst;
    }
    
}
