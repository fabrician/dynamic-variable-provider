/*
 * Copyright (c) 2013 TIBCO Software Inc. All Rights Reserved.
 *
 * Use is subject to the terms of the TIBCO license terms accompanying the download of this code.
 * In most instances, the license terms are contained in a file named license.txt.
 */
package org.fabrican.extension.variable.provider.dao;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectionManager {
	private static final int POOL_SIZE=5;
	private LinkedList<ConnectionWrapper> connPools = new LinkedList<>();
	private ConnectionWrapper[] conns = null;
	private Lock poolLock = new ReentrantLock();
	private Condition accessCond  = poolLock.newCondition(); 
	private boolean inTransition = false;
	private String driver, url, user, pass;
	
	
	public ConnectionWrapper getConnection() throws InterruptedException{
		poolLock.lock();
		try {
			while (conns != null && connPools.isEmpty() || inTransition){
				accessCond.wait();
			}
			if ( conns == null ){
				return null;
			} else {
				return connPools.removeFirst();
			}
		} finally {
			poolLock.unlock();
		}
	}
	
	public void releaseConnection(ConnectionWrapper c){
		poolLock.lock();
		try {
			if ( !inTransition ){
				connPools.addFirst(c);
				accessCond.signal();
			}
		} finally {
			poolLock.unlock();
		}
	}

	public boolean resetConnection(String driver, String url, String username, String password) throws InterruptedException, ClassNotFoundException, SQLException{
		Class.forName(driver);
        LinkedList<ConnectionWrapper> pool = new LinkedList<>();
        for ( int i = 0; i < POOL_SIZE; i++ ){
        	pool.add(new ConnectionWrapper(DriverManager.getConnection(url, username, password)));
        }
		poolLock.lock();
		try {
			while ( inTransition ){
				accessCond.wait();
			}
			inTransition = true;
		} finally {
			poolLock.unlock();
		}
		try {
			if ( conns != null ){
				for ( ConnectionWrapper c : conns){
					c.close();
				}
			} else {
				conns = new ConnectionWrapper[POOL_SIZE];
			}
			Iterator<ConnectionWrapper> iter = pool.iterator();
	        for ( int i = 0; i < POOL_SIZE; i++ ){
	        	conns[i] = iter.next();
	        }
	        connPools = pool;
	        pool = null;
	        setDriver(driver);
	        setUrl(url);
	        setUser(username);
	        setPass(password);
	        return true;
		} finally {
			poolLock.lock();
			try {
				inTransition = false;
				accessCond.signal();
			} finally {
				poolLock.unlock();
			}
			if ( pool != null ){
				for ( ConnectionWrapper c : pool ){
					c.close();
				}
			}
		}
	}
	
	public boolean connected(){
		poolLock.lock();
		try {
			return conns != null && !inTransition;
		} finally {
			poolLock.unlock();
		}
	}

	public String getDriver() {
    	return driver;
    }

	public void setDriver(String driver) {
    	this.driver = driver;
    }

	public String getUrl() {
    	return url;
    }

	public void setUrl(String url) {
    	this.url = url;
    }

	public String getUser() {
    	return user;
    }

	public void setUser(String user) {
    	this.user = user;
    }

	public String getPass() {
    	return pass;
    }

	public void setPass(String pass) {
    	this.pass = pass;
    }

	
	
	

}
