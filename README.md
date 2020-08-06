



# ![](https://support.wizzdi.com/wp-content/uploads/2020/05/flexicore-icon-extra-small.png) FlexiCore [![Build Status](https://jenkins.wizzdi.com/buildStatus/icon?job=FlexiCore)](https://jenkins.wizzdi.com/job/FlexiCore/)


Flexicore empowers [Spring Boot]([https://github.com/spring-projects/spring-boot](https://github.com/spring-projects/spring-boot)) applications with a very flexible and powerful plugins support, robust access control,and an optional set of services many applications require

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
The first code snippet shows a **PersonService** plugin managing a person CRUD (only create is shown) . the second code sinppet shows an **AuthorService** plugin managing authors which is dependent on **PersonService** . both plugins are compiled separately. use by placing both of them in the FlexiCore plugin directory 


## Documentation

FlexiCore maintains a reference [documentation ](https://support.wizzdi.com)

## Stay in Touch
Contact us at our [site]([http://wizzdi.com/](http://wizzdi.com/))


### Main 3rd Party Dependencies

[Spring Boot](https://github.com/spring-projects/spring-boot)

[Pf4J](https://github.com/pf4j/pf4j)
