/**
 * @author XiongJie, Date: 13-9-17
 */
package net.happyonroad.component.container.support;

import net.happyonroad.component.ComponentTestSupport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * 对缺省组件仓库的测试
 * 本测试用例的定位，主要是测试其能不能很好的handle lib/*.jar, lib/poms/*.pom等里面定义的各种依赖关系
 */
public class DefaultComponentRepositoryTest extends ComponentTestSupport {

    @BeforeClass
    public static void setUpTotal() throws Exception {
        System.setProperty("app.prefix", "com.itsnow.;spring.test");
        createPom("comp_0", "spring/test@0.0.1", tempFolder);
        createJar("comp_1", "spring.test/comp_1@0.0.1", "spring/test/api", tempFolder);
        createJar("comp_2", "spring.test/comp_2@0.0.1", "spring/test/standalone", tempFolder);
        createJar("comp_3", "spring.test/comp_3@0.0.1", "spring/test/provider", tempFolder);
        createJar("comp_4", "spring.test/comp_4@0.0.1", "spring/test/user", tempFolder);
        createJar("comp_5", "spring.test/comp_5@0.0.1", "spring/test/mixed", tempFolder);
        createJar("comp_6", "spring.test/comp_6@0.0.1", "spring/test/scan_p", tempFolder, new Filter("Provider"));
        createJar("comp_7", "spring.test/comp_7@0.0.1", "spring/test/scan_u", tempFolder, new Filter("User"));
        createJar("comp_8", "spring.test/comp_8@0.0.1", "spring/test/scan_u", tempFolder, new Filter("User"));
    }

    List<File> files, sorted;
    File[] array;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        files = new ArrayList<File>(8);;
        array = new File[8];
        for (int i = 1; i <= 8; i++) {
            files.add(new File(tempFolder, "lib/spring.test/comp_" + i + "@0.0.1.jar"));
        }
        List<File> temp = new ArrayList<File>(files);
        Random random = new SecureRandom();
        int i = 0;
        while(!temp.isEmpty()){
            int pos = random.nextInt(temp.size());
            File file = temp.remove(pos);
            array[i++] = file;
        }
    }


    //包括了对sortComponents的测试
    //  3 -> 1
    //  4 -> 1,3
    //  5 -> 1,3,4
    //  7 -> 6
    //  8 -> 6
    @Test
    public void testSortCandidates() throws Exception {
        output("Before");
        repository.sortCandidates(array);
        sorted = Arrays.asList(array);
        Assert.assertTrue(indexOf(3) > indexOf(1));
        Assert.assertTrue(indexOf(4) > indexOf(3));
        Assert.assertTrue(indexOf(5) > indexOf(4));
        Assert.assertTrue(indexOf(7) > indexOf(6));
        Assert.assertTrue(indexOf(8) > indexOf(6));
        output("After");
    }

    private int indexOf(int componentNameIndex) {
        File file = files.get(componentNameIndex - 1 );
        return sorted.indexOf(file);
    }

    private void output(String phase) {
        System.out.println(phase + " Sort");
        for (File file : array) {
            System.out.println("\t" + file.getName());
        }
    }
}
