package org.eclipse.releng.j2;

import java.util.ArrayList;

import org.eclipse.releng.j1.ListManip;


public class UseList {

    private boolean notUsedBoolan = false;
    private ArrayList notUsedArrayList = new ArrayList();
    public UseList() {
        ListManip otherInstance = new ListManip();
        otherInstance.testInstance();
    }

}
