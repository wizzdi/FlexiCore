



# ![](https://support.wizzdi.com/wp-content/uploads/2020/05/flexicore-icon-extra-small.png) FlexiCore [![Build Status](https://jenkins.wizzdi.com/buildStatus/icon?job=FlexiCore)](https://jenkins.wizzdi.com/job/FlexiCore/)

### What is 

> Flexicore

**Flexicore adds to Spring Boot web application a very flexible and powerful plugins support**, **robust access control,** **and an additional set of services many applications require.**  
**Learn more on Flexicore in Wizzdi web site**:  
https://www.wizzdi.com

## Use Case

**PersonService** in person-service jar

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

## Getting started

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
## Prerequisites

-   PostgreSQL running and accessible at port 5432 locally (see below how to access a database on a different server)
-   A database named _flexicore_ (can be empty) with a login role named _flexicore_ having a password _flexicore_. See below how to change these default values.
-   MongoDB running locally and accessible at port 27017, the database will be automatically created.
-   JDK 11 or newer installed on the target system.

## Spring Configuration Properties
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

## Documentation

FlexiCore maintains a reference [documentation ](https://support.wizzdi.com)

## Stay in Touch
Contact us at our [site]([http://wizzdi.com/](http://wizzdi.com/))


### Main 3rd Party Dependencies

[Spring Boot](https://github.com/spring-projects/spring-boot)

[Pf4J](https://github.com/pf4j/pf4j)
