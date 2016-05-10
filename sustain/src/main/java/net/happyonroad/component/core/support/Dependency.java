/**
 * @author XiongJie, Date: 13-8-29
 */
package net.happyonroad.component.core.support;

import net.happyonroad.component.core.Component;
import net.happyonroad.component.core.ComponentVersion;
import net.happyonroad.component.core.Versionize;
import net.happyonroad.component.core.exception.InvalidComponentNameException;
import net.happyonroad.component.core.exception.InvalidVersionSpecificationException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 版本依赖
 */
public class Dependency implements Versionize{

    private static Pattern digitPattern        = Pattern.compile("(^\\d+)(.+)?");
    private static Pattern rangeFeaturePattern = Pattern.compile("[,||\\(|\\)|\\[\\]]+");

    private String groupId;

    private String artifactId;

    /*package*/ String version;//核心版本，如: 1.0.0, for test

    private VersionRange range;//版本范围，如 [1.0.0,)

    private String type;//pom or jar, war, rar, and so on

    private String classifier;//snapshot, release, javadoc, source, java4, java5, java6, or other feature...

    private String scope;//See Component.SCOPE_XXX

    private Boolean         optional;
    private Set<Exclusion> exclusions;

    public Dependency() {
    }

    public Dependency(String groupId, String artifactId) {
        this(groupId, artifactId, null);
    }

