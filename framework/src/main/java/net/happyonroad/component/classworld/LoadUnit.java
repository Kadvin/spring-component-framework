/**
 * Developer: Kadvin Date: 14-2-17 上午10:18
 */
package net.happyonroad.component.classworld;

import java.io.PrintStream;

/**
 * 加载某个类的计量单元
 */
public class LoadUnit {
    private int    loading;
    private String result;
    private int    loaded;
    private int    missed;


    public void loading() {
        loading++;
    }

    public void loaded(String hit) {
        result = hit;
        loaded++;
    }


    public void dump(PrintStream sw) {
        if( result != null ){
            sw.append("    Loading ")
              .append(String.valueOf(loading))
              .append(" times by ")
              .append(result)
              .append(" ")
              .append(String.valueOf(loaded))
              .append(" times\n");
        }else{
            sw.append("    Missing ")
              .append(String.valueOf(missed))
              .append(" times\n");
        }
    }

    public void missing() {
        missed ++;
    }

    public boolean isMissed() {
        return missed > 0 ;
    }
}
