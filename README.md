[![Build Status](https://jenkins.wizzdi.com/buildStatus/icon?job=FlexiCore)](https://jenkins.wizzdi.com/job/FlexiCore/)
[![Build Status](https://jenkins.wizzdi.com/buildStatus/icon?job=FlexiCore)](https://jenkins.wizzdi.com/job/FlexiCore/)
<!-- wp:image {"id":1517,"sizeSlug":"large"} -->
<figure class="wp-block-image size-large"><img src="https://wizzdi.com/wp-content/uploads/2020/06/flxicore.image_-1.png" alt="" class="wp-image-1517"/></figure>
<!-- /wp:image -->

<!-- wp:heading {"level":3} -->
<h3>Introduction</h3>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p><strong>Flexicore adds to Spring Boot or to Java EE web application a very flexible and powerful plugins support</strong>,<strong> robust access control,</strong> <strong>and an additional set of services many applications require.</strong><br><strong> Learn more on Flexicore in Wizzdi web site</strong>:<br><a href="http://www.wizzdi.com"></a><a href="https://www.wizzdi.com"><a href="https://www.wizzdi.com">https://www.wizzdi.com</a></a></p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p></p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p><strong> Flexicore can host any type of back-end system or desktop application while allowing developers to build full applications from inter-injectable and extensible plugins.</strong></p>
<!-- /wp:paragraph -->

<!-- wp:image {"id":1481,"width":580,"height":317,"sizeSlug":"large"} -->
<figure class="wp-block-image size-large is-resized"><img src="https://wizzdi.com/wp-content/uploads/2020/06/Flexicore-in-Spring-2.png" alt="" class="wp-image-1481" width="580" height="317"/><figcaption><strong>Flexicore in Spring Boot based server</strong></figcaption></figure>
<!-- /wp:image -->

<!-- wp:paragraph -->
<p>The image above shows a typical deployed Flexicore based server using Spring boot, Flexicore itself is not changed when a back-end is built. The back-end full domain model, business logic, services, API are all hosted in plugins.</p>
<!-- /wp:paragraph -->

<!-- wp:heading {"level":3} -->
<h3>Flexicore unique plugin system</h3>
<!-- /wp:heading -->

<!-- wp:block {"ref":1400} /-->

<!-- wp:paragraph -->
<p><br></p>
<!-- /wp:paragraph -->

<!-- wp:heading {"level":3} -->
<h3>The benefits of a plugin system for back-end development</h3>
<!-- /wp:heading -->

<!-- wp:list -->
<ul><li><strong>Avoid a monolithic, difficult to develop, and difficult to maintain systems.</strong> </li><li> The deployed system is fully modular and runs in a single process. The set of jars comprising the application is Spring Boot Flexicore jar and a set of additional jars placed in predefined folders on the server/desktop. Each jar is a plugin and is separately deployed either manually or Flexicore plugins API.</li><li>Services of plugins can be injected into other plugins.</li><li>Entities can be extended forming new entities. </li><li>Deploy the plugins you need.</li><li><strong>Flexibly add new features to some customers.</strong></li><li>Use the Flexicore permissions system to expose new features only to target customers.</li><li>Deploy multiple versions of the same services at the same time. Allows gradual migration during the product lifetime.<br><br></li></ul>
<!-- /wp:list -->

<!-- wp:heading {"level":3} -->
<h3>Access Control in Flexicore </h3>
<!-- /wp:heading -->

<!-- wp:heading {"level":4} -->
<h4>Access control, tenants and Flexicore plugins</h4>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>When a system is developed with Flexicore, new entities are automatically added to the access control system. This is carried out by extending any entity from Flexicore&nbsp;<em>Baseclass</em>&nbsp;or from any entity extending it. Such entities can either be defined in Flexicore itself or in any entity defined in the plugin dependency chain.</p>
<!-- /wp:paragraph -->

<!-- wp:heading {"level":4} -->
<h4>Control access to operations</h4>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>The common method of controlling access to REST API endpoints through annotation of the method with roles name is not used in Flexicore. Instead, developers just decide what is the&nbsp;<em>Operation</em>&nbsp;name associated with the method. Developers can select a new name or one of the basic four operations, that is,&nbsp;<em>read, write, delete, and update.</em><br>In addition, developers can define if a REST endpoint is allowed to everyone by default or blocked by default.<br>Any further access control is database driven.</p>
<!-- /wp:paragraph -->

<!-- wp:heading {"level":4} -->
<h4>Control access to data</h4>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>Access control to data is much more difficult to implement, Flexicore provides data access control out the box and transparently applied to all your new entities.<br>When data is retrieved from the database, Flexicore applies all access control definitions, as stored in the database&nbsp;<strong>as a single, optimized database operation.&nbsp;</strong>that is, unlike many such solutions, no data is first retrieved then removed from the dataset based on the current&nbsp;<em>User&nbsp;</em>identity.<br>In order to save database rows, the system uses default definitions on the Class (Entity), Role, Tenant, and User level.<br>When different access rules are required for some data set for some users or roles, new rules are stored. These rules override the default behavior.<br>Data access is always in the context of the requested&nbsp;<em>Operation</em>, see above.<br>However, here also, default access can be defined to the Entity level or to piece od data for&nbsp;<em>all Operations</em>.<br>When managing millions and billions of rows, it is sometimes required to allow or deny access to a heterogeneous group of data rows, of any entity set. This is provided through the&nbsp;<em>Permission Group</em>&nbsp;class. Like&nbsp;<em>Role&nbsp;</em>that groups&nbsp;<em>User</em>&nbsp;instances,&nbsp;<em>Permission Group</em>&nbsp;groups instances of any type.</p>
<!-- /wp:paragraph -->

<!-- wp:heading {"level":3} -->
<h3>Frequently Asked Questions</h3>
<!-- /wp:heading -->

<!-- wp:heading {"level":4} -->
<h4>How Flexicore differs from Micro-services?</h4>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>Like micro-services, Flexicore is designed to address the shortcomings of monolithic systems.<br>However, Flexicore is not a design pattern, it is a run time framework providing services to applications built from multiple plugins.<br>Flexicore runs with plugins in the same process so it enjoys the benefits of dependency injection and class inheritance, including entities.<br>Flexicore dependency tree is truly uni-directional, this cannot always be true for micro-services.</p>
<!-- /wp:paragraph -->

<!-- wp:heading {"level":4} -->
<h4>Can Flexicore be used in Micro-services development?</h4>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>Of course, a micro-service built using Flexicore is much more modular by itself.<br>plugins can be deployed in different micro-services.</p>
<!-- /wp:paragraph -->

<!-- wp:heading {"level":4} -->
<h4>Are Flexicore based Systems currently in Production?</h4>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>Flexicore is used in production by multiple organizations since 2015. Most existing systems are Wildfly (Java EE) based, Spring support is newer.</p>
<!-- /wp:paragraph -->

<!-- wp:heading {"level":4} -->
<h4>What types of systems have been developed using Flexicore?</h4>
<!-- /wp:heading -->

<!-- wp:list -->
<ul><li>Video and media production system in the cloud.</li><li>Personal health care system used by hundreds of thousands of users around the world, Flexicore provides the cloud system on AWS.</li><li>Medical system (embedded) connected to a Windows PC running Flexicore accessible locally through browser or Electron-based app and also from other computers in the clinic</li><li>Medical system running in US hospitals with unique deployment, Flexicore provides a local server on Android tablet hosting Linux image.  A cloud-based system is used to aggregate the collected data using Flexicore too.</li><li>IoT system for generically managing millions of sensors of many different types. This system is used to control critical units in cities using integrated video cameras, street lights, perimeter control, and Flexicore based gateways.</li><li>IoT system for automated vending machines using Wizzdi distributed and synchronized database solution. Both cloud and endpoints are Flexicore based.</li><li>Talent recruitment system running on mobile devices, using SMS notifications from Flexicore based cloud server.</li><li>Video server systems for drones.<br><br><br></li></ul>
<!-- /wp:list -->

<!-- wp:heading {"level":4} -->
<h4>Is technical support available?</h4>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>Technical support is available through info@flexi-core.com and info@wizzdi.com<br>a new web site: support.wizzdi.com is available (under construction) for online and community support.</p>
<!-- /wp:paragraph -->

<!-- wp:heading {"level":4} -->
<h4>What other services based on Flexicore are provided?</h4>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>Wizzdi software systems provides design and implementation services in breakthrough price performance to:</p>
<!-- /wp:paragraph -->

<!-- wp:list -->
<ul><li>Design back-end systems of any size and shape.</li><li>Optionally implement back-end systems from scratch in matter of days.</li><li>Implement user interfaces for the above using Flutter, Angular (Typescript) , Java and Swift.</li><li>Design and implement distributed systems based on Flexicore and Wizzdi database synchronization plugin.</li><li>Design and implement embedded systems if these are associated with Flexicore based systems.<br><br></li></ul>
<!-- /wp:list -->

<p><a href="https://www.wizzdi.com"></a><a href="https://www.wizzdi.com"></a><a href="https://www.wizzdi.com">https://www.wizzdi.com</a> - Company web site<br><a href="https://www.wizzdi.com">Wizzdi Software Systems</a></p>
<p>contact information:<br>info@wizzdi.com</p>

<!-- wp:paragraph -->
<p></p>
<!-- /wp:paragraph -->
