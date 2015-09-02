package net.happyonroad.component.container.support;

import net.happyonroad.component.container.Executable;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * <h1>通过文件与某个已经启动的进程进行通讯(stop)</h1>
 *
 * @author Jay Xiong
 */
public class LauncherThroughFile implements Executable {
    private final int appIndex;

    public LauncherThroughFile(int appIndex) {
        this.appIndex = appIndex;
    }

    @Override
    public void start() throws Exception {
        throw new UnsupportedOperationException("Can't launch app " + appIndex + " through file");
    }

    @Override
    public void exit() {
        File signal = getSignal("exit");
        try {
            //相应的进程，如果存在，看到该信号文件，应该
            // 先检查这个文件的有效性：主要是检查创建时间，是不是晚于start时间，否则为以前的遗留文件，需要删除
            // 如果信号文件有效，则先删除这个文件，再创建一个退出的信号文件，而后执行退出动作
            FileUtils.touch(signal);
        } catch (IOException e) {
            throw new UnsupportedOperationException("Can't touch a signal file " + signal.getPath());
        }
    }

    private File getSignal(String name) {
        if (appIndex == 0) {
            return new File("tmp/" + name);
        } else {
            return new File("tmp/" + name + "." + appIndex);
        }
    }

    @Override
    public boolean exiting() {
        return getSignal("exiting").exists();
    }
}