    public Dependency(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        setVersion(version);
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getClassifier() {
        return classifier;
    }

    public boolean hasClassifier() {
        return classifier != null && !"".equalsIgnoreCase(classifier.trim());
    }

    public String getType() {
        return type;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setClassifier(String classifier) {
        if(classifier == null || classifier.length() == 0)
            this.classifier = null;
        else
            this.classifier = classifier;
    }

    public void setVersion(String version) {
        if(version != null ){
            if(rangeFeaturePattern.matcher(version).find()){
                VersionRange range;
                try {
                    range = VersionRange.createFromVersionSpec(version);
                } catch (InvalidVersionSpecificationException e) {
                    throw new RuntimeException("Can't parse version range: " + version, e);
                }
                if( range.hasRestrictions() ){
                    this.version = null;
                    this.range = range;
                }else{
                    this.version = range.getRecommendedVersion().toString();
                }
            }else{
                if( version.contains("-") ){
                    String[] vac = version.split("-");
                    this.version = vac[0];
                    this.classifier = vac[1];
                    return;
                }
                String[] versionAndClassifier = splitClassifierFromVersion(version);
                this.version = versionAndClassifier[0];
                this.setClassifier(versionAndClassifier[1]);
            }
        }else{
            this.version = null;
        }
    }


    public boolean isOptional() {
        if(optional == null ) return false;
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public boolean isTest(){
        return Component.SCOPE_TEST.equalsIgnoreCase(getScope());
    }

    public boolean isProvided(){
        return Component.SCOPE_PROVIDED.equalsIgnoreCase(getScope());
    }

    @SuppressWarnings("UnusedDeclaration")
    public boolean isRuntime(){
        return Component.SCOPE_RUNTIME.equalsIgnoreCase(getScope());
    }

    public boolean isCompile(){
        return Component.SCOPE_COMPILE.equalsIgnoreCase(getScope()) || getScope() == null;
    }

    public Set<Exclusion> getExclusions() {
        return exclusions;
    }

    public void setExclusions(Set<Exclusion> exclusions) {
        this.exclusions = exclusions;
    }

    @Override
    public String toString() {
        String gs = groupId == null ? "<undefined>" : groupId;
        String as = artifactId == null ? "<undefined>" : artifactId;
        String vs = version != null && version.length() > 0 ? "@" + version : null;
        if(vs == null ){
            if( range != null )
                vs = "@" + range.toString();
            else
                vs = "@<undefined>";
        }
        String cs = classifier != null && classifier.length() > 0 ? "-" + classifier : "";
        String ts = type != null && type.length() > 0 ? "." + type : "";
        return String.format("%s/%s%s%s%s", gs, as, vs, cs, ts);
    }


    public static Dependency parse(File file) throws InvalidComponentNameException {
        String fileName = FilenameUtils.getName(file.getAbsolutePath());
        String type = FilenameUtils.getExtension(fileName);
        String baseName;
        if( "pom".equalsIgnoreCase(type) || "jar".equalsIgnoreCase(type) ){
            baseName = FilenameUtils.getBaseName(fileName);
        }else{
            baseName = fileName;
            type = null;
        }
        if( !baseName.contains("@") )
            throw new InvalidComponentNameException("The artifact file should be named as artifactId@version, " +
                                                    "instead of " + file.getPath());
        String groupId = FilenameUtils.getName(file.getParent());
        String artifactId = StringUtils.substringBefore(baseName, "@");
        String version = StringUtils.substringAfter(baseName, "@");
        String classifier = null;
        String[] versionAndClassifier = splitClassifierFromVersion(version);
        if( versionAndClassifier.length > 1 ){
            version = versionAndClassifier[0];
            classifier = versionAndClassifier[1];
        }
        Dependency dependency = new Dependency(groupId, artifactId, version);
        dependency.setClassifier(classifier);
        dependency.setType(type);
        return dependency;
    }
    /**
     * 根据文件全名称解析依赖信息
     *
     * @param fullName 全名称，形如： groupId/artifactId@version.type
     * @return 依赖信息
     */
    public static Dependency parse(String fullName) throws InvalidComponentNameException {
        return parse(new File(fullName));
    }

    private static void removeLastChar(StringBuilder string) {
        if (string.length() > 0)
            string.deleteCharAt(string.length() - 1);
    }

    static String[] splitClassifierFromVersion(String version) {
        StringBuilder classifier = new StringBuilder();
        // 分离version中的classifier，注意，version并不总是 1.0.0这样，可能：
        //   1.1 -> 1.1.0
        //   甚至1.1.2.3这种形式
        //   我认为，暂时不应该有简化到只有一个数字的版本号，如：1
        String[] versions = version.split("-");
        if( versions.length > 1 ) {
            String[] again = splitClassifierFromVersion(versions[0]);
            if( again.length > 1 && StringUtils.isNotBlank(again[1]) ){
                return new String[]{again[0], again[1] + "-" + join(versions, 1, "-")};
            }else{
                return new String[]{versions[0], join(versions, 1, "-")};
            }
        }
        versions = version.split("\\.");
        StringBuilder pureVersion = new StringBuilder();
        int i = 0;
        for (; i < versions.length; i++) {
            String v = versions[i];
            Matcher dm = digitPattern.matcher(v);
            if (dm.matches()) {
                pureVersion.append(dm.group(1)).append(".");
                String closingClassifier = dm.group(2);
                if(closingClassifier != null){
                    if(closingClassifier.startsWith("-")){
                        closingClassifier = closingClassifier.substring(1, closingClassifier.length());
                    }
                    if(classifier.length() > 0 ){
                        classifier.insert(0, closingClassifier + "-");
                    }else{
                        classifier.append(closingClassifier);
                    }
                    i = versions.length;
                    break;
                }
            } else {
                break;
            }
        }
        removeLastChar(pureVersion);
        // pureVersion = 1.0.0
        StringBuilder classifierInVersion = new StringBuilder();
        for (; i < versions.length; i++) {
            String c = versions[i];
            classifierInVersion.append(c).append(".");
        }
        removeLastChar(classifierInVersion);
        if (classifierInVersion.length() > 0) {
            boolean separate = (classifier.length() > 0);
            classifier.insert(0, classifierInVersion);
            if (separate)
                classifier.insert(classifierInVersion.length(), "-");
        }
        return new String[]{pureVersion.toString(), classifier.toString()};
    }

    private static String join(String[] versions, int start, String sep) {
        StringBuilder sb = new StringBuilder();
        for(int i=start;i<versions.length;i++)
        {
            sb.append(versions[i]).append("-");
        }
        removeLastChar(sb);
        return sb.toString();
    }

    /**
     * 判断某个组件是否满足特定依赖信息
     * <pre>
     * 如， dependency1 = {groupId: cn.happyonroad, artifactId: component}
     *     dependency2 = {groupId: cn.happyonroad, artifactId: component, version: 1.0.0}
     *     那么: dependency1.accept(dependency2) = true
     *     反之: dependency2.accept(dependency1) = false
     * </pre>
     * @param componentOrDependency 被判断的组件或者其他依赖
     * @return 是否依赖
     */
    public boolean accept(Versionize componentOrDependency) {
        boolean accept = getGroupId().equals(componentOrDependency.getGroupId()) &&
                         getArtifactId().equals(componentOrDependency.getArtifactId());
        if (!accept) return false;
        if (getVersion() != null) {
            //有version需要，暂时不知道是否需要支持version的表达式，maven自身好像没有这个机制
            accept = getVersion().equals(componentOrDependency.getVersion());
            if (!accept) return false;
        }
        if( range != null ){
            // Temp solution
            if( componentOrDependency.getVersion() == null ) return true;
            accept = range.containsVersion(new ComponentVersion(componentOrDependency.getVersion()));
            if (!accept) return false;
        }
/*
        if( getClassifier() != null ){
            accept = getClassifier().equals(componentOrDependency.getClassifier());
            if (!accept) return false;
        }
*/
/*
        if( getType() != null ){
            accept = getType().equals(componentOrDependency.getType());
            if (!accept) return false;
        }
*/
        return true;
    }

    public boolean exclude(Versionize componentOrDependency){
        if( exclusions == null || exclusions.isEmpty()) return false;
        for (Exclusion exclusion : exclusions) {
            if(exclusion.cover(componentOrDependency)) return true;
        }
        return false;
    }

    public boolean conflict(Dependency dependency){
        return getGroupId().equals(dependency.getGroupId()) &&
                getArtifactId().equals(dependency.getArtifactId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Dependency)) {
            return false;
        }

        Dependency that = (Dependency) o;

        if (!artifactId.equals(that.artifactId)) {
            return false;
        }
        if (!groupId.equals(that.groupId)) {
            return false;
        }
        //noinspection RedundantIfStatement
        if (version != null ? !version.equals(that.version) : that.version != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = groupId.hashCode();
        result = 31 * result + artifactId.hashCode();
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }

    /**
     * 为某个依赖补齐信息
     * @param parentDefined 父组件中定义的依赖信息
     */
    public void merge(Dependency parentDefined) {
        if(optional == null ){
            setOptional(parentDefined.isOptional());
        }
        if(version == null){
            setVersion(parentDefined.getVersion());
        }
        if(classifier == null ){
            setClassifier(parentDefined.getClassifier());
        }
        if(scope == null ){
            setScope(parentDefined.getScope());
        }
        if(exclusions == null ){
            setExclusions(parentDefined.getExclusions());
        }
    }

    /**
     * 根据组件的上下文，替换依赖中的信息
     * @param component 所属的组件
     */
    public void interpolate(DefaultComponent component) {
        if(groupId != null) this.groupId = component.interpolate(groupId);
        if(artifactId != null) this.artifactId = component.interpolate(artifactId);
        //暂时主要处理一些启动相关的关键属性，如version
        if(version != null ) this.setVersion(component.interpolate(version)) ;
        if(classifier != null ) this.classifier = component.interpolate(classifier);
        if(scope != null ) this.scope = component.interpolate(scope);
    }


    public boolean hasExclusions() {
        return exclusions != null && !exclusions.isEmpty();
    }

    public void exclude(Collection<Exclusion> exclusions) {
        if( exclusions == null ) return;
        if( this.exclusions == null ) this.exclusions = new HashSet<Exclusion>();
        this.exclusions.addAll(exclusions);
    }
}
