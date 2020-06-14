<!-- wp:heading {"level":3} -->
<h3>Introduction</h3>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p><strong>Flexicore adds to Spring Boot or to Java EE web application a very flexible and powerful plugins support</strong>,<strong> robust access control,</strong> <strong>and an additional set of services many applications require.</strong><br><strong> Learn more on Flexicore in Wizzdi web site</strong>:<br><a href="http://www.wizzdi.com">https://www.wizzdi.com</a></p>
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



<!-- wp:block {"ref":1400} /-->

<!-- wp:paragraph -->
<p><br></p>
<!-- /wp:paragraph -->

<!-- wp:heading {"level":3} -->
<h3>The benefits of a plugin system for back-end development</h3>
<!-- /wp:heading -->

<!-- wp:list -->
<ul><li>Avoid a monolithic, difficult to develop, and difficult to maintain systems. </li><li> The deployed system is fully modular but runs in a single process. The set of jars comprising the application is Spring Boot Flexicore jar and additional jars matching the number of developed or referenced plugins</li><li>Flexicore runs in the container process (the application server process, or Spring boot process).</li><li>Services of plugins can be injected into other plugins.</li><li>Entities can be extended forming new entities. <br><br></li></ul>
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

<!-- wp:paragraph -->
<p></p>
<!-- /wp:paragraph -->

<p><a href="https://www.wizzdi.com">https://www.wizzdi.com</a> - Company web site<br><a href="https://www.wizzdi.com">Wizzdi Software Systems</a></p>
<p>contact information:<br>info@wizzdi.com</p>

<!-- wp:paragraph -->
<p></p>
<!-- /wp:paragraph -->
