
[![Build Status](https://jenkins.wizzdi.com/buildStatus/icon?job=FlexiCore)]

https://www.wizzdi.com - Company web site 
[Wizzdi Software Systems](https://www.wizzdi.com)

contact information: 
info@wizzdi.com 

<!-- wp:heading {"level":3} -->
<h3>Introduction</h3>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p><strong>Flexicore hosts any type of back-end or desktop application while allowing developers to build 100% of their applications from inter injectable and extensible plugins.</strong></p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Flexicore is provided in two versions, for <a href="https://en.wikipedia.org/wiki/Spring_Framework" target="_blank" rel="noreferrer noopener">Spring </a>and Java EE (<a href="https://en.wikipedia.org/wiki/WildFly" target="_blank" rel="noreferrer noopener">Wildfly application server</a>).</p>
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
<ul><li>Like Micro-services, Flexicore is designed to avoid monolithic, difficult to develop, and difficult to maintain systems. </li><li> Unlike Micro-services, Flexicore and defined plugins run in a single process.<br>Flexicore runs in the container process (the application server process, or Spring boot process)<br>Services of plugins can be injected into other plugins.<br>Entities can be extended while defining new entities. <br>This flexibility is not available with Micro-services.</li><li>Yet, Flexicore can be used to develop and better manage micro-services.<br>In such a workflow, the service itself is comprised of multiple plugins.  in addition, plugins can be reused in multiple services, for example for accessing a shared database or defining services common to more than one micro-service, depending on the dependency tree, micro-services can be easily changed by moving plugins among micro-services.</li></ul>
<!-- /wp:list -->

<!-- wp:list -->
<ul><li>same process plugins-based system provides:<ul><li>True single direction dependency (can’t be achieved with Microservices)</li><li>Dependency injection</li><li>Entities inheritance</li><li>Shared database(s) with true dependency isolation&nbsp;</li></ul></li><li>Deeper layers never require changes when upper layers are created or modified. </li><li>Generics, core system, and deeper layers serve entities and result sets of upper layers in the correct type without being aware of newly introduced entities and containers.</li><li>Allows developers to focus on their core skills</li><li>Allows independent development stream for each team</li><li>Build only the features your customers need now, add additional plugins, or extend existing ones.</li><li>Keep on the server multiple versions of the same services while migrating to a new version.</li><li>Deploy a different set of plugins matching and evolving customer needs.</li></ul>
<!-- /wp:list -->

<!-- wp:image {"align":"center","id":1410,"sizeSlug":"large"} -->
<div class="wp-block-image"><figure class="aligncenter size-large"><img src="https://wizzdi.com/wp-content/uploads/2020/06/Flexicore-dependencies-Spring-1.png" alt="" class="wp-image-1410"/><figcaption>Typical Plugins inter-dependencies Spring example </figcaption></figure></div>
<!-- /wp:image -->

<!-- wp:image {"id":1413,"sizeSlug":"large"} -->
<figure class="wp-block-image size-large"><img src="https://wizzdi.com/wp-content/uploads/2020/06/Flexicore-dependencies-sample-Java-EE-1-1.png" alt="" class="wp-image-1413"/><figcaption>Typical Plugins inter-dependencies Java EE (Wildly) example</figcaption></figure>
<!-- /wp:image -->

<!-- wp:heading {"level":4} -->
<h4>Explanation</h4>
<!-- /wp:heading -->

<!-- wp:list -->
<ul><li>Dependency and inheritance are always in <strong>one direction</strong></li><li>Flexicore services and models as well as depend-on plugins never need a change to support your plugins’ new features.</li></ul>
<!-- /wp:list -->

<!-- wp:image {"id":1415,"sizeSlug":"large"} -->
<figure class="wp-block-image size-large"><img src="https://wizzdi.com/wp-content/uploads/2020/06/different-customers-slightly-changed-deployment.png" alt="" class="wp-image-1415"/><figcaption>Gain flexibility in addressing customer needs<br></figcaption></figure>
<!-- /wp:image -->

<!-- wp:paragraph -->
<p></p>
<!-- /wp:paragraph -->
