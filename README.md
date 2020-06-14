<!-- wp:paragraph -->
<p>Easy to manage, <a href="https://en.wikipedia.org/wiki/Multitenancy" target="_blank" rel="noreferrer noopener">multi-tenancy</a> state of the art roles &amp; permissions management&nbsp;</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Fits Business to Business and  Business to Customer patterns.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Complies with toughest and most challenging security demands.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>New modules (plugins) automatically benefit from the Flexicore authorization system, no additional development required.</p>
<!-- /wp:paragraph -->

<!-- wp:heading {"level":4} -->
<h4>Multi-tenancy </h4>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>Multi-tenancy allows managing multiple organizations on the same database allowing the organization's administrators the same flexibility they have with a dedicated database while providing aggregated access to multiple tenants by users authorized to do so.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>A proper Platform as a service and any System as a service must be based on a proper multi-tenant capable system.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>In Flexicore, data created in a tenant is available to tenant users if the tenant administrator has granted them access or if they have created the data. If so allowed, tenant administrators can create new tenants 'under' their tenant to any depth.</p>
<!-- /wp:paragraph -->

<!-- wp:heading {"level":4} -->
<h4>Multi-tenancy challenges</h4>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>Unlike simple access control systems where access to operations is controlled, data access control and multi-tenancy require very fine granularity access to data.<br>When multiple tenants, roles, and millions of users are involved, controlling access may hamper performance.<br>In Flexicore a high-performance relational database system controls access through default access definitions to types of data, groups of instances, clever use of roles so the number of extra links in the database is optimized.</p>
<!-- /wp:paragraph -->

<!-- wp:heading {"level":4} -->
<h4>Access control, tenants and Flexicore plugins</h4>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>When systems are developed with Flexicore, new entities are automatically added to the access control system. This is carried out by extending any entity from Flexicore <em>Baseclass</em> or from any entity extending it. Such entities can either be defined in Flexicore itself or in any entity defined in the plugin dependency chain. </p>
<!-- /wp:paragraph -->

<!-- wp:heading {"level":4} -->
<h4>Control access to operations</h4>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>The common method of controlling access to REST API endpoints through annotation of the method with roles name is not used in Flexicore. Instead, developers just decide what is the <em>Operation</em> name associated with the method. Developers can select a new name or one of the basic four operations, that is, <em>read, write, delete, and update.</em> <br>In addition, developers can define if a REST endpoint is allowed to everyone by default or blocked by default. <br>Any further access control is database driven.</p>
<!-- /wp:paragraph -->

<!-- wp:heading {"level":4} -->
<h4>Control access to data</h4>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>Access control to data is much more difficult to implement, Flexicore provides data access control out the box and transparently applied to all your new entities.<br>When data is retrieved from the database, Flexicore applies all access control definitions, as stored in the database <strong>as a single, optimized database operation. </strong>that is, unlike many such solutions, no data is first retrieved then removed from the dataset based on the current <em>User </em>identity. <br>In order to save database rows, the system uses default definitions on the Class (Entity), Role, Tenant, and User level. <br>When different access rules are required for some data set for some users or roles, new rules are stored. These rules override the default behavior.<br>Data access is always in the context of the requested <em>Operation</em>, see above.<br>However, here also, default access can be defined to the Entity level or to piece od data for <em>all Operations</em>.<br>When managing millions and billions of rows, it is sometimes required to allow or deny access to a heterogeneous group of data rows, of any entity set. This is provided through the <em>Permission Group</em> class. Like <em>Role </em>that groups <em>User</em> instances, <em>Permission Group</em> groups instances of any type. </p>
<!-- /wp:paragraph -->

<!-- wp:heading {"level":4} -->
<h4><a href="https://wizzdi.com/access-control/">Learn More</a></h4>
<!-- /wp:heading -->
