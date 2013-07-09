/*
 * Copyright (c) 2013 TIBCO Software Inc. All Rights Reserved.
 *
 * Use is subject to the terms of the TIBCO license terms accompanying the download of this code.
 * In most instances, the license terms are contained in a file named license.txt.
 */
package org.fabrican.extension.variable.provider.dao;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.prefs.Preferences;

import org.fabrican.extension.variable.provider.rules.Rule;
import org.fabrican.extension.variable.provider.rules.RuleSet;
import org.fabrican.extension.variable.provider.rules.SmallRuleSet;

public class RuleSetDAO {
	private ConnectionManager manager = new ConnectionManager();
	private static RuleSetDAO instance = new RuleSetDAO();
	public static final String LIST = "list";
	public static final String FETCH = "fetch";
	public static final String ADD = "add";
	public static final String REMOVE = "remove";
	public static final String UPDATE = "update";
	public static final String FORCE_UPDATE = "force_update";
	private static HashMap<String, String> hmStmts = new HashMap<>();

	private static final int RULE_CHUNKS = 10;
	private static final int RULE_CHUNK_SIZE = 4000;
	private CacheManager cache = new CacheManager();
	static {
		hmStmts.put(LIST, "select name, description, version from variable_provider" );
		hmStmts.put(FETCH, "select name, description, version, match_case, rules1, rules2, rules3, rules4, rules5, rules6, rules7, " +
				"rules8, rules9, rules10 from variable_provider where name = ?" );
		hmStmts.put(ADD, "insert into variable_provider ( name, description, version, match_case, rules1, rules2, rules3, rules4, rules5, rules6, rules7," +
				"rules8, rules9, rules10 ) values( ?,?,?,?,?,?,?,?,?,?,?,?,?,?)" );
		hmStmts.put(REMOVE, "delete from variable_provider where name = ?" );
		hmStmts.put(UPDATE, "update variable_provider set description = ?, version = version+1, match_case = ?, rules1 = ?, rules2 = ?, " +
				"rules3 = ?, rules4 = ?, rules5 = ?, rules6 = ?, rules7 = ?, rules8 = ?, rules9 = ?, rules10 = ? where name = ? and version = ?" );
		hmStmts.put(FORCE_UPDATE, "update variable_provider set description = ?, version = version+1, match_case = ?, rules1 = ?, rules2 = ?. " +
				"rules3 = ?, rules4 = ?, rules5 = ?, rules6 = ?, rules7 = ?, rules8 = ?, rules9 = ?, rules10 = ? where name = ?" );
	}
	private static final String PREF_DRIVER = "pd";
	private static final String PREF_URL = "pl";
	private static final String PREF_USER = "pu";
	private static final String PREF_PASS = "pp";
	
	private RuleSetDAO(){
		
	}
	public static RuleSetDAO getInstance(){
		return instance;
	}

	public List<RuleSet> listRules() throws SQLException, InterruptedException {
		ConnectionWrapper c = null;
		ResultSet rs = null;
		try {
			List<RuleSet> cached = cache.getRuleSetList();
			if ( cached != null ){
				return cached;
			}
			String cmd = LIST;
	        c = manager.getConnection();
	        PreparedStatement s = c.getStatement(cmd);
	        if ( s == null ){
	        	s = c.prepareStatement(cmd, hmStmts.get(cmd));
	        }
	        rs = s.executeQuery();
	        LinkedList<RuleSet> rst = new LinkedList<>();
	        while ( rs.next() ){
	        	RuleSet r = new SmallRuleSet();
	        	String name = rs.getString(1);
	        	r.setName(name);
	        	String desc = rs.getString(2);
	        	if (!rs.wasNull()){
	        		r.setDesc(desc);
	        	}
	        	int ver = rs.getInt(3);
	        	if ( !rs.wasNull() ){
	        		r.setVersion(ver);
	        	}
	        	rst.add(r);
	        }
	        cache.touchRuleSetList(rst);
	        return rst;
	        
        }  finally {
        	if ( c != null ){
        		manager.releaseConnection(c);
        	}
        	if ( rs != null ){
        		rs.close();
        	}
        }
	}
	
