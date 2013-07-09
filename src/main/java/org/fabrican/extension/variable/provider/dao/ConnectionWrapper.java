/*
 * Copyright (c) 2013 TIBCO Software Inc. All Rights Reserved.
 *
 * Use is subject to the terms of the TIBCO license terms accompanying the download of this code.
 * In most instances, the license terms are contained in a file named license.txt.
 */
package org.fabrican.extension.variable.provider.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;

public class ConnectionWrapper{
    private Connection c;
    private HashMap<String, PreparedStatement> stmts = new HashMap<>();

    public ConnectionWrapper(Connection c){
        this.c = c;
    }
    
    public void close(){
        try {
            c.close();
        }catch(Exception e){
            
        }
    }
    
    public PreparedStatement getStatement(String name) {
            return stmts.get(name);
    }

    public PreparedStatement prepareStatement(String key, String stmt) throws SQLException {
        PreparedStatement ps = c.prepareStatement(stmt);
        stmts.put(key, ps);
        return ps;
    }
    
    
}
