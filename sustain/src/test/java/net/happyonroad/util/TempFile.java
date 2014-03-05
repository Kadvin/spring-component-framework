/**
 * @author XiongJie, Date: 13-10-29
 */
package net.happyonroad.util;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static java.lang.String.format;

/** 临时文件相关工具方法 */
public class TempFile {

    //Clean my temp dir
    static {
        String folderPath = System.getProperty("java.io.tmpdir");
        File tmpdir = new File(folderPath, "net.happyonroad.spring");
        try {
            FileUtils.deleteDirectory(tmpdir);
            if (!tmpdir.mkdirs()) {
                System.err.println("Can't make tmp dir: " + tmpdir.getAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println(String.format("Can't clean tmp dir: %s, because of: %s",
                                             tmpdir.getAbsolutePath(), e.getMessage()));
        }
    }

    /**
     * Create a file in temp folder
     *
     * @param fileName the file name
     * @return the temp file path
     */
    public static File tempFile(String fileName) {
        return new File(tempFolder(), fileName);
    }

    /**
     * Generate a temp folder(created)
     *
     * @return the temp folder
     */
    public static File tempFolder() {
        String folderPath = System.getProperty("java.io.tmpdir");
        File tmpdir = new File(folderPath, "net.happyonroad.spring");
        File folder = new File(tmpdir, generateSeed());
        if (folder.exists()) {
            try {
                FileUtils.deleteDirectory(folder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!folder.mkdirs()) {
            throw new UnsupportedOperationException(format("Can't make temp folder: %s", folderPath));
        }
        return folder;
    }

    private static synchronized String generateSeed() {
        return UUID.randomUUID().toString();
    }
}
