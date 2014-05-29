/**
 * @author XiongJie, Date: 13-8-29
 */
package net.happyonroad.component.core;

/**
 * 组件异常的基类
 */
public class ComponentException extends Exception{
    protected Component component;

    public ComponentException() {
    }

    public ComponentException(String message) {
        super(message);
    }

    public ComponentException(String message, Throwable cause) {
        super(message, cause);
    }

    public Component getComponent() {
        return component;
    }

    public void setComponent(Component component) {
        this.component = component;
    }

    public String getPath(){
        if( this.getCause() != null && this.getCause() instanceof ComponentException){
            return currPath() + " -> " + ((ComponentException) this.getCause()).getPath();
        }else{
            return currPath();
        }
    }

    protected String currPath(){
         return this.component == null ? "/" : this.component.toString();
    }

    public Throwable getRootCause(){
        Throwable rootCause = this;
        while(rootCause.getCause() != null){
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }
}


