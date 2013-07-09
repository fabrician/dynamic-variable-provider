/*
 * Copyright (c) 2013 TIBCO Software Inc. All Rights Reserved.
 *
 * Use is subject to the terms of the TIBCO license terms accompanying the download of this code.
 * In most instances, the license terms are contained in a file named license.txt.
 */
package org.fabrican.extension.variable.provider.dao;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.fabrican.extension.variable.provider.rules.RuleSet;
import org.fabrican.extension.variable.provider.rules.SmallRuleSet;

public class CacheManager {
    private LinkedList<RuleSet> listCache = null;
    private HashMap<String, RuleSet> hmRuleSetList = new HashMap<>();
    private HashMap<String, RuleSet> hmRuleSetList_work = new HashMap<>();
    private HashMap<String, RuleSet>  ruleSetCache = new HashMap<>();
    private Lock cacheLock = new ReentrantLock();
    private static final long TIME_OUT = 30 * 60 *1000; // 30 minutes
    private long LIST_LAST_UPDATED = -1;
    private HashMap<String, Long> hmLastUpdated = new HashMap<>();
    

    public List<RuleSet> getRuleSetList(){
        cacheLock.lock();
        try {
            if( listCache != null && !expired() ){
                return listCache;
            } else {
                return null;
            }
        } finally {
            cacheLock.unlock();
        }
    }

    public void touchRuleSetList(LinkedList<RuleSet> ruleSetList ){
        cacheLock.lock();
        try{
            boolean toUpdate = true;
            hmRuleSetList_work.clear();
            for ( RuleSet r : ruleSetList ){
                hmRuleSetList_work.put(r.getName(), r);
                RuleSet r1 = ruleSetCache.get(r.getName());
                if ( r1 != null ){
                    if ( r.getVersion() > r1.getVersion() ){
                        ruleSetCache.remove(r.getName());
                    } else if ( r.getVersion() < r1.getVersion() ){
                        toUpdate = false;
                    }
                }
            }
            if ( toUpdate ){
                LIST_LAST_UPDATED = System.currentTimeMillis();
                listCache = ruleSetList;
                HashMap<String, RuleSet> t = hmRuleSetList;
                hmRuleSetList = hmRuleSetList_work;
                hmRuleSetList_work = t;
                
            }
        } finally {
            cacheLock.unlock();
        }
    }
    
    public void evictRuleSetList(){
        cacheLock.lock();
        try{
            listCache = null;
            hmRuleSetList.clear();
        } finally {
            cacheLock.unlock();
        }
    }
    
    public void updateRuleSet(RuleSet r){
        cacheLock.lock();
        try {
            RuleSet old = ruleSetCache.get(r.getName());
            if ( old == null || old.getVersion() <= r.getVersion() ){
                ruleSetCache.put(r.getName(), r.deepCopy());
                hmLastUpdated.put(r.getName(), System.currentTimeMillis());
                RuleSet rs = hmRuleSetList.get(r.getName());
                if ( rs != null ){
                    r.shallowCopy(rs);
                } else {
                    rs = new SmallRuleSet();
                    r.shallowCopy(rs);
                    hmRuleSetList.put(r.getName(), rs);
                    if ( listCache != null ){
                        listCache.addLast(rs);
                    }
                }                
            } else { // strange
                listCache = null;
                hmRuleSetList.clear();
            }
        } finally {
            cacheLock.unlock();
        }
    }

    
    public void addRuleSet(RuleSet r){
        cacheLock.lock();
        try {
            RuleSet old = ruleSetCache.get(r.getName());
            if ( old == null ){
                //assert(old.getVersion() < r.getVersion());
                RuleSet rs = hmRuleSetList.get(r.getName());
                if ( rs != null ){
                    r.shallowCopy(rs);
                } else {
                    rs = new SmallRuleSet();
                    r.shallowCopy(rs);
                    hmRuleSetList.put(r.getName(), rs);
                    if ( listCache != null ){
                        listCache.addLast(rs);
                    }
                }                
            } else { // strange
                //shouldn't happen
                listCache = null;
                hmRuleSetList.clear();
            }
            ruleSetCache.put(r.getName(), r.deepCopy());
            hmLastUpdated.put(r.getName(), System.currentTimeMillis());
        } finally {
            cacheLock.unlock();
        }
    }
    
    public void evictRuleSet(String name){
        cacheLock.lock();
        try {
            ruleSetCache.remove(name);
            RuleSet rs = hmRuleSetList.get(name);
            // it's possible that same rule is added right after a delete
            // we haven't handled that
            if ( rs != null ){
                hmRuleSetList.remove(name);
                Iterator<RuleSet> it = listCache.iterator();
                while (it.hasNext() ){
                    RuleSet r = it.next();
                    if( r.getName().equals(name) ){
                        it.remove();
                        break;
                    }
                }
            } else {
                listCache = null;
                hmRuleSetList.clear();
            }                

        } finally {
            cacheLock.unlock();
        }
    }
    
    public RuleSet getRuleSet(String name){
        cacheLock.lock();
        try {
            RuleSet rst = ruleSetCache.get(name);
            if ( rst != null  ){
                if ( expired(rst) ){
                    return null;
                } else {
                    return rst.deepCopy();
                }
            } else {
                return null;
            }
        } finally {
            cacheLock.unlock();
        }
    }

    public void clear(){
        cacheLock.lock();
        try {
            listCache = null;
            ruleSetCache.clear();
            hmRuleSetList.clear();
        } finally {
            cacheLock.unlock();
        }
    }
    
    private boolean expired(RuleSet rst) {
        Long lastUpdated = hmLastUpdated.get(rst.getName());
        if( lastUpdated == null ){
            return true;
        } else {
            return System.currentTimeMillis() - lastUpdated > TIME_OUT;
        }
    }
    
    private boolean expired() {
        return System.currentTimeMillis() - LIST_LAST_UPDATED > TIME_OUT;

    }
}
