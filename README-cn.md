spring-component-framework
==========================

1. 使用场景
-----------

Spring component framework 是一个基于SpringFramework和Maven的组件化的微内核Java独立程序框架(规划3.0.0版本将支持Web应用）。

它能帮助你将应用程序切割成为独立的小块（一个Jar包就是一个模块），且对你的应用程序**完全**没有任何侵入性。
不需要像OSGi那样，需要实现BundleContext接口，了解MANEFEST.MF里面一堆Bundle-*语义

在此之外，它还可以辅助你打包应用程序，并且在Maven的支持下，保持你的应用程序中在开发态与运行态的一致性。

在阅读以下介绍时，你可以下载并参考完整的 [示例程序](https://github.com/Kadvin/spring-component-example)

2. 使用方式
----------

### 2.1 普通应用程序

假设开发一个分布式程序，包括服务器端(router)和分布式工作端(worker)两个部分。

```
              +---------+           +--------+
              | router  |           | worker |
   Caller --> |  |-basis|       (n) |  |     |
              |  |-api  |<---RMI--->|  |-api |
              +---------+           +--------+

```

服务器(router)和工作端(worker)部署在不同的进程空间，相互之间通过RMI访问，共同遵守api中定义的接口。

服务器比客户端额外多一个basis模块，用于提供存储/缓存功能。

调用者(Caller)可以被视为对系统外部程序的一种模拟，通过RMI发起对Router的调用。

我们可以将程序分为如下几个模块：

```XML
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <packaging>pom</packaging>

    <groupId>com.myapp</groupId>
    <artifactId>spring-component-example</artifactId>
    <version>2.2.0</version>
    <name>My App</name>
    <modules>
        <module>api</module>
        <module>caller</module>
        <module>router</module>
        <module>basis</module>
        <module>worker</module>
    </modules>
</project>
```

#### 1. com.myapp.api

  我们将模块之间的契约作为公开API全部定义在本模块中.

```java
  //all below API is in this package;
  package com.myapp.api;

  /**
   * Router的API，被客户端或者外部调用者调用
   */
  public interface RouteAPI{
    /**
     * A service export to worker to register
     */
    String register(String workerId, String address);

    /**
     * Receive some job assigned by outer system
     * and the router will pick a worker to perform the job, cache the result.
     */
    Object perform(String job);
  }
```

```java
  /**
   * Worker API，被Router调用
   */
  public interface WorkAPI{
    /**
     * A service export to router to be assigned with some job
     */
    Object perform(String job);
  }

```

```java
  /**
   * 缓存服务，被Router内部模块调用
   */
  public interface CacheService{
    boolean store(String key, Object value);
    Object pick(String key);
  }
```

API项目的Maven Pom定义文件大致如下:

```XML
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.myapp</groupId>
        <artifactId>spring-component-example</artifactId>
        <version>2.2.0</version>
    </parent>
    <artifactId>api</artifactId>
    <name>My App API</name>
</project>
```

#### 2. com.myapp.worker

  Worker可能在多台机器中被启动多个实例，伪码如下：

```java
  package com.myapp.work;

  @org.springframework.stereotype.Component
  class Worker implements WorkAPI{
    public Object perform(String job){
      //do some real staff
      //and return the result;
    }
  }
```

客户端的Maven Pom大致如下:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.myapp</groupId>
        <artifactId>spring-component-example</artifactId>
        <version>2.2.0</version>
    </parent>
    <artifactId>worker</artifactId>
    <name>My App Worker</name>
    <dependencies>
      <dependency>
        <groupId>com.myapp</groupId>
        <artifactId>api</artifactId>
        <version>${project.version}</version>
      </dependency>
    </dependencies>
</project>
```

#### 3. com.myapp.basis

  Basis被Router模块依赖，为其提供简单的缓存服务

```java
  package com.myapp.basis;

  @org.springframework.stereotype.Component
  class CacheServiceImpl implements CacheService{
    private Map<String, Object> store = new HashMap<String,Object>();
    public boolean store(String key, Object value){
      store.put(key, value);
      return true;
    }

    public Object pick(String key){
      return store.get(key);
    }
  }
```

Basis模块的Pom大致如下:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.myapp</groupId>
        <artifactId>spring-component-example</artifactId>
        <version>2.2.0</version>
    </parent>
    <artifactId>basis</artifactId>
    <name>My App Basis</name>
    <dependencies>
      <dependency>
        <groupId>com.myapp</groupId>
        <artifactId>api</artifactId>
        <version>${project.version}</version>
      </dependency>
    </dependencies>
</project>
```

#### 4. com.myapp.router

  Router模块依赖Basis模块。

```java
  package com.myapp.route;

@org.springframework.stereotype.Component
public class Router implements RouteAPI {
    @Autowired
    private CacheService cacheService;

    @Value("${worker.port}")
    int workerPort;

    private Map<String, WorkAPI> workers = new HashMap<String, WorkAPI>();

    public String register(String workerId, String address) {
        RmiProxyFactoryBean factoryBean = new RmiProxyFactoryBean();
        factoryBean.setServiceInterface(WorkAPI.class);
        factoryBean.setServiceUrl(String.format("rmi://%s:%s/worker", address, workerPort));
        factoryBean.afterPropertiesSet();
        WorkAPI worker = (WorkAPI) factoryBean.getObject();
        String token = UUID.randomUUID().toString();
        workers.put(token, worker);
        System.out.println(String.format("A worker(%s) at %s registered, and assigned with token(%s)",
                                         workerId, address, token));
        return token;
    }

    public Object perform(String job){
        // Reused cached result first
        Object result = cacheService.pick(job);
        if( result != null )
        {
            System.out.println(String.format("Return cached job(%s) with effort %s", job, result));
            return result;
        }
        // pick a worker to perform the job if no cached result
        WorkAPI worker = randomPick();
        if( worker == null )
            throw new IllegalStateException("There is no worker available to perform the job: " + job);
        result = worker.perform(job);
        // store the result to be reused later
        cacheService.store(job, result);
        System.out.println(String.format("Worker perform job(%s) with effort %s", job, result));
        return result;
    }

    private WorkAPI randomPick() {
        int max = workers.size();
        int randIndex = new Random().nextInt(max);
        return (WorkAPI) workers.values().toArray()[randIndex];
    }
}
```

Router模块的Pom大致如下:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.myapp</groupId>
        <artifactId>spring-component-example</artifactId>
        <version>2.2.0</version>
    </parent>
    <artifactId>router</artifactId>
    <name>My App Router</name>
    <dependencies>
      <dependency>
        <groupId>com.myapp</groupId>
        <artifactId>api</artifactId>
        <version>${project.version}</version>
      </dependency>
      <dependency>
        <groupId>com.myapp</groupId>
        <artifactId>basis</artifactId>
        <version>${project.version}</version>
      </dependency>
    </dependencies>
</project>
```

#### 4. com.myapp.caller

  一个命令行程序，支持用户通过命令行调用服务器的API

```java
/** Accept test caller */
public class CLI {

    /**
     * java -Drouter.port=1097 -Drouter.address=localhost -jar path/to/com.myapp.caller-1.0.0.jar jobId
     *
     * @param args jobId(mandatory)
     */
    public static void main(String[] args) {
        if (args.length < 1)
            throw new IllegalArgumentException("You must specify a job id");
        String jobId = args[0];
        RmiProxyFactoryBean factoryBean = new RmiProxyFactoryBean();
        factoryBean.setServiceInterface(RouteAPI.class);
        factoryBean.setServiceUrl(String.format("rmi://%s:%s/router",
                                                System.getProperty("router.address", "localhost"),
                                                System.getProperty("router.port", "1097")));
        factoryBean.afterPropertiesSet();
        RouteAPI router = (RouteAPI) factoryBean.getObject();
        Object result = router.perform(jobId);
        System.out.println("Got router response: " + result);
    }
}
```

相应的POM文件大致如下：

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.myapp</groupId>
        <artifactId>spring-component-example</artifactId>
        <version>2.2.0</version>
    </parent>
    <artifactId>caller</artifactId>
    <name>My App Caller</name>
    <dependencies>
        <dependency>
            <groupId>com.myapp</groupId>
            <artifactId>api</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-context</artifactId>
            <version>${version.springframework}</version>
        </dependency>
    </dependencies>
</project>
```

该项目实际将会被打包成为一个uberjar，用户可以直接 java -jar path/to/caller.jar job 方式调用

### 2.2 组件化介绍

#### 1. 静态组件

  由于API项目在运行时没有主动创建/管理任何Java对象实例，它仅仅是提供一些接口/静态函数，常量给其他模块使用

  所以我们视其为 **静态** 组件。
  
  静态组件包只需要按照Maven规范进行打包，将pom.xml文件放到META-INF/$groupId/$artifactId目录下，成为如下格式：

```
  path/to/com.myapp.api-2.2.0.jar!
    |-META-INF
    |  |-MANIFEST.MF               # 一般性打包工具生成
    |  |-com.myapp
    |  |  |-api
    |  |    |-pom.xml              # Maven打包时会自动加上，组件标识
    |-com
    |  |-myapp
    |  |  |-api
    |  |  |  |-CacheService.class
    |  |  |  |-RouteAPI.class
    |  |  |  |-WorkAPI.class
```

Spring Component Framework在运行时，会根据pom.xml文件的定义，为其解析相关依赖，并将其作为library放在应用程序的classpath中。

#### 2. 应用组件

示例的Worker是作为一个独立的运行时程序运行，它通过RMI暴露服务给服务器调用（而不是进程内依赖）。

我们将其定义为 **应用** 组件

开发者有两种配置方式，强烈推荐采用第一种Annotation方式

##### 2.1 基于Annotation

1. 通过maven-jar-plugin，在pom.xml中声明如下配置
```
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <configuration>
                    <archive>
                        <manifestEntries>
                            <App-Config>com.myapp.WorkerAppConfig</App-Config>
                        </manifestEntries>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>
```

2. 以便在最终jar包得Manifest里面有App-Config指令：
```
App-Config: com.myapp.WorkerAppConfig
```

3. 相应的WorkerAppConfig内容为：

```JAVA
@Configuration
@ComponentScan("com.myapp.work")
public class WorkerAppConfig {

    @Autowired
    WorkAPI workAPI;
    @Value("${app.port}")
    int workerPort;

    @Bean
    public RmiServiceExporter workAPIExporter() {
        RmiServiceExporter exporter = new RmiServiceExporter();
        exporter.setServiceInterface(WorkAPI.class);
        exporter.setServiceName("worker");
        exporter.setRegistryPort(workerPort);
        exporter.setService(workAPI);
        return exporter;
    }
}
```

打包之后的文件形如：

```
  path/to/com.myapp/worker@2.2.0.jar!
    |-META-INF
    |  |-MANIFEST.MF               # 其中包括 App-Config指令
    |  |-com.myapp
    |  |  |-worker
    |  |    |-pom.xml              # 组件标识
    |-com
    |  |-myapp
    |  |  |-work
    |  |  |  |-Worker.class
    |  |  |-WorkerAppConfig.class
```

Spring Component Framework在运行时加载该jar时，会根据WorkerAppConfig 创建一个Spring Context，并与该组件关联起来。

##### 2.2 基于XML

1. 在组件的META-INF目录下提供一个application.xml，用Spring Context对这些Bean加以管理。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans.xsd
       http://www.springframework.org/schema/context
       http://www.springframework.org/schema/context/spring-context.xsd">
    <import resource="properties.xml"/>
    <context:component-scan base-package="com.myapp.work"/>
    <bean name="workerExporter" class="org.springframework.remoting.rmi.RmiServiceExporter">
      <property name="serviceInterface" value="com.myapp.api.WorkAPI"/>
      <property name="serviceName" value="worker"/>
      <property name="registryPort" value="${worker.port}"/>
      <property name="service" ref="worker"/>
    </bean>
</beans>
```

2. 打包之后的jar内容如下：

```
  path/to/com.myapp/worker@2.2.0.jar!
    |-META-INF
    |  |-MANIFEST.MF               # 一般性打包工具生成
    |  |-application.xml           # 相应Spring Context的Resource
    |  |-com.myapp
    |  |  |-worker
    |  |    |-pom.xml              # Maven打包时会自动加上，组件标识
    |-com
    |  |-myapp
    |  |  |-work
    |  |  |  |-Worker.class
```


#### 3. 服务组件(扮演服务提供者角色)

Basis模块在运行时需要创建一个CacheServiceImpl实例，而且还需要将其 **暴露** 给其他模块使用。

我们将其视为 **服务** 组件

##### 3.1 Annotation配置方式

1. 通过maven-jar-plugin，在pom.xml中声明如下配置

```
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <configuration>
                    <archive>
                        <manifestEntries>
                            <App-Config>com.myapp.BasisAppConfig</App-Config>
                        </manifestEntries>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>
```

2. 以便在最终jar包得Manifest里面有App-Config指令：

```
App-Config: com.myapp.BasisAppConfig
```

3. BasisAppConfig的内容

开发者需要在其 BasisAppConfig 里面将 cache service暴露到服务注册表，为此，需要extends AbstractAppConfig
并继承 doExports方法，通过exports方法将服务暴露出去

```java
/**
 * Basis App Config
 */
@Configuration
@ComponentScan("com.myapp.basis")
public class BasisAppConfig extends AbstractAppConfig{
    @Override
    protected void doExports() {
        exports(CacheService.class);
    }
}
```

4. 最终打包成为的形态：

```
  path/to/com.myapp/basis@2.2.0.jar!
    |-META-INF
    |  |-MANIFEST.MF               # 包括了 App-Config 指令
    |  |-com.myapp
    |  |  |-basis
    |  |  |  |-pom.xml             # 组件标识
    |-com
    |  |-myapp
    |  |  |-basis
    |  |  |  |-CacheServiceImpl.class
    |  |  |-BasisAppConfig.class
```


##### 3.2 XML配置方式

1. 相应application.xml 内容大致如下
(特别注意：当前并未实现对service:export的解析):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans.xsd
       http://www.springframework.org/schema/context
       http://www.springframework.org/schema/context/spring-context.xsd">
    <context:component-scan base-package="com.myapp.basis"/>
    <service:export role="com.myapp.CacheService"/>
    <!-- optional: hint="*" ref="cacheServiceImpl"/> -->
</beans>
```

2. 最终打包成为的形态：

```
  path/to/com.myapp/basis@2.2.0.jar!
    |-META-INF
    |  |-MANIFEST.MF
    |  |-com.myapp
    |  |  |-basis
    |  |  |  |-pom.xml             # 组件标识
    |  |-application.xml           # 应用组件标识
    |-com
    |  |-myapp
    |  |  |-basis
    |  |  |  |-CacheServiceImpl.class
```

#### 4. 服务组件(服务的使用者)

示例程序的Router也是一个 **服务** 组件，它不仅仅需要创建一个Router实例，还需要依赖Basis提供的Cache服务。

### 4.1 Annotation方式

1. 通过maven-jar-plugin，在pom.xml中声明如下配置

```
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <configuration>
                    <archive>
                        <manifestEntries>
                            <App-Config>com.myapp.RouterAppConfig</App-Config>
                        </manifestEntries>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>
```

2. 以便在最终jar包得Manifest里面有App-Config指令：

```
App-Config: com.myapp.RouterAppConfig
```

3. RouterAppConfig的内容如下：

```java
@Configuration
@ComponentScan("com.myapp.route")
@ImportResource("classpath:META-INF/properties.xml")
public class RouterAppConfig extends AbstractAppConfig {
    @Autowired
    RouteAPI routeAPI;
    @Value("${app.port}")
    int routerPort;

    @Bean
    CacheService cacheService(){
      return imports(CacheService.class);
    }

    @Bean
    public RmiServiceExporter workAPIExporter() {
        RmiServiceExporter exporter = new RmiServiceExporter();
        exporter.setServiceInterface(RouteAPI.class);
        exporter.setServiceName("router");
        exporter.setRegistryPort(routerPort);
        exporter.setService(routeAPI);
        return exporter;
    }
}
```

4. 最终打包的结果如下：

```
  path/to/com.myapp/router@2.2.0.jar!
    |-META-INF
    |  |-MANIFEST.MF               # App-Config: com.myapp.router.RouterAppConfig
    |  |-com.myapp
    |  |  |-router
    |  |  |  |-pom.xml             # 组件标识
    |-com
    |  |-myapp
    |  |  |-route
    |  |  |  |-Router.class
    |  |  |-RouterAppConfig.class
```

### 4.2 XML方式

1. 其内部的application.xml大致如下
(特别注意：当前并未实现对service:export的解析):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans.xsd
       http://www.springframework.org/schema/context
       http://www.springframework.org/schema/context/spring-context.xsd">
    <context:component-scan base-package="com.myapp.router"/>
    <service:import role="com.myapp.CacheService"/>
    <bean name="routerExporter" class="org.springframework.remoting.rmi.RmiServiceExporter">
        <property name="serviceInterface" value="com.myapp.api.RouteAPI"/>
        <property name="serviceName" value="router"/>
        <property name="registryPort" value="2000"/>
        <property name="service" ref="router"/>
    </bean>
</beans>
```

2. 需要被打包成如下格式(XML Based):

```
  path/to/com.myapp/router@2.2.0.jar!
    |-META-INF
    |  |-MANIFEST.MF
    |  |-com.myapp
    |  |  |-router
    |  |  |  |-pom.xml             # 组件标识
    |  |-application.xml           # 应用组件标识
    |-com
    |  |-myapp
    |  |  |-route
    |  |  |  |-Router.class
```

#### 5. 组件开发规范

大多数情况，一个服务组件，既会引用其他组件提供的服务，也可能暴露一些服务给别的组件。
当组件越来越多时，开发者可能发现，难以维护组件与组件之间的服务import/export关系；
为了解决该问题，设定如下的组件开发规范：

1. 组件的内部实现类应该尽量采用package visible（隔离，断了其他使用者直接构建相关实例的念想）
2. 组件的尽量采用Annotation方式开发
3. 组件的内部App-Config一般取名为: XxxAppConfig，并与组件的内容包名同级，如：

```
    |-com
    |  |-myapp
    |  |  |-basis
    |  |  |  |-CacheServiceImpl.class
    |  |  |-BasisAppConfig.class
```
4. 对于会被别人依赖/导入的组件，一般应该再提供一个XxxUserConfig，如：

```
    |-com
    |  |-myapp
    |  |  |-basis
    |  |  |  |-CacheServiceImpl.class
    |  |  |-BasisAppConfig.class
    |  |  |-BasisUserConfig.class
```

BasisUserConfig的内容如下：

```java
@Configuration
public class BasisUserConfig extends AbstractUserConfig{
    @Bean
    CacheService cacheService(){
        return imports(CacheService.class);
    }
}
```

5. 相应依赖其的AppConfig可以使用Spring的@Import语义

 如RouterAppConfig可以被改写为：

```java
@Configuration
@ComponentScan("com.myapp.route")
@Import(BasisUserConfig.class)
public class RouterAppConfig extends AbstractAppConfig {
    @Autowired
    RouteAPI routeAPI;

    @Bean
    public RmiServiceExporter workAPIExporter() {
        RmiServiceExporter exporter = new RmiServiceExporter();
        exporter.setServiceInterface(RouteAPI.class);
        exporter.setServiceName("router");
        exporter.setServicePort(2000);
        exporter.setService(routeAPI);
        return exporter;
    }
}
```


### 2.3 项目发布

#### 1. 手工发布

示例的应用程序发布需要遵循一些约束与规范：

假设示例服务发布的目录是: path/to/router

  1. 所有该运行时实际要用到的jar包（包括 com.myapp.*, 第三方包)都应该被放到lib目录下
  2. 所有的包文件名需要符合格式: $groupId/$artifactId@$version.jar
  3. 对于第三方包，其内部没有按照我们的规范内嵌 pom.xml， 我们需要将其pom.xml解析出来，放在 lib/poms.jar中
  4. Spring Component Framework 的Jar包应该被单独放在 boot 目录

```
  path/to/router
    |  |-boot
    |  |  |-net.happyonroad.spring-component-framework-2.2.0.jar
    |  |-lib
    |  |  |-com.myapp
    |  |  |  |-router@2.2.0.jar
    |  |  |  |-api@2.2.0.jar
    |  |  |  |-basis@2.2.0.jar
    |  |  |-org.springframework
    |  |  |  |-spring-beans@3.2.4.RELEASE.jar
    |  |  |-<other depended jars>
    |  |  |-poms.jar
```

按照如下格式部署之后，我们就可以使用如下命令行启动应用程序：

```
  cd path/to/router
  java -jar boot/spring-component-framework@2.2.0.jar com.myapp/router@2.2.0
```
最后一个参数就是告诉Spring Component Framework，哪里是程序的入口，也就是从哪里开始加载jar包。

启动后，你将会看到如下输出：

```
Listening for transport dt_socket at address: 5004
27 15:20:46.191 [main] INFO  AppLauncher               - ******* Loading components starts from com.myapp/router@2.2.0.jar **********************************
2015-02-27 15:20:46,191 [main] INFO  - ******* Loading components starts from com.myapp/router@2.2.0.jar **********************************
27 15:20:46.866 [main] INFO  AppLauncher               - ******* Container starts took 0:00:00.672 **********************************************************
2015-02-27 15:20:46,866 [main] INFO  - ******* Container starts took 0:00:00.672 **********************************************************
27 15:20:46.866 [main] INFO  AppLauncher               - ******* Loaded  components starts from com.myapp/router@2.2.0.jar **********************************
2015-02-27 15:20:46,866 [main] INFO  - ******* Loaded  components starts from com.myapp/router@2.2.0.jar **********************************
27 15:20:46.884 [main] INFO  AppLauncher               - ******* Export Executable Service at rmi://localhost:1097/My_App_RouterLauncher ********************
2015-02-27 15:20:46,884 [main] INFO  - ******* Export Executable Service at rmi://localhost:1097/My_App_RouterLauncher ********************
27 15:20:46.885 [main] INFO  AppLauncher               - ******* The My_App_Router is started ***************************************************************
2015-02-27 15:20:46,885 [main] INFO  - ******* The My_App_Router is started ***************************************************************
27 15:20:46.885 [main] INFO  AppLauncher               - ******* System starts took 0:00:00.696 *************************************************************
2015-02-27 15:20:46,885 [main] INFO  - ******* System starts took 0:00:00.696 *******************************************************```

#### 2. 自动发布应用

我们需要在实际运行时的主模块pom文件中声明对 spring-component-framework 的依赖

```xml
<dependencies>
  <dependency>
    <groupId>net.happyonroad</groupId>
    <artifactId>spring-component-framework</artifactId>
    <version>0.1.0</version>
  </dependency>
</dependencies>
```

我们可以在示例程序 worker，router中增加一个定制化的Maven插件来完成以上描述的复杂过程：

需要更多信息，请参考该插件的 [详细说明](https://github.com/Kadvin/spring-component-builder)

```xml
  <build>
    <plugins>
      <plugin>
        <groupId>net.happyonroad</groupId>
        <artifactId>spring-component-builder</artifactId>
        <version>2.2.0-SNAPSHOT</version>
        <executions>

          <execution>
            <id>index-detail</id>
            <goals><goal>detail</goal></goals>
          </execution>

          <execution>
            <id>package-app</id>
            <goals><goal>package</goal></goals>
            <jvmOptions>-Dapp.prefix=com.myapp</jvmOptions>
            <configuration>
              <target>path/to/${project.artifactId}</target>
            </configuration>
          </execution>

          <execution>
            <id>clean-app</id>
            <goals><goal>clean</goal></goals>
            <configuration>
              <target>path/to/${project.artifactId}</target>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
```

该插件三个任务默认在maven的package/process-classes/clean阶段工作，作用分别为索引/打包/清除

1. package

| 参数          | 作用                             |
|--------------|---------------------------------|
| target       | 输出目录                         |
| extensionPath| 扩展目录                         |
| appName      | 设定本应用的控制台/日志显示名称      |
| appPort      | 设定本应用所绑定的RMI端口，缺省1099 |
| jvmOptions   | 设定本应用启动时的JVM选项          |
| debug        | 调试端口号，如果设置了，最终通过修改jvmOptions实现 |
| jmx          | JMX端口号，如果设置了，最终通过修改jvmOptions实现 |
| properties   | 设定本应用的config/${app.name}.properties内容|
| propertyFile | 直接设定config/${app.name}.properties文件|
| logbackFile  | 直接设定config/logback.xml文件，如果不设置，将会自动生成一个|
| folders      | 从当前项目中copy哪些目录到最终输出目录 |
| files        | 从当前项目中copy哪些文件到最终输出目录 |
| frontendNodeModules | 前端npm项目缓存位置          |
| appPrefix    | 标记如何识别应用组件，默认为net.happyonroad;dnt开头 |
| wrapper      | 是否生成Java Service Wrapper       |

2. index-detail

没有什么参数需要设置

3. clean

| 参数          | 作用                            |
|--------------|---------------------------------|
| target       | 需要清理的输出目录                 |

4. extend

| 参数              | 作用                            |
|------------------|---------------------------------|
| target           | 输出目录                         |
| extensionPath    | 扩展目录                         |
| copyDependencies | 是否copy该扩展引入的依赖           |

5. unextend

| 参数          | 作用                            |
|--------------|---------------------------------|
| target       | 需要清理的输出目录                 |

当你在示例程序(worker/router)的根目录执行:

```bash
mvn package
```

我们将会看到如下输出:

```
  path/to/router
    |  |-bin
    |  |  |-start.bat
    |  |  |-stop.bat
    |  |  |-start.sh
    |  |  |-stop.sh
    |  |-config
    |  |  |-logback.xml
    |  |-boot
    |  |  |-spring-component-framework@2.2.0.jar
    |  |-lib
    |  |  |-com.myapp
    |  |  |  |-router@2.2.0.jar
    |  |  |  |-api@2.2.0.jar
    |  |  |  |-basis@2.2.0.jar
    |  |  |-org.springframework
    |  |  |  |-spring-beans@3.2.4.RELEASE.jar
    |  |  |-<other depended jars>
    |  |  |-poms.jar
    |  |-logs
    |  |-tmp
```

```
  path/to/worker
    |  |-bin
    |  |  |-start.bat
    |  |  |-stop.bat
    |  |  |-start.sh
    |  |  |-stop.sh
    |  |-config
    |  |  |-logback.xml
    |  |-boot
    |  |  |-spring-component-framework@2.2.0.jar
    |  |-lib
    |  |  |-com.myapp
    |  |  |  |-worker@2.2.0.jar
    |  |  |  |-api@2.2.0.jar
    |  |  |-org.springframework
    |  |  |  |-spring-beans@3.2.4.RELEASE.jar
    |  |  |-<other depended jars>
    |  |  |-poms.jar
    |  |-logs
    |  |-tmp

```

此时Worker与Router已经准备就绪，可以通过start(bat|sh)运行，启动后，您可以采用Caller测试一下以上程序是否可以正常工作：

```
java  -jar com.myapp.caller\@2.2.0.jar hello
```
将会看到 Router控制台输出：

```
A worker(5c0c2711-27a7-46bd-b0ce-f014cf947158) at 192.168.12.63 registered, and assigned with token(fa514ee3-eb2e-4538-ad98-17666a40d4de)
Worker perform job(hello) with effort 0fcc09c1-279a-472b-b292-f6dd48ed162a
```

Worker控制台输出：

```
Return router job hello with 0fcc09c1-279a-472b-b292-f6dd48ed162a
```

Caller控制台输出：

```
Got router response: 0fcc09c1-279a-472b-b292-f6dd48ed162a
```

3. 扩展组件
------------

### 3.1 了解扩展机制

  本组件框架并不将组件局限为仅有的 `静态组件`, `应用组件`, `服务组件` 三类，资深的应用开发者可以在系统的原有语义下定义出新的组件类型。

  为了定义新的组件类型，开发者必须了解组件的加载原理。
  
  实际上，系统加载组件时，会认为每个组件都具有`静态组件`特征，按照 maven的pom.xml指示的依赖关系构建相应的class load graph。

  而后，`应用组件` `服务组件` 都是根据被解析的组件包中是否具备相应的特征(xml或者annotation directive)，由对应的 `Feature Resolver` 进行加载/卸载。
  
  与其他特性相比，系统只是内置了Application, Service两种特性，基本的加载顺序为：
  
| order | Feature Resolver             |   内容         |
|-------|------------------------------|----------------|
| 10    | Static Feature Resolver      | 构建组件需要的class loader |
| 25    | Service Feature Resolver     | 根据 import 指令，将依赖的service对象组织到 service context中（留待可能的app context引用））；如果有子context started，根据export指令，将子context中的对象发布到服务注册表中，留待其他组件使用 |
| 30    | Application Feature Resolver | 构建组件内的app context（为 service context的子context【如果有】） |

  系统基本的卸载顺序为：
  
| order | Feature Resolver             | 内容     |  
|-------|------------------------------|---------|
| 65    | Service Feature Resolver     | 关闭service context |
| 70    | Application Feature Resolver | 关闭app context |
| 100   | Static Feature Resolver      | 卸载 class loader |
  
 增加新的扩展机制，也就包括了两个部分的内容
 
 3.2 定义扩展特征
 3.3 解析扩展特征

以及最后一个实施的过程

 3.4 自动发布扩展

### 3.2 定义扩展特征

  可以要求开发者在最终的组件中提供相应的可识别的特征，如jar文件名，jar中文件信息，以及manifest里面的指令(directive)

  这些特征应该与3.3节中Feature Resolver的 hasFeature(Component) 接口实现方法一致

### 3.3 解析扩展特征

1. 定义你的 Feature Resolver 类
2. 在系统启动时，增加 -Dcomponent.feature.resolvers=fqn1,fqn2
3. 实现相应的Feature Resolver

实现 Feature Resolver时，需要考虑到其他feature的解析顺序，建议从 `AbstractFeatureResolver` 继承，实现如下几个主要方法：

```java
    /**
     * 判断组件是否有本特性
     *
     * @param component 被判断的组件
     * @return 是否有特性
     */
    boolean hasFeature(Component component);

    /**
     * 在特定的上下文中解析相应的组件
     *
     * @param component 被解析的组件
     *
     */
    void resolve(Component component) throws Exception;

    /**
     * 在特定的上下文中卸载/释放相应的组件
     * @param component 被卸载的组件
     */
    Object release(Component component) ;
```


### 3.4 自动发布扩展 

与项目打包一样，扩展也需要被打包，并植入到目标系统中，打包的主要方式如下：

```
    <build>
        <plugins>
            <plugin>
                <groupId>net.happyonroad</groupId>
                <artifactId>spring-component-builder</artifactId>
                <version>${version.component-framework}</version>
                <executions>
                    <execution>
                        <id>extend-app</id>
                        <goals><goal>extend</goal></goals>
                        <configuration>
                            <targetRelease>${release.dir}</targetRelease>
                        </configuration>
                    </execution>
                    <execution>
                        <id>un_extend-app</id>
                        <goals><goal>un_extend</goal></goals>
                        <configuration>
                            <targetRelease>${release.dir}</targetRelease>
                        </configuration>
                    </execution>
                </executions>
            </plugin>

        </plugins>
    </build>
```

其中的 targetRelease参数需要提供目标部署系统，我们将会扩展部署到相应 ${release.dir}/repository目录

支持如下参数：

1. targetRelease: 被扩展的系统
2. extensionPath （默认值 就是 repository）: 扩展所在目录 
3. copyDependencies (默认false): 是否需要将扩展的依赖也copy过去，为了提高扩展效率，默认为false，但对于引入了自身依赖的扩展包而言，这个选项必须设置为true
 

4. 技术原理
---------------

有人会有疑问，已经有OSGi规范了，还有许多不同实现，Spring系列中，还有一个Spring DM Server，为什么还要再发明一个组件/插件框架的轮子？

更有甚者，SpringSource还在开发一个叫做 [spring-plugin](https://github.com/spring-projects/spring-plugin) 插件框架。

我个人在工作中，曾经尝试基于OSGi开发一些应用，但其实在是过于复杂，引入了太多的概念，工具，约束；我甚至连调试OSGi应用程序的勇气都没了。

在OSGi的泥潭里面挣扎时，我认识到，OSGi的复杂性，主要来源于其运行时的动态性：

而我仅仅需要一个简单，清晰的组件框架，打个比方，我仅需要制造一个正常的机器人，它可以开机，运行，而后关机。

但使用了OSGi，它试图一开始就让我制造一个更强大的机器人，它不仅仅可以开/关机，运行，还能在运行时把自己的胳膊，腿卸下来，换一个新的！

但这并不意味着基于Spring Component Framework开发的程序无法实现这种动态性；我们认为实现这种动态性是最终应用开发者的责任，我们把选择权交给最终开发者。

另外，由于一直基于Spring Framework搭建项目框架，使用Maven进行项目组织，我猜想这两个框架/工具是java项目的主流

所以，本组件框架把jar包内部的对象管理直接限定在使用Spring，而把依赖管理绑定在Maven的战车上。

在实际开发过程中，我们还参考了Maven所使用的Plexus IOC容器，甚至直接使用了其底层的Classworlds做Jar之间的Class Path管理。
(1.0.0 版本之后，为了加速系统启动，并简化结构，改写了class loader，不再使用 plexus的class loader)

关于Spring Plugin，我在开发本组件框架之前，对其进行了考察，但我发现Spring-Plugin与本项目有着不同的关注点。

我认为，spring-plugin更关注程序之间的 **连接性** ，而Spring Component Framework更关注 **隔离性** 

如下是从Spring Plugin的README中copy来的示例配置：

```xml
<beans xmlns="http://www.springframework.org/schema/beans"
  xmlns:plugin="http://www.springframework.org/schema/plugin"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
    http://www.springframework.org/schema/plugin http://www.springframework.org/schema/plugin/spring-plugin.xsd">

  <import resource="classpath*:com/acme/**/plugins.xml" />

  <bean id="host" class="com.acme.HostImpl">
    <property name="plugins" ref="plugins" />
  </bean>

  <plugin:list id="plugins" class="org.acme.MyPluginInterface" />
</beans>
```

由此可见，它的基本出发点，是用一个比较强大的 Spring Application Context，跨越多个物理Jar包的阻隔，在其内部通过
一定的标签，让对象间的按照Plugin语义进行组装，这也符合Spring IOC容器的原本定位。

而Spring Component Framework设计定位于：

 * 面向组件（一个jar就是一个组件）
 * 开发者友好（不要引入太多的概念）
 * 零侵入性，零依赖
 * 在开发态与运行态保持一致

TODO: 更多的技术阐述。