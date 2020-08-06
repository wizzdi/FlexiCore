
[![Build Status](https://jenkins.wizzdi.com/buildStatus/icon?job=FlexiCore)](https://jenkins.wizzdi.com/job/FlexiCore/)

![](https://wizzdi.com/wp-content/uploads/2020/06/flxicore.image_-1.png)

### What is Flexicore

**Flexicore adds to Spring Boot web application a very flexible and powerful plugins support**, **robust access control,** **and an additional set of services many applications require.**  
**Learn more on Flexicore in Wizzdi web site**:  
https://www.wizzdi.com

### Use Case

**AuthorService** in person-service jar

    @PluginInfo(version = 1)  
    public class PersonService implements ServicePlugin {  
      
     @Inject  
     @PluginInfo(version = 1)  
      private PersonRepository repository;  
      @Inject  
      private Logger logger;  
      
     public Person createPerson(PersonCreate personCreate, SecurityContext securityContext) {  
            Person person = createPersonNoMerge(personCreate, securityContext);  
      repository.merge(person);  
      return person;  
       }  


  
    public Person createPersonNoMerge(PersonCreate personCreate, SecurityContext securityContext) {  
        Person person = new Person(personCreate.getFirstName(),securityContext);  
        updatePersonNoMerge(person, personCreate); 
         return person;  
         }
  

  

**AuthorService** in library-service jar

    @PluginInfo(version = 1)  
    @Extension  
    @Component  
    public class AuthorService implements ServicePlugin {  
      
      @PluginInfo(version = 1)  
      @Autowired  
      private AuthorRepository repository;  
      @Autowired  
      private Logger logger;  
      
      @PluginInfo(version = 1)  
       @Autowired  
      private PersonService personService;  
      
     public Author createAuthor(AuthorCreate authorCreate,  
      SecurityContext securityContext) {  
          Author author = createAuthorNoMerge(authorCreate, securityContext);  
      repository.merge(author);  
     return author;  
      }  
      
       public Author createAuthorNoMerge(AuthorCreate authorCreate,  
      SecurityContext securityContext) {  
          Author author = new Author(authorCreate.getFirstName(),  
      securityContext);  
      updateAuthorNoMerge(author, authorCreate);  
     return author;  
      }
the first code snippet shows a **PersonService** plugin managing a person CRUD(only create shown) . the second code shows a **AuthorService** plugin managing authors and is dependent on **PersonService** . both of the plugins are compiled separately simply place both of them in FlexiCore plugin directory 

### Getting started

Maven Dependency for plugins:

      <properties>
            <flexicore-api.version>4.0.12</flexicore-api.version>
            <pf4j-spring.version>0.6.0</pf4j-spring.version>
           ...
        </properties>
        <dependencyManagement>
            <dependencies>
               ...
                <dependency>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-dependencies</artifactId>
                    <version>2.3.0.RELEASE</version>
                    <type>pom</type>
                    <scope>import</scope>
                </dependency>
            </dependencies>
        </dependencyManagement>
    
    ...
        <dependency>
                <groupId>com.wizzdi</groupId>
                <artifactId>flexicore-api</artifactId>
                <version>${flexicore-api.version}</version>
                <scope>provided</scope>
            </dependency>
            <dependency>
                <groupId>org.pf4j</groupId>
                <artifactId>pf4j-spring</artifactId>
                <version>${pf4j-spring.version}</version>
                <scope>provided</scope>
            </dependency>
        </dependencies>
    
       <plugins>
    ...
                <plugin>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.8.1</version>
                </plugin>
                <plugin>
                    <artifactId>maven-shade-plugin</artifactId>
                    <version>3.2.1</version>
                    <executions>
                        <execution>
                            <phase>package</phase>
                            <goals>
                                <goal>shade</goal>
                            </goals>
                            <configuration>
                                <minimizeJar>false</minimizeJar>
                                <createDependencyReducedPom>true</createDependencyReducedPom>
                                <dependencyReducedPomLocation>${java.io.tmpdir}/dependency-reduced-pom.xml
                                </dependencyReducedPomLocation>
                                <transformers>
                                 
                                    <transformerimplementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                        <manifestEntries>
               <!--check https://github.com/pf4j/pf4j-spring for more info-->
    
                                            <Plugin-Id>${artifactId}</Plugin-Id>
                                            <Plugin-Version>${version}</Plugin-Version>
    
                                        </manifestEntries>
                                    </transformer>
                                </transformers>
                            </configuration>
                        </execution>
                    </executions>
                </plugin>
            </plugins>
        </build>

compiling a FlexiCore-exec jar from repository

```
mvn package
```

running the FlexiCore-exec.jar

```
java -jar FlexiCore-exec.jar
```
### Prerequisites

-   PostgreSQL running and accessible at port 5432 locally (see below how to access a database on a different server)
-   A database named _flexicore_ (can be empty) with a login role named _flexicore_ having a password _flexicore_. See below how to change these default values.
-   MongoDB running locally and accessible at port 27017, the database will be automatically created.
-   a set of files and folders at _/home/flexicore_, get the files from [here](https://github.com/wizzdi/FlexiCore/blob/master/home.zip), please extract the file to your root or change the properties file below accordingly. The zip file can be opened to C:\ on a Windows system or to / on a Linux system.
-   JDK 11 or newer installed on the target system.

Spring Configuration Properties
|Property name|Description|Default Value|
|--|--|--|
| flexicore.entities | Path to entities folder, put your entity plugins here. | /home/flexicore/entities
 |
|flexicore.plugins|Path to plugins folder, put service plugins here.|/home/flexicore/plugins|
|flexicore.externalStaticMapping|mapping for static contents| /**|
|flexicore.externalStatic|static content location|/home/flexicore/ui/|
|flexicore.loginFailedAttempts|failed logging attempts till blacklisting ip (-1 to disable login blacklist)|-1|
|flexicore.loginBlacklistRetentionMs|time to keep ip in blacklist , only if flexicore.loginFailedAttempts is enabled|600000|
|spring.datasource.url|Postgresql connection string|jdbc:postgresql://localhost:5432/flexicore|
|spring.datasource.username|username to access PostgreSQL database|flexicore|
|spring.datasource.password|password to access PostgreSQL database|flexicore|
|spring.data.mongodb.uri|mongodb connection string|mongodb://localhost:27018/flexicoreNoSQL|
|spring.data.mongodb.port|mongodb port|27017|
|spring.data.mongodb.host|mongodb host|localhost|
|spring.data.mongodb.database|mongodb database|flexicoreNoSQL|

### Test FlexiCore runs properly

-   When using the default settings and the home.zip file extracted to your root folder, the password for accessing Flexicore is generated and is available in firstRun.txt file at /home/flexicore/firstRun.txt
-   use a modern browser to access FlexiCore Swagger interface, it allows you to execute the API through an easy to use interface:
    -   localhost:8080/FlexiCore
-   You should be prompted to enter a username and password, please use the username _admin@flexicore.com_ and the password from the firstRun.txt above.
-   You should see a user interface similar to the image below when running for the first time, the server may need some time for user interface generation.

### Swagger test sequence

![](https://wizzdi.com/wp-content/uploads/2020/06/swagger-sign-in-1.png)

sign in to Swagger

![](https://wizzdi.com/wp-content/uploads/2020/06/swagger-interface-1.png)

Swagger Typical interface

![](https://wizzdi.com/wp-content/uploads/2020/06/get-all-users-1.png)

Try the getAllUsers API

![](https://wizzdi.com/wp-content/uploads/2020/06/get-all-users-result-1.png)

Result

### Introduction

**Flexicore can power any type of back-end system or desktop application while allowing developers to build full applications from inter-injectable and extensible plugins.**

![](https://wizzdi.com/wp-content/uploads/2020/06/flexicore-in-spring-1.png)

**Spring Boot back-end powered by Flexicore**

The image above shows a typical back-end developed in Spring boot and powered by Flexicore. The back-end full domain model, business logic, services, API are all implemented as plugins.  
Development uses Spring annotations with few additional ones adding inter-injectable plugins support.

### Flexicore unique plugin system

### The benefits of a plugin system for back-end development

-   **Avoid a monolithic, difficult to develop, and difficult to maintain systems.**
-   The deployed system is fully modular and runs as a single process. The set of jars comprising the application is Spring Boot Flexicore jar and a set of additional jars placed in predefined configurable folders on the server/desktop. Each jar is a plugin and is separately deployed.
-   Services of plugins can be injected into other plugins.
-   Entities in one plugin can be extended forming new entities in a dependent plugin.
-   Deploy the plugins you need, remove the ones you do not.
-   **Flexibly add new features to some customers.**
-   Deploy multiple versions of the same services at the same time. Allows gradual migration during the product lifetime.

### Access Control in Flexicore

#### Access control, tenants and Flexicore plugins

When a system is developed with Flexicore, new entities are automatically added to the access control system. This is carried out by extending any entity from Flexicore _Baseclass_ or from any entity extending it. Such entities can either be defined in Flexicore itself or in any plugin in the plugin dependency chain.

#### Control access to operations

The common method of controlling access to REST API endpoints through annotation of the method with roles name is not used in Flexicore. Instead, developers just decide what is the _Operation_ name associated with the method. Developers can define a new name or use one of the basic four operations: _read, write, delete, and update._  
Defined _Operations_ are accessible from the API (or from an available access management system user interface) for a very fine granulated access control on Tenant, Role, and User level.

#### Control access to data

One of the more complex sub-systems to implement in a new system is access control to data. Flexicore provides the best of breed data access control out the box and transparently applied to all your new entities.  
When data is retrieved from the database, Flexicore applies all access control definitions, as stored in the database **as a single, optimized database operation.** that is, unlike many such solutions, no data is first retrieved into memory then removed from the dataset based on the current _User_ identity.  
In order to save database rows, the system uses default definitions on the Entity Class, Role, Tenant, and User level.  
When different access rules are required for some data set for some users or roles, new rules are stored. These rules override the default behavior.  
Data access is always in the context of the requested _Operation_, see above. So for example, a user may view a piece of information but may not edit it.  
Default access of a Tenant, a Role, or a User can be defined with respect to a Class of an Entity or to a specific instance.  
When handling millions and billions of rows, it is sometimes useful to allow or deny access to a heterogeneous group of data rows each of any entity. This is provided through the _Permission Group_ Entity. While _Role_ groups _User_ instances, _Permission Group_ groups instances of any Entity.

### Frequently Asked Questions

#### How Flexicore differs from Micro-services?

Like micro-services, Flexicore is designed to address the shortcomings and pitfalls of monolithic systems.  
However, Flexicore is not a design pattern, it is a run time framework allowing applications to be built from multiple plugins.  
Flexicore and the deployed plugins run in the same process, so it enjoys the benefits of dependency injection and class inheritance, including JPA inheritance among plugins.  
Many of the disadvantages associated with Micro-services are simply not present with the Flexicore plugin system.

#### Can Flexicore be used in Micro-services development?

Of course, a micro-service built using Flexicore is much more modular by itself.

This can reduce the number of micro-services and ease a shared database pattern when used, some of the plugins can be installed on the different micro-services accessing the same database.  
The same plugin can be deployed in different micro-services.

#### Prerequisites

##### development

-   Flexicore plugins are built using Maven or a suitable IDE capable of using Maven such as IntelliJ, Eclipse, or Netbeans, a reference to Flexicore-API needed, till this is available on Maven central, you can clone and install locally.
-   Other dependencies may apply based on requirements.
-   Examples repository is available:[https://github.com/wizzdi/FlexiCore-Examples](https://github.com/wizzdi/FlexiCore-Examples)

##### Run time

-   Java 11 JDK installed, this is a prerequisite for Spring. Java 8 is not frequently tested and may work.
-   running Flexicore server (Spring boot Jar), this can be obtained from Wizzdi or from Maven central, can also be built from this repository.
-   Spring boot jars can run as service in Linux and Windows, this is beyond the scope of this document.
-   Flexicore itself requires a PostgreSQL relational database and MongoDB installed.
-   See above the required properties for accessing the databases, note that the relational database must exist in the defined name as well as the role on PostgresSQL.

#### Are Flexicore based Systems currently in Production?

Flexicore is used in production by multiple organizations since 2015. Most existing systems are Wildfly (Java EE) based, Spring support is newer.

#### What types of systems have been developed using Flexicore?

-   A Video and media production system in the cloud.
-   A Personal health care system used by hundreds of thousands of users around the world, Flexicore provides the cloud system on AWS.
-   Medical system (embedded) connected to a Windows PC running Flexicore. Users can access the system locally or from any system connected to the local network.
-   A Medical system running in US hospitals with a unique deployment: Flexicore provides a local server on Android tablet hosting Linux image. A cloud-based system is used to aggregate the collected data using Flexicore too.
-   An IoT system for generically managing millions of sensors of many different types. This system is used to control critical units in cities using integrated video cameras, street lights, perimeter control, and Flexicore based gateways.
-   An IoT system for automated vending machines using Wizzdi distributed and synchronized database solution. Both cloud and endpoints are Flexicore based.
-   Talent recruitment system running on mobile devices, using SMS notifications from Flexicore based cloud server.
-   Video server systems for drones.
-   An AI system running on iOS and Android is connected to the Flexicore server providing statistics, configuration, and semi-automatic training of CNN models.

#### Is technical support available?

Technical support is available through info@flexi-core.com and info@wizzdi.com  
a new web site: support.wizzdi.com is available (under construction) for online and community support.

#### What other services based on Flexicore are provided?

Wizzdi software systems provide global design and implementation services in breakthrough price-performance to:

-   Design back-end systems of any size and shape.
-   Optionally implement back-end systems from scratch in a matter of days with full accountability for performance and compliance.
-   Deploy the implemented systems for you on the cloud in unbeatable price-performance. Dev-ops services included.
-   Implement user interfaces for the above using Google Flutter, Angular (Typescript), Java, and Swift.
-   Design and implement distributed systems based on Flexicore and Wizzdi database synchronization plugin.
-   Design and implement embedded systems if these are associated with Flexicore based systems.

[https://www.wizzdi.com](https://www.wizzdi.com) - Company web site  
[Wizzdi Software Systems](https://www.wizzdi.com)

contact information:  
info@wizzdi.com

### Main 3rd Party Dependencies

#### Spring Boot

https://github.com/spring-projects/spring-boot

#### Pf4J

https://github.com/pf4j/pf4j