	public RuleSet getRuleSetByName(String key) throws SQLException, InterruptedException, IOException {
		ConnectionWrapper c = null;
		ResultSet rs = null;
		try {
			RuleSet cached = cache.getRuleSet(key);
			if ( cached != null ){
				return cached;
			}
	        c = manager.getConnection();
			String cmd = FETCH;
	        PreparedStatement s = c.getStatement(cmd);
	        if ( s == null ){
	        	s = c.prepareStatement(cmd, hmStmts.get(cmd));
	        }
	        s.setString(1, key);
	        rs = s.executeQuery();
	        if ( rs.next() ){
	        	RuleSet r = new RuleSet();
	        	String name = rs.getString(1);
	        	r.setName(name);
	        	String desc = rs.getString(2);
	        	if (!rs.wasNull()){
	        		r.setDesc(desc);
	        	}
	        	int ver = rs.getInt(3);
	        	if ( !rs.wasNull() ){
	        		r.setVersion(ver);
	        	}
	        	boolean matchCase = rs.getBoolean(4);
	        	if ( !rs.wasNull() ){
	        		r.setMatchCase(matchCase);
	        	} 
	        	byte[][] rules = new byte[RULE_CHUNKS][];
	        	for ( int i = 0; i < RULE_CHUNKS; i++){
	        		byte[] rChunk = rs.getBytes(5+i);
	        		if ( rs.wasNull() ){
	        			rules[i]= null;
	        		} else {
	        			rules[i]= rChunk;
	        		}
	        	}
	        	r.setRules(rules);
	        	cache.addRuleSet(r);
	        	return r;
	        }
	        return null;
	        
        }  finally {
        	if ( c != null ){
        		manager.releaseConnection(c);
        	}
        	if ( rs != null ){
        		rs.close();
        	}
        }
	}
	
	public boolean addRuleSet(RuleSet r) throws SQLException, InterruptedException, IOException {
		ConnectionWrapper c = null;
		try {
	        c = manager.getConnection();
			String cmd = ADD;
	        PreparedStatement s = c.getStatement(cmd);
	        if ( s == null ){
	        	s = c.prepareStatement(cmd, hmStmts.get(cmd));
	        }
	        s.setString(1, r.getName());
	        if ( r.getDesc() != null ){
	        	s.setString(2, r.getDesc());
	        } else {
	        	s.setNull(2, Types.VARCHAR);
	        }
	        s.setInt(3, 1);
	        s.setBoolean(4, r.isMatchCase());
	        byte[][] rules = r.encodeRules(RULE_CHUNKS, RULE_CHUNK_SIZE);
	        int i = 0;
	        for ( byte[] ruleChunk : rules ){
	        	if (ruleChunk != null ){
	        		s.setBytes(5 + i++, ruleChunk);
	        	} else {
	        		s.setNull(5 + i++, Types.BINARY);
	        	}
	        }
	        boolean rst =  s.executeUpdate() == 1;
	        if ( rst ){
	        	r.setVersion(1);
	        	cache.addRuleSet(r);
	        }
	        return rst;
        }  finally {
        	if ( c != null ){
        		manager.releaseConnection(c);
        	}
        }
	}
	
	public boolean removeRuleSet(String name) throws SQLException, InterruptedException, IOException {
		ConnectionWrapper c = null;
		try {
	        c = manager.getConnection();
			String cmd = REMOVE;
	        PreparedStatement s = c.getStatement(cmd);
	        if ( s == null ){
	        	s = c.prepareStatement(cmd, hmStmts.get(cmd));
	        }
	        s.setString(1, name);
	        boolean rst =  s.executeUpdate() == 1;
	        if ( rst ){
	        	cache.evictRuleSet(name);
	        }
	        return rst;
        }  finally {
        	if ( c != null ){
        		manager.releaseConnection(c);
        	}
        }
	}
	
