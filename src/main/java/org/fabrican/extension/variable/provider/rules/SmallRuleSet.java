/*
 * Copyright (c) 2013 TIBCO Software Inc. All Rights Reserved.
 *
 * Use is subject to the terms of the TIBCO license terms accompanying the download of this code.
 * In most instances, the license terms are contained in a file named license.txt.
 */
package org.fabrican.extension.variable.provider.rules;

import java.io.IOException;
import java.util.List;

public class SmallRuleSet extends RuleSet{

    @Override
    public boolean isMatchCase()  {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    public void setMatchCase(boolean matchCase) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    public List<Rule> getRules() {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    public void setRules(byte[][] bAryAry) throws IOException {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new RuntimeException("Operation not supported");
    }
}
