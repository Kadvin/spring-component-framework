/**
 * Developer: Kadvin Date: 14-2-17 上午10:18
 */
package net.happyonroad.component.classworld;


/**
 * 加载某个类的计量单元
 */
public class LoadUnit {
    private int    loading;
    private int    missed;


    public void loading() {
        loading++;
    }

    public void missing() {
        missed ++;
    }

    public boolean isMissed() {
        return missed > 0 ;
    }
}
