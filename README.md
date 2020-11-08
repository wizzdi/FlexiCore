



# ![](https://support.wizzdi.com/wp-content/uploads/2020/05/flexicore-icon-extra-small.png) FlexiCore [![Build Status](https://jenkins.wizzdi.com/buildStatus/icon?job=FlexiCore)](https://jenkins.wizzdi.com/job/FlexiCore/)[![Maven Central](https://img.shields.io/maven-central/v/com.wizzdi/flexicore-api.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.wizzdi%22%20AND%20a:%22flexicore-api%22)[![Maven Central](https://img.shields.io/docker/cloud/automated/wizzdi/flexicore)](https://hub.docker.com/r/wizzdi/flexicore)


For comprehensive information about FlexiCore please visit our [site](http://wizzdi.com/).

FlexiCore boosts [Spring Boot](https://github.com/spring-projects/spring-boot) applications with a very flexible and powerful plugins support, robust access control and an optional set of services many applications require.

## Use Case

**PersonService** in person-service jar

    @PluginInfo(version = 1)  
    @Extension  
    @Component
    public class PersonService implements ServicePlugin {  
    
      private static final Logger logger=LoggerHandler.getLogger(PersonService.class);  
      
     @Autowired  
     @PluginInfo(version = 1)  
      private PersonRepository repository;  
      
      
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
The first code snippet shows a **PersonService** plugin managing a person CRUD (only create is shown) . the second code sinppet shows an **AuthorService** plugin managing authors which is dependent on **PersonService** . both plugins are compiled separately. use by placing both of them in the FlexiCore plugin directory 

## Debugging Plugins
add the relevant debuggin line to your java properties
for java 11: 
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8787

for java 8:
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8787

now from your IDE connect to the port specified in the above command (8787)


## Documentation
FlexiCore maintains a reference [documentation](https://support.wizzdi.com)

## Docker
if your machine is running docker ( if not installation instructions are available in [docker documentation site](https://docs.docker.com/get-docker/) ) simply run:

    docker run -p 8080:8080 wizzdi/flexicore
    
once initialization is complete a swagger will be avilable at http://<your-server-ip>:8080/FlexiCore
the default username is admin@flexicore.com the password is randomlly generated and can be found in the docker container at /home/flexicore/firstRun.txt
docker image is available at [Docker Hub](https://hub.docker.com/r/wizzdi/flexicore)


## Stay in Touch
Contact us at our [site](http://wizzdi.com/)


### Main 3rd Party Dependencies

[Spring Boot](https://github.com/spring-projects/spring-boot)

[Pf4J](https://github.com/pf4j/pf4j)
