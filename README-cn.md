spring-component-framework
==========================

1. 使用场景
-----------

Spring component framework 是一个基于SpringFramework和Maven的组件化的微内核Java独立程序框架(未来版本将支持Web应用）。

它能帮助你将应用程序切割成为独立的小块（一个Jar包就是一个模块），且对你的应用程序**完全**没有任何侵入性。
不需要像OSGi那样，需要实现BundleContext接口，了解MANEFEST.MF里面一堆Bundle-*语义

在此之外，它还可以辅助你打包应用程序，并且在Maven的支持下，保持你的应用程序中在开发态与运行态的一致性。

在阅读以下介绍时，你可以下载并参考完整的 [示例程序](https://github.com/Kadvin/spring-component-example)

2. 使用方式
----------

### 2.1 普通应用程序

假设你需要开发一个分布式程序，包括服务器端和客户端两个部分。

```
              +---------+           +--------+
              | server  |           | client |
   Caller --> |  |-basis|           |  |     |
              |  |-api  |<---RMI--->|  |-api |
              +---------+           +--------+

```

服务器和客户端部署在不同的进程空间，相互之间通过RMI访问，共同遵守api中定义的接口。

服务器比客户端额外多一个部署一个basis组件，用于为server提供存储/缓存功能。

调用者(caller)可以被视为对系统外部程序的一种模拟，通过RMI发起对Server的调用。

我们可以将程序分为如下几个模块：

```XML
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <packaging>pom</packaging>

    <groupId>com.myapp</groupId>
    <artifactId>spring-component-example</artifactId>
    <version>0.0.1</version>

    <name>My app</name>
    <modules>
        <module>api</module>
        <module>basis</module>
        <module>server</module>
        <module>client</module>
        <module>caller</module>
    </modules>
</project>
```

#### 1. com.myapp.api

  定义模块之间的API.

```java
  //all below API is in this package;
  package com.myapp.api;

  /**
   * 服务器的API，被客户端或者外部调用者调用
   */
  public interface ServerAPI{
    /**
     * A service export to client to register
     */
    String register(String clientId, String address);

    /**
     * Receive some job assigned by outer system
     * and the server will pick a client to perform the job, cache the result.
     */
    Object perform(String job);
  }
```

```java
  /**
   * 客户端API，被服务器调用
   */
  public interface ClientAPI{
    /**
     * A service export to server to be assigned with some job
     */
    Object perform(String job);
  }

```

```java
  /**
   * 缓存服务，被服务器内部模块调用
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
        <version>0.0.1</version>
    </parent>
    <artifactId>api</artifactId>
    <name>My App API</name>
</project>
```

#### 2. com.myapp.client

  客户端伪码如下：

```java
  package com.myapp.client;

  @org.springframework.stereotype.Component
  public class ClientImpl implements ClientAPI{
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
        <version>0.0.1</version>
    </parent>
    <artifactId>client</artifactId>
    <name>My App Client</name>

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

  提供简单的缓存服务，被服务器模块调用

```java
  package com.myapp.basis;

  @org.springframework.stereotype.Component
  public class CacheServiceImpl implements CacheService{
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
        <version>0.0.1</version>
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

#### 4. com.myapp.server

  Server模块依赖API项目，同时集成Basis模块。

```java
  package com.myapp.server;

  @org.springframework.stereotype.Component
  public class ServerImpl implements ServerAPI{
    @org.springframework.beans.factory.annotation.Autowired
    private CacheService cacheService;

    private Map<String, ClientAPI> clients = new HashMap<String, ClientAPI>();

    public String register(String clientId, String address) {
        RmiProxyFactoryBean factoryBean = new RmiProxyFactoryBean();
        factoryBean.setServiceInterface(ClientAPI.class);
        factoryBean.setServiceUrl(String.format("rmi://%s:%d/client", address, 1099));
        factoryBean.afterPropertiesSet();
        ClientAPI client = (ClientAPI) factoryBean.getObject();
        String token = UUID.randomUUID().toString();
        clients.put(token, client);
        return token;
    }

    public Object perform(String job){
        // Reused cached result first
        Object result = cacheService.pick(job);
        if( result != null )
            return result;
        // pick a client to perform the job if no cached result
        ClientAPI client = pickClient();
        if( client == null )
            throw new IllegalStateException("There is no client available to perform the job: " + job);
        result = client.perform(job);
        // store the result to reused latter
        cacheService.store(job, result);
        return result;
    }

    private ClientAPI pickClient() {
        //pick a client by random
        int max = clients.size();
        int randIndex = new Random().nextInt(max);
        return (ClientAPI) clients.values().toArray()[randIndex];
    }
  }
```

Server模块的Pom大致如下:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.myapp</groupId>
        <artifactId>spring-component-example</artifactId>
        <version>0.0.1</version>
    </parent>
    <artifactId>server</artifactId>
    <name>My App Server</name>

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
     * java -Dserver.port=1097 -Dserver.address=localhost -jar path/to/com.myapp.caller-0.0.1.jar jobId
     *
     * @param args jobId(mandatory)
     */
    public static void main(String[] args) {
        if (args.length < 1)
            throw new IllegalArgumentException("You must specify a job id");
        String jobId = args[0];
        RmiProxyFactoryBean factoryBean = new RmiProxyFactoryBean();
        factoryBean.setServiceInterface(ServerAPI.class);
        factoryBean.setServiceUrl(String.format("rmi://%s:%s/server",
                                                System.getProperty("server.address", "localhost"),
                                                System.getProperty("server.port", "1097")));
        factoryBean.afterPropertiesSet();
        ServerAPI server = (ServerAPI) factoryBean.getObject();
        Object result = server.perform(jobId);
        System.out.println("Got server response: " + result);
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
        <version>0.0.1</version>
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
  path/to/com.myapp.api-0.0.1.jar!
    |-META-INF
    |  |-MANIFEST.MF               # 一般性打包工具生成
    |  |-com.myapp
    |  |  |-api
    |  |    |-pom.xml              # Maven打包时会自动加上，静态组件标识
    |-com
    |  |-myapp
    |  |  |-api
    |  |  |  |-ServerAPI.class
    |  |  |  |-ClientAPI.class
    |  |  |  |-CacheService.class
```

Spring Component Framework在运行时，会根据pom.xml文件的定义，为其解析相关依赖。

#### 2. 应用组件

示例的客户端是作为一个独立的运行时程序运行，它通过RMI暴露服务给服务器调用（而不是进程内依赖）。

我们将其定义为 **应用** 组件

开发者有两种配置方式

##### 2.1 基于XML

在组件的META-INF目录下提供一个application.xml，用Spring Context对这些Bean加以管理。

打包之后的发布结构如下：

```
  path/to/com.myapp.api-0.0.1.jar!
    |-META-INF
    |  |-MANIFEST.MF               # 一般性打包工具生成
    |  |-application.xml           # 相应Spring Context的Resource
    |  |-com.myapp
    |  |  |-api
    |  |    |-pom.xml              # Maven打包时会自动加上，静态组件标识
    |-com
    |  |-myapp
    |  |  |-client
    |  |  |  |-ClientImpl.class
```

application.xml文件内容：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans.xsd
       http://www.springframework.org/schema/context
       http://www.springframework.org/schema/context/spring-context.xsd">

    <context:component-scan base-package="com.myapp.client"/>

    <bean name="clientExporter" class="org.springframework.remoting.rmi.RmiServiceExporter">
      <property name="serviceInterface" value="com.myapp.api.ClientAPI"/>
      <property name="serviceName" value="client"/>
      <property name="servicePort" value="1099"/>
      <property name="service" ref="clientImpl"/>
    </bean>
</beans>  
```

##### 2.2 基于Annotation

在Manifest里面增加App-Config指令：

```
App-Config: com.myapp.client.ClientAppConfig
```

相应的ClientAppConfig内容为：

```JAVA
  @Configuration
  @ComponentScan("com.app.client")
  public class ClientAppConfig{
    @Bean 
    public RmiServiceExporter clientExporter(){
      RmiServiceExporter exporter = new RmiServiceExporter();
      exporter.setServiceInterface(ClientAPI.class)
      exporter.setServiceName("client");
      exporter.setServicePort(1099);
      exporter.setObject(clientImpl);
      return exporter;
    }
  }
```

打包之后的文件形如：

```
  path/to/com.myapp.client-0.0.1.jar!
    |-META-INF
    |  |-MANIFEST.MF               # 其中包括 App-Config指令
    |  |-com.myapp
    |  |  |-client
    |  |    |-pom.xml              # Maven打包时会自动加上，静态组件标识
    |-com
    |  |-myapp
    |  |  |-client
    |  |  |  |-ClientImpl.class
    |  |  |  |-ClientAppConfig.class
```

Spring Component Framework在运行时加载该jar时，会根据application.xml 或者 ClientAppConfig 创建一个Spring Context，并与该组件关联起来。

#### 3. 服务组件(扮演服务提供者角色)

Basis模块在运行时需要创建一个CacheServiceImpl实例，而且还需要将其 **暴露** 给其他模块使用。

我们将其视为 **服务** 组件，它需要在application context之外，再提供一个 **service context** 

也有两种配置方式：

##### 3.1 XML配置方式

basis应该被打包成为带上service.xml的格式:

```
  path/to/com.myapp.basis-0.0.1.jar!
    |-META-INF
    |  |-MANIFEST.MF
    |  |-com.myapp
    |  |  |-basis
    |  |  |  |-pom.xml             # 静态组件标识
    |  |-application.xml           # 应用组件标识
    |  |-service.xml               # 服务组件标识
    |-com
    |  |-myapp
    |  |  |-basis
    |  |  |  |-CacheServiceImpl.class
```

其service.xml内容如下：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<service xmlns="http://www.happyonroad.net/schema/service"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://www.happyonroad.net/schema/service
       http://www.happyonroad.net/schema/service.xsd">
    <export>
        <role>com.myapp.api.CacheService</role>
    </export>
</service>
```

其应用特征可以用XML方式配置，也可以用Annotation方式配置，假设XML方式，相应application.xml 内容大致如下:

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
</beans>  
```

##### 3.2 Annotation配置方式

```
  path/to/com.myapp.basis-0.0.1.jar!
    |-META-INF
    |  |-MANIFEST.MF               # 包括了 Service-Config 指令
    |  |-com.myapp
    |  |  |-basis
    |  |  |  |-pom.xml             # 静态组件标识
    |-com
    |  |-myapp
    |  |  |-basis
    |  |  |  |-CacheServiceImpl.class
    |  |  |  |-BasicAppConfig.class
    |  |  |  |-BasicServiceConfig.class
```

假设其Application特征也以Annotation方式配置，Manifest.MF内容如下：

```
App-Config: com.myapp.basis.BasicAppConfig
Service-Config: com.myapp.basis.BasicServiceConfig
```

其中 BasicAppConfig 内容类似：

```java
  @Configuration
  @ComponentScan("com.myapp.basis")
  public class BasicAppConfig{
  }
```

BasicServiceConfig 内容如下：

```java
  public class BasicServiceConfig extends DefaultServiceConfig{
  
    public void defineServices(){
      super.defineServices();
      exportService(CacheService.class);
    }
  }
```

特别注意：  **Service Config 类不需要 用 @Configuration 标记，但需要从 DefaultServiceConfig继承**

Spring Component Framework在加载这个jar包之后，会通过某种机制，将其声明的服务 **暴露** 出去给其他服务组件使用。

#### 4. 服务组件(服务的使用者)

示例程序的服务器端也是一个 **服务** 组件，它不仅仅需要创建一个ServerImpl实例，还需要依赖Basis提供的服务。

为了导入依赖的Basis服务，它需要在service.xml里面做如下声明：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<service xmlns="http://www.happyonroad.net/schema/service"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://www.happyonroad.net/schema/service
       http://www.happyonroad.net/schema/service.xsd">
    <import>
        <role>com.myapp.api.CacheService</role>
    </import>
</service>
```

或者在其 Service Config 的 defineServices 函数里面做如下声明：

```java
  public void defineServices(){
    importService(CacheService.class);
  }
```

其内部的application.xml大致如下:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans.xsd
       http://www.springframework.org/schema/context
       http://www.springframework.org/schema/context/spring-context.xsd">

    <context:component-scan base-package="com.myapp.server"/>
</beans>  
```

或者其 App Config 写为:

```java
  @Configuration
  @ComponentScan("com.myapp.server")
  public ServerAppConfig{
  }
```

最后，它需要被打包成如下格式(XML Based):

```
  path/to/com.myapp.server-0.0.1.jar!
    |-META-INF
    |  |-MANIFEST.MF               # 可用 App-Config 指令代替 application.xml
    |  |                           # 或者 Service-Config 指令代替 service.xml
    |  |-com.myapp
    |  |  |-server
    |  |  |  |-pom.xml             # 静态组件标识
    |  |-application.xml           # 应用组件标识
    |  |-service.xml               # 服务组件标识
    |-com
    |  |-myapp
    |  |  |-server
    |  |  |  |-ServerImpl.class
```

或者(annotation based)

```
  path/to/com.myapp.server-0.0.1.jar!
    |-META-INF
    |  |-MANIFEST.MF               # App-Config: com.myapp.server.ServerAppConfig
    |  |                           # Service-Config: com.myapp.server.ServerServiceConfig
    |  |-com.myapp
    |  |  |-server
    |  |  |  |-pom.xml             # 静态组件标识
    |-com
    |  |-myapp
    |  |  |-server
    |  |  |  |-ServerImpl.class
    |  |  |  |-ServerAppConfig.class
    |  |  |  |-ServerServiceConfig.class
```

#### 5. 服务组件(混合)

大多数情况，一个服务组件，既会引用其他组件提供的服务，也可能暴露一些服务给别的组件。

我们可以将服务的依赖与导出一起定义在service.xml里面，此时组件就是一个混合式的服务组件。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<service xmlns="http://www.happyonroad.net/schema/service"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://www.happyonroad.net/schema/service
       http://www.happyonroad.net/schema/service.xsd">
    <import>
        <role>com.someapp.api.ServiceA</role>
        <hint>default</hint>
    </import>
    <import>
        <role>com.someapp.api.ServiceA</role>
        <hint>myapp</hint>
        <as>myappServiceA</as>
    </import>

    <export>
        <role>com.someapp.api.ServiceB</role>
        <hint>someapp</hint>
        <ref>someAppBeanNameOrId</ref>
    </export>
</service>
```

或者相应Service-Config指令指示的类中：

```java
 public class XxxServiceConfig extends DefaultServiceConfig{
   public void defineServices(){
     super.defineServices();
     importService(ServiceA.class, "default");
     importService(ServiceA.class, "myappServiceA");
     exportService(ServiceB.class, "someapp", "someAppBeanNameOrId")
   }
 }
```

  * 当某个组件依赖的同一接口的服务实例可能存在多个时，服务组件的导入/导出关系可以通过 `hint` 节点进行限制  

  * 当某个组件内部的Spring Context包含多个需要导出的接口的实现实例，可以通过设定 `ref` 节点来定向导出相应的bean作为服务。

  * 导入的服务，可以通过设定 `as` 元素来为其取名，而后组件内部的application context可以将其视为普通的bean，通过spring内置的 `@Qualifier` 加以限定。

### 2.3 项目发布

#### 1. 手工发布

示例的应用程序发布需要遵循一些约束与规范：

假设示例服务发布的目录是: path/to/server

  1. 所有该运行时实际要用到的jar包（包括 com.myapp.*, 第三方包)都应该被放到lib目录下
  2. 所有的包文件名需要符合格式: $groupId.$artifactId-$version.jar
  3. 对于第三方包，其内部没有按照我们的规范内嵌 pom.xml， 我们需要将其pom.xml解析出来，放在 lib/poms下
  4. Spring Component Framework 的Jar包应该被单独放在 boot 目录

```
  path/to/server
    |  |-boot
    |  |  |-net.happyonroad.spring-component-framework-0.0.1.jar
    |  |-lib
    |  |  |-com.myapp.server-0.0.1.jar
    |  |  |-com.myapp.api-0.0.1.jar
    |  |  |-com.myapp.basis-0.0.1.jar
    |  |  |-org.springframework.spring-beans-3.2.4.RELEASE.jar
    |  |  |-<other depended jars>
    |  |  |-poms
    |  |  |  |-org.springframework.spring-beans-3.2.4.RELEASE.pom
    |  |  |  |-<other depended poms>
```

按照如下格式部署之后，我们就可以使用如下命令行启动应用程序：

```
  cd path/to/server
  java -jar boot/net.happyonroad.spring-component-framework-0.0.1.jar com.myapp.server-0.0.1
```
最后一个参数就是告诉Spring Component Framework，哪里是程序的入口，也就是从哪里开始加载jar包。

启动后，你将会看到如下输出：

```
2013-12-10 17:11:26,864 [main] WARN  - app.host is not set, use localhost as default
2013-12-10 17:11:27,152 [main] INFO  - ********* Scanning jars ************************************************************************************************
2013-12-10 17:11:27,177 [main] INFO  - ********* Scanned jars *************************************************************************************************
2013-12-10 17:11:27,178 [main] INFO  - ********* Resolving starts from com.myapp.server-0.0.1 *****************************************************************
2013-12-10 17:11:27,382 [main] INFO  - ********* Resolved  starts from com.myapp.server-0.0.1 *****************************************************************
2013-12-10 17:11:27,385 [main] INFO  - ********* Configuring main realm for com.myapp.server-0.0.1.jar ********************************************************
2013-12-10 17:11:27,415 [main] INFO  - ********* Configured  main realm ClassRealm[com.myapp.server-0.0.1.jar, parent: net.happyonroad.component.classworld.Cla
2013-12-10 17:11:27,431 [main] INFO  - ********* Loading components starts from com.myapp.server-0.0.1.jar ****************************************************
2013-12-10 17:11:29,396 [main] INFO  - ********* Loaded  components starts from com.myapp.server-0.0.1.jar ****************************************************
2013-12-10 17:11:29,514 [main] INFO  - ********* Export Executable Service at rmi://localhost:1097/My_App_ServerLauncher **************************************
2013-12-10 17:11:29,515 [main] INFO  - ********* The My_App_Server is started *********************************************************************************
```

#### 2. 自动发布应用

我们需要在实际运行时的主模块pom文件中声明对 spring-component-framework 的依赖，强烈建议将该依赖类型设置为 runtime
  避免开发者在开发过程中直接使用spring-component-framework的静态API，从而引入不必要的侵入性。

```xml
<dependencies>
  <dependency>
    <groupId>net.happyonroad</groupId>
    <artifactId>spring-component-framework</artifactId>
    <version>0.1.0</version>
    <scope>runtime</scope>
  </dependency>
</dependencies>
```

我们可以在示例程序的client，server中增加一个定制化的Maven插件来完成以上描述的复杂过程：

需要更多信息，请参考该插件的 [详细说明](https://github.com/Kadvin/spring-component-builder)

```xml
  <build>
    <plugins>
      <plugin>
        <groupId>net.happyonroad</groupId>
        <artifactId>spring-component-builder</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <executions>

          <execution>
            <id>index-detail</id>
            <goals><goal>detail</goal></goals>
          </execution>

          <execution>
            <id>package-app</id>
            <goals><goal>package</goal></goals>
            <configuration>
              <outputDirectory>path/to/${project.artifactId}</outputDirectory>
            </configuration>
          </execution>

          <execution>
            <id>clean-app</id>
            <goals><goal>clean</goal></goals>
            <configuration>
              <outputDirectory>path/to/${project.artifactId}</outputDirectory>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
```

该插件三个任务默认在maven的package/process-classes/clean阶段工作，作用分别为索引/打包/清除，当你在示例程序(client/server)的根目录执行:

```bash
mvn package
```

我们将会看到如下输出:

```
  path/to/server
    |  |-bin
    |  |  |-start.bat
    |  |  |-stop.bat
    |  |  |-start.sh
    |  |  |-stop.sh
    |  |-config
    |  |  |-logback.xml
    |  |-boot
    |  |  |-net.happyonroad.spring-component-framework-0.0.1.jar
    |  |-lib
    |  |  |-com.myapp.server-0.0.1.jar
    |  |  |-com.myapp.api-0.0.1.jar
    |  |  |-com.myapp.basis-0.0.1.jar
    |  |  |-org.springframework.spring-beans-3.2.4.RELEASE.jar
    |  |  |-<other depended jars>
    |  |  |-poms
    |  |  |  |-org.springframework.spring-beans-3.2.4.RELEASE.pom
    |  |  |  |-<other depended poms>
    |  |-logs
    |  |-tmp
```

```
  path/to/client
    |  |-bin
    |  |  |-start.bat
    |  |  |-stop.bat
    |  |  |-start.sh
    |  |  |-stop.sh
    |  |-config
    |  |  |-logback.xml
    |  |-boot
    |  |  |-net.happyonroad.spring-component-framework-0.0.1.jar
    |  |-lib
    |  |  |-com.myapp.client-0.0.1.jar
    |  |  |-com.myapp.api-0.0.1.jar
    |  |  |-org.springframework.spring-beans-3.2.4.RELEASE.jar
    |  |  |-<other depended jars>
    |  |  |-poms
    |  |  |  |-org.springframework.spring-beans-3.2.4.RELEASE.pom
    |  |  |  |-<other depended poms>
    |  |-logs
    |  |-tmp

```

此时Client与Server已经准备就绪，可以通过start(bat|sh)运行。

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