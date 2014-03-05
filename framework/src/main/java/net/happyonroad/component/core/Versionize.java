package net.happyonroad.component.core;

/**
 * 可以被版本化的对象信息，主要包括组件和依赖
 */
public interface Versionize {
    /**
     * 组标识
     *
     * @return 组id
     */
    String getGroupId();

    /**
     * 组件标识
     *
     * @return 组件标识
     */
    String getArtifactId();

    /**
     * 版本
     *
     * @return 版本
     */
    String getVersion();

    /**
     * 额外的描述或者限定
     * 我们的策略参考了Maven，但并不完全遵循Maven
     * 例如，Maven中可能将版本设置为： 1.0.0.RELEASE
     * 但我们将RELEASE视为Classifier，版本视为1.0.0
     *
     * @return 描述
     */
    String getClassifier();

    /**
     * 判断是否有限定信息
     *
     * @return 是否有
     */
    boolean hasClassifier();

    /**
     * 组件类型，一般为 jar,pom,war,ear
     * 以后可能会扩充出 探针包类型(par)，业务包类型(bar)，模型包类型(mar)，规则包类型(lar)
     *
     * @return 类型
     */
    String getType();
}
