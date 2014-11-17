spring-component-framework
==========================

1. Scenario
-----------

The spring component framework is used to setup a plugin based, micro-kernel, standalone application(today, we will support webapp in later releases) which is based on SpringFramework.

It can help you decouple your application into several components clearly with zero invasion
and keep your application consistent between develop time and runtime.

You can download the [spring-component-example application](https://github.com/Kadvin/spring-component-example) and try it when you read the usage below.


2. Usage
----------

### 2.1 Normal application

Given you want to develop a complex application contains two parts: server and client

```
              +---------+           +--------+
              | server  |           | client |
   Caller --> |  |-basis|           |  |     |
              |  |-api  |<---RMI--->|  |-api |
              +---------+           +--------+

```

server and client will run in standalone runtimes, and deployed with a shared static lib both: api

they can communicate with each other in accordance with the contract defined in api through RMI(over SpringFramework).

and the server is deployed with another shared component: basis.

caller is a simulator of outer user, which can be executed by command line.

you can seperate the project into several modules:

```xml
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

  Define the api between modules.

```java
  //all below API is in this package;
  package com.myapp.api;

  /**
   * The Server API, used by client or caller
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
   * The Client API, used by server
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
   * A shared service, which will be used by server
   */
  public interface CacheService{
    boolean store(String key, Object value);
    Object pick(String key);
  }
```

and the pom of api:

```xml
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

  The client pseudo-code:

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

and the pom of client looks like:

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

  Provide some basic services which can be deployed and used by others(server).

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

and the pom of the basis:

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

  The server depends on the api project and services provided by basis.

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

and the pom of server:

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

  A command line programm, accept user command, call server api by RMI.

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

the pom of caller looks like:

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

The caller will be packaged as a uberjar, and you can execute it by `java -jar path/to/caller.jar job`

### 2.2 Componentization

#### 1. Static Component

Because of the api project does not provide any bean instances in runtime, we treat it as a **static** component.

  you just need package this project output as:

```
  path/to/com.myapp.api-0.0.1.jar!
    |-META-INF
    |  |-MANIFEST.MF               # Normal, generated by package tool
    |  |-pom.xml                   # just the pom of api projects
    |-com
    |  |-myapp
    |  |  |-api
    |  |  |  |-ServerAPI.class
    |  |  |  |-ClientAPI.class
    |  |  |  |-CacheService.class
```

The spring-component-framework will resolve the dependencies declaired by pom.xml in runtime.

#### 2. Application Component

The client runs as a standalone runtime, and it exports services by RMI,
you should use spring application context to manage them.

And we treat it as an **application** component.

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

and package this project:

```
  path/to/com.myapp.client-0.0.1.jar!
    |-META-INF
    |  |-MANIFEST.MF
    |  |-pom.xml                   # just the pom of basis projects
    |  |-application.xml           # spring context defined above
    |-com
    |  |-myapp
    |  |  |-client
    |  |  |  |-ClientImpl.class
```

The spring-component-framework will create an application context defined by application.xml for it in runtime.


#### 3. Service Component(Provider)

The basis need create a CacheServiceImpl bean in runtime and exports it as a shared service,

 We treat it as a **service** component which contains a service.xml besides application.xml:

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

and it contains an application.xml also:

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

at last, package the basis output as:

```
  path/to/com.myapp.basis-0.0.1.jar!
    |-META-INF
    |  |-MANIFEST.MF
    |  |-pom.xml                   # just the pom of basis projects
    |  |-application.xml           # spring context defined above
    |  |-service.xml               # service declaration 
    |-com
    |  |-myapp
    |  |  |-basis
    |  |  |  |-CacheServiceImpl.class
```

The spring-component-framework will **export** the service to be imported by other service components.


#### 4. Service Component(Consumer)

The server is a **service** component also, which will create some beans not only, depends some other services but also in runtime.

Import some services as below:

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

Organize inner beans by:

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

at last, package the server output as:

```
  path/to/com.myapp.server-0.0.1.jar!
    |-META-INF
    |  |-MANIFEST.MF
    |  |-pom.xml                   # just the pom of basis projects
    |  |-application.xml           # spring context defined above
    |  |-service.xml               # service declaration 
    |-com
    |  |-myapp
    |  |  |-server
    |  |  |  |-ServerImpl.class
```

#### 5. Service Component(Mixed)

If there is a component which will use other services not only, provide some services but also.

You can declair those imports/exports in the service.xml both, then it acts as a mixed service component.

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

  * You can qualify the service relationship by specifying a `hint` for import/export
    if there are multiple services exported with the given interface in module depends.
    
  * You can specify the exported service with a `ref` to target the bean which is exported actually
    if there are multiple beans implements the given interface in module application context

  * You can name the imported service by specify a `as` element, 
    then you can qualify the service with Spring `@Qualifier` as common spring application.

### 2.3 Deploy the project

#### 1. Deploy myapp manually

The application should be deployed with some constrants:

Given the target folder of the server release is path/to/server

  1. All libraries(include com.myapp.*, 3rd parts) should be placed in lib
  2. We should place the pom of 3rd-part libraries without META-INF/pom.xml in the jar in lib/poms
  3. Spring-component-framework jar should be placed in boot

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

Then you can start your application by below script:

```
  cd path/to/server
  java -jar boot/net.happyonroad.spring-component-framework-0.0.1.jar com.myapp.server-0.0.1
```
the last argument tell the spring-component-framework where to start the application.

then you will see below output:

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
Input command:
```

#### 2. Deploy myapp automatically

Because of it's zero invasion, you can add spring-component-framework as runtime dependency 
to your main project's pom

  client and server only, whom will be started by:

```
  java -jar spring-component-framework-0.0.1.jar com.myapp.server-0.0.1
or
  java -jar spring-component-framework-0.0.1.jar com.myapp.client-0.0.1
```

```xml
<dependencies>
  <dependency>
    <groupId>net.happyonroad</groupId>
    <artifactId>spring-component-framework</artifactId>
    <version>0.0.1</version>
    <scope>runtime</scope>
  </dependency>
</dependencies>
```

and you need add a customized plugin to package the client/server app:

```xml
  <build>
    <plugins>
      <plugin>
        <groupId>net.happyonroad</groupId>
        <artifactId>spring-component-builder</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <executions>
          <execution>
            <id>package-app</id>
            <phase>package</phase>
            <goals><goal>package</goal></goals>
            <configuration>
              <outputDirectory>path/to/${project.artifactId}</outputDirectory>
            </configuration>
          </execution>

          <execution>
            <id>clean-app</id>
            <phase>clean</phase>
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

when you execute such commands in the project root:

```bash
mvn package
```

you should saw your app is built like below:

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

and your client and server is ready for start or stop by corresponding start/stop (bat|sh) file.

3. Extended
---------------

### 3.1 Define an extended component type
### 3.2 Resolve the extended component
### 3.3 Inject the resolver for the runtime application

4. Technologies
---------------

Someone maybe doubt about, why there is another more wheel about component/plugin framework? 

There is OSGi framework already, and Spring DM server as application server also.

Even more, SpringSource is developing a sub-project named as [spring-plugin](https://github.com/spring-projects/spring-plugin). 

I'v tried to integrate those excellent products into my application, but I found I'm stucked in OSGi terrable complexicity, especially integrated with my familar tools, such as IDE(Intellij), repository managment(Maven).

I think OSGi's complexity comes from runtime dynamic ability, it try to help me create a person who can cut off his leg and replace with another new one.

But I don't want a so powerful and terrible man, I just need a normal man who can be borned, play and dead then.

But it doesn't means, application based on this framework can't be dynamical, I leave this choice to application developer.

Because we use spring as IOC container, maven as dependencies management tool, so we bind springframework as inner application container, maven as develop/runtime dependencies management. 

We referred to the IOC container of maven: Plexus, and we use its ClassWorlds as jar's class path manager.

About the spring-plugin, I have referred it when I finish this project, but I think we have different concerns, it seems to enhance the application in just one application context cross many jars, just like normal spring app does.

That is to say, it take care about connectivity more than isolation (in my opinion).

below is the example application context configuration I copied from it's README.

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

The Spring Component Framework is designed to be:

 * Component oriented
 * Developer friendly
 * Zero invasion
 * Consistent in anytime from any aspects

TODO: more technology details