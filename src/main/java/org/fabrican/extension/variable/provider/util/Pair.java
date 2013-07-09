/*
 * Copyright (c) 2013 TIBCO Software Inc. All Rights Reserved.
 *
 * Use is subject to the terms of the TIBCO license terms accompanying the download of this code.
 * In most instances, the license terms are contained in a file named license.txt.
 */
package org.fabrican.extension.variable.provider.util;

public class Pair<T, U> {
    private T left;
    private U right;
    public Pair(){
        
    }
    public Pair(T l, U r){
        this.left = l;
        this.right = r;
    }
    public T left(){
        return left;
    }
    public U right(){
        return right;
    }
    public void setLeft(T l){
        this.left = l;
    }
    public void setRight(U r){
        this.right = r;
    }

}