	public boolean updateRuleSet(RuleSet r) throws SQLException, InterruptedException, IOException {
		ConnectionWrapper c = null;
		try {
	        c = manager.getConnection();
			String cmd = UPDATE;
	        PreparedStatement s = c.getStatement(cmd);
	        if ( s == null ){
	        	s = c.prepareStatement(cmd, hmStmts.get(cmd));
	        }
	        if ( r.getDesc() != null ){
	        	s.setString(1, r.getDesc());
	        } else {
	        	s.setNull(1, Types.VARCHAR);
	        }
	        s.setBoolean(2, r.isMatchCase());
	        byte[][] rules = r.encodeRules(RULE_CHUNKS, RULE_CHUNK_SIZE);
	        int i = 0;
	        for ( byte[] ruleChunk : rules ){
	        	if (ruleChunk != null ){
	        		s.setBytes(3 + i++, ruleChunk);
	        	} else {
	        		s.setNull(3 + i++, Types.BINARY);
	        	}
	        }
	        s.setString(3 + RULE_CHUNKS, r.getName());
	        s.setInt(4 + RULE_CHUNKS, r.getVersion());
	        if ( s.executeUpdate() == 1 ){
	        	r.setVersion(r.getVersion()+1);
	        	cache.updateRuleSet(r);
	        	return true;
	        } else {
	        	return false;
	        }
	        
        }  finally {
        	if ( c != null ){
        		manager.releaseConnection(c);
        	}
        }
	}
	
	public Preferences getConfig(){
		Preferences node = null;
		try {
			node = Preferences.userRoot().node(RuleSet.class.getName());
		} catch (Exception ex){
			
		}
		if ( node == null){
			try {
				node = Preferences.systemRoot().node(RuleSet.class.getName());
			} catch (Exception ex){
				
			}
		}
		return node;
	}

	public boolean connected(){
		return manager.connected();
	}
	
	
	public boolean resetConnection(String driver, String url, String user, String pass) throws InterruptedException, ClassNotFoundException, SQLException{
		boolean rst = manager.resetConnection(driver, url, user, pass);
		Preferences prefs = getConfig();
		if ( rst && prefs != null ){
			prefs.put(PREF_DRIVER, driver);
			prefs.put(PREF_URL, url);
			prefs.put(PREF_USER, user);
			prefs.put(PREF_PASS, pass);
		}
		if ( rst ){
			cache.clear();
		}
		return rst;
		
	}
	
	public boolean init(){
		Preferences prefs = getConfig();
		if ( prefs == null ){
			return false;
		}
			
		String driver = prefs.get(PREF_DRIVER,"");
		String url = prefs.get(PREF_URL,"");
		String user = prefs.get(PREF_USER, "");
		String pass = prefs.get(PREF_PASS, "");
		if ( driver.length() > 0 || url.length() > 0 ){
			try {
				return manager.resetConnection(driver, url, user, pass);
			} catch (Exception ex){
				return false;
			}
		} else {
			return false;
		}
	}
	public String getDriver() {
    	return manager.getDriver();
    }
	public String getUrl() {
    	return manager.getUrl();
    }
	public String getUser() {
    	return manager.getUser();
    }
	public String getPass() {
    	return manager.getPass();
    }
	
