package org.eclipse.releng.main;
import java.util.ArrayList;
import org.eclipse.releng.j2.UseList;


public class MainClass {

    private ArrayList notUsedArrayList = new ArrayList();
    
    public MainClass() {
        
    }

    public static void main(String[] args) {
       new MainClass().testCode();
    }
    private void testCode() {
        UseList useList = new UseList();

        System.out.println(useList.toString());
    }

}