	public void destroyCache(){
		cache.clear();
	}

//	public static void main(String[] args) throws Exception{
//		RuleSetDAO dao = RuleSetDAO.getInstance();
//		boolean inited = dao.init();
//		if ( !inited ){
//			dao.resetConnection("org.hsqldb.jdbcDriver", "jdbc:hsqldb:hsql://localhost:5000/vprovider", "admin", "admin");
//		} else {
//			System.out.println("preference worked");
//		}
//		System.out.println(dao.addRuleSet(r1("r1")));		
//		System.out.println(dao.addRuleSet(r2()));
//		for ( int i = 0; i < 20 ; i++ ){
//			dao.addRuleSet(r1("r-" + i));
//		}
//		List<RuleSet> rl = dao.listRules();
//		for ( RuleSet r : rl ){
//			System.out.println("r:" + r.getName() + " ,desc:" + r.getDesc() + " ,ver:" + r.getVersion());
//			//dao.removeRuleSet(r);
//		}
//		rl = dao.listRules();
//		RuleSet r1 = dao.getRuleSetByName("r1");
//		dumpRuleSet(r1);
//		r1 = dao.getRuleSetByName("r1");
//		RuleSet r2 = dao.getRuleSetByName("r2");
//		dumpRuleSet(r2);
//		r1 = dao.getRuleSetByName("r1");
//		r1.setDesc("r1 desc 001");
//		System.out.println("update again r1, " + dao.updateRuleSet(r1));
//		r1 = dao.getRuleSetByName("r1");
//		
//		//dao.resetConnection("org.hsqldb.jdbcDriver", "jdbc:hsqldb:hsql://localhost:5000/vprovider", "admin", "admin");
//		rl = dao.listRules();
//		for ( RuleSet r : rl ){
//			System.out.println("r:" + r.getName() + " ,desc:" + r.getDesc() + " ,ver:" + r.getVersion());
//		}
//		System.out.println("adding rule r3 " + dao.addRuleSet(r1("r4")));
//		r1 = dao.getRuleSetByName("r4");
//		dumpRuleSet(r1);
//		rl = dao.listRules();
//		for ( RuleSet r : rl ){
//			System.out.println("r:" + r.getName() + " ,desc:" + r.getDesc() + " ,ver:" + r.getVersion());
//			//dao.removeRuleSet(r);
//		}
//		System.out.println("deleting rule r3 " + dao.removeRuleSet("r4"));
//		r1 = dao.getRuleSetByName("r4");
//		dumpRuleSet(r1);
//		rl = dao.listRules();
//		for ( RuleSet r : rl ){
//			System.out.println("r:" + r.getName() + " ,desc:" + r.getDesc() + " ,ver:" + r.getVersion());
//			//dao.removeRuleSet(r);
//		}
//
//		System.out.println("Done");
//		
//	}
//	
//	private static void dumpRuleSet(RuleSet r1) {
//		System.out.println("=================");
//		if ( r1 == null ){
//			System.out.println("null");
//			
//		} else {
//		System.out.println("name: " + r1.getName());
//		System.out.println("desc: " + r1.getDesc());
//		System.out.println("ver:" + r1.getVersion());
//		System.out.println("m_c:" + r1.isMatchCase());
//		List<Rule> rl = r1.getRules();
//		if ( rl != null ){
//			for (Rule rule : rl ){
//				System.out.println (rule.getPKey() + "," + rule.getSKey() + ", " + rule.getVName() + ", " + rule.getVValue());
//			}
//		}
//		}
//		System.out.println("----------------");
//
//	    
//    }
//
//	private static RuleSet r1(String name){
//		RuleSet r1 = new RuleSet();
//		r1.setName(name);
//		r1.setDesc(name + " desc");
//		List<Rule> rules = new LinkedList<>();
//		Rule r = new Rule();
//		r.setPKey("*");
//		r.setSKey("*");
//		r.setVName("HOME");
//		r.setVValue("/home/ffan");
//		rules.add(r);
//		r = new Rule();
//		r.setPKey("a");
//		r.setSKey("*");
//		r.setVName("HOME");
//		r.setVValue("/home/a");
//		rules.add(r);
//		r1.setRules(rules);
//		return r1;
//
//	}
//	private static RuleSet r2(){
//		RuleSet r1 = new RuleSet();
//		r1.setName("r2");
//		r1.setDesc("r2 desc");
//		List<Rule> rules = new LinkedList<>();
//		Rule r = new Rule();
//		r.setPKey("*");
//		r.setSKey("*");
//		r.setVName("HOME1");
//		r.setVValue("/home/ffan1");
//		rules.add(r);
//		r = new Rule();
//		r.setPKey("a");
//		r.setSKey("*");
//		r.setVName("HOME1");
//		r.setVValue("/home/a1");
//		rules.add(r);
//		r1.setRules(rules);
//		return r1;
//
//	}
}
