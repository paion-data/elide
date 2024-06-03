"use strict";(self.webpackChunkdocs=self.webpackChunkdocs||[]).push([[1965],{1970:(e,s,t)=>{t.r(s),t.d(s,{assets:()=>o,contentTitle:()=>d,default:()=>h,frontMatter:()=>r,metadata:()=>l,toc:()=>a});var n=t(4848),i=t(8453);const r={sidebar_position:15,title:"Design",description:"Design Overview"},d=void 0,l={id:"design",title:"Design",description:"Design Overview",source:"@site/docs/design.md",sourceDirName:".",slug:"/design",permalink:"/docs/design",draft:!1,unlisted:!1,editUrl:"https://github.com/facebook/docusaurus/tree/main/packages/create-docusaurus/templates/shared/docs/design.md",tags:[],version:"current",sidebarPosition:15,frontMatter:{sidebar_position:15,title:"Design",description:"Design Overview"},sidebar:"tutorialSidebar",previous:{title:"Development",permalink:"/docs/development"}},o={},a=[{value:"Overview",id:"overview",level:2},{value:"Module Layout",id:"module-layout",level:2},{value:"High Level Design",id:"high-level-design",level:2},{value:"Parsing Layer",id:"parsing-layer",level:3},{value:"Business Logic Layer",id:"business-logic-layer",level:3},{value:"Persistence Layer",id:"persistence-layer",level:3},{value:"Client Request Model",id:"client-request-model",level:3},{value:"Metadata and Configuration",id:"metadata-and-configuration",level:3},{value:"Modeling",id:"modeling",level:3},{value:"Security Subsystem",id:"security-subsystem",level:2},{value:"Analytic Query Subsystem",id:"analytic-query-subsystem",level:2}];function c(e){const s={a:"a",code:"code",h2:"h2",h3:"h3",img:"img",li:"li",p:"p",table:"table",tbody:"tbody",td:"td",th:"th",thead:"thead",tr:"tr",ul:"ul",...(0,i.R)(),...e.components};return(0,n.jsxs)(n.Fragment,{children:[(0,n.jsx)(s.h2,{id:"overview",children:"Overview"}),"\n",(0,n.jsx)(s.p,{children:"The following guide is intended for developers who want to make changes to the Elide framework. It will cover the design\nof various subsystems."}),"\n",(0,n.jsx)(s.h2,{id:"module-layout",children:"Module Layout"}),"\n",(0,n.jsx)(s.p,{children:"Elide is a mono-repo consisting of the following published modules:"}),"\n",(0,n.jsxs)(s.table,{children:[(0,n.jsx)(s.thead,{children:(0,n.jsxs)(s.tr,{children:[(0,n.jsx)(s.th,{children:"Module Name"}),(0,n.jsx)(s.th,{children:"Description"})]})}),(0,n.jsxs)(s.tbody,{children:[(0,n.jsxs)(s.tr,{children:[(0,n.jsx)(s.td,{children:"elide-core"}),(0,n.jsx)(s.td,{children:"Contains modeling annotations, JSON-API parser, and core functions."})]}),(0,n.jsxs)(s.tr,{children:[(0,n.jsx)(s.td,{children:"elide-graphql"}),(0,n.jsx)(s.td,{children:"Contains the GraphQL parser."})]}),(0,n.jsxs)(s.tr,{children:[(0,n.jsx)(s.td,{children:"elide-async"}),(0,n.jsx)(s.td,{children:"Contains Elide's asynchronous and data export APIs."})]}),(0,n.jsxs)(s.tr,{children:[(0,n.jsx)(s.td,{children:"elide-swagger"}),(0,n.jsx)(s.td,{children:"Contains OpenAPI document generation for JSON-API."})]}),(0,n.jsxs)(s.tr,{children:[(0,n.jsx)(s.td,{children:"elide-standalone"}),(0,n.jsx)(s.td,{children:"Opinionated embedded Jetty application with JAX-RS endpoints for Elide"})]}),(0,n.jsxs)(s.tr,{children:[(0,n.jsx)(s.td,{children:"elide-spring"}),(0,n.jsx)(s.td,{children:"Parent module for spring boot support"})]}),(0,n.jsxs)(s.tr,{children:[(0,n.jsx)(s.td,{children:"elide-spring-boot-autoconfigure"}),(0,n.jsx)(s.td,{children:"Elide spring boot auto configuration module"})]}),(0,n.jsxs)(s.tr,{children:[(0,n.jsx)(s.td,{children:"elide-spring-boot-starter"}),(0,n.jsx)(s.td,{children:"Elide spring boot starter pom"})]}),(0,n.jsxs)(s.tr,{children:[(0,n.jsx)(s.td,{children:"elide-test"}),(0,n.jsxs)(s.td,{children:["JSON-API and GraphQL test DSLs for ",(0,n.jsx)(s.a,{href:"https://rest-assured.io/",children:"Rest Assured Testing Framework"})]})]}),(0,n.jsxs)(s.tr,{children:[(0,n.jsx)(s.td,{children:"elide-integration-tests"}),(0,n.jsx)(s.td,{children:"Integration tests that are run for JPA, Hibernate, and In-Memory data stores."})]}),(0,n.jsxs)(s.tr,{children:[(0,n.jsx)(s.td,{children:"elide-model-config"}),(0,n.jsx)(s.td,{children:"HJSON Configuration language for the Aggregation store semantic layer."})]}),(0,n.jsxs)(s.tr,{children:[(0,n.jsx)(s.td,{children:"elide-datastore"}),(0,n.jsx)(s.td,{children:"Parent module for all data stores."})]}),(0,n.jsxs)(s.tr,{children:[(0,n.jsx)(s.td,{children:"elide-datastore-aggregation"}),(0,n.jsx)(s.td,{children:"Datastore and semantic layer for processing analytic API queries."})]}),(0,n.jsxs)(s.tr,{children:[(0,n.jsx)(s.td,{children:"elide-datastore-hibernate"}),(0,n.jsx)(s.td,{children:"Parent package for all hibernate and JPA stores."})]}),(0,n.jsxs)(s.tr,{children:[(0,n.jsx)(s.td,{children:"elide-datastore-hibernate3"}),(0,n.jsx)(s.td,{children:"Legacy data store for Hibernate 3 support."})]}),(0,n.jsxs)(s.tr,{children:[(0,n.jsx)(s.td,{children:"elide-datastore-hibernate5"}),(0,n.jsx)(s.td,{children:"Legacy data store for Hibernate 5 support."})]}),(0,n.jsxs)(s.tr,{children:[(0,n.jsx)(s.td,{children:"elide-datastore-jpa"}),(0,n.jsx)(s.td,{children:"Primary data store for elide CRUD API queries.  Replaces legacy hibernate stores."})]}),(0,n.jsxs)(s.tr,{children:[(0,n.jsx)(s.td,{children:"elide-datastore-mulitplex"}),(0,n.jsx)(s.td,{children:"Support for multiple data stores."})]}),(0,n.jsxs)(s.tr,{children:[(0,n.jsx)(s.td,{children:"elide-datastore-noop"}),(0,n.jsx)(s.td,{children:"Zero persistence store.  This is used for implementing simple POST functions."})]}),(0,n.jsxs)(s.tr,{children:[(0,n.jsx)(s.td,{children:"elide-datastore-search"}),(0,n.jsx)(s.td,{children:"Indexed text search store.  It must be used in conjunction with the JPA store."})]}),(0,n.jsxs)(s.tr,{children:[(0,n.jsx)(s.td,{children:"elide-datastore-inmemorydb"}),(0,n.jsx)(s.td,{children:"Hashmap implementation of a datastore."})]})]})]}),"\n",(0,n.jsx)(s.h2,{id:"high-level-design",children:"High Level Design"}),"\n",(0,n.jsx)(s.p,{children:"The following diagram represents a high level component breakout of Elide. Names in italics represent class names\nwhereas other names represent functional blocks (made up of many classes).  Gray arrows represent client request and\nresponse flow through the system."}),"\n",(0,n.jsx)(s.p,{children:(0,n.jsx)(s.img,{alt:"High Level Design",src:t(2688).A+"",width:"960",height:"720"})}),"\n",(0,n.jsx)(s.p,{children:"Elide can be broken down into the following layers:"}),"\n",(0,n.jsx)(s.h3,{id:"parsing-layer",children:"Parsing Layer"}),"\n",(0,n.jsxs)(s.p,{children:["The parsing layer consists of a JSON-API parser and GraphQL parser.  This layer is responsible for mapping a client\nrequest in JSON-API or GraphQL into ",(0,n.jsx)(s.a,{href:"#client-request-model",children:"Elide's internal request model"}),". The parsers load, create,\nand manipulate Elide models via the ",(0,n.jsx)(s.code,{children:"PersistentResource"}),"."]}),"\n",(0,n.jsx)(s.h3,{id:"business-logic-layer",children:"Business Logic Layer"}),"\n",(0,n.jsx)(s.p,{children:"The business logic layer is responsible for performing:"}),"\n",(0,n.jsxs)(s.ul,{children:["\n",(0,n.jsx)(s.li,{children:"Authorization checks"}),"\n",(0,n.jsx)(s.li,{children:"Lifecycle hooks"}),"\n",(0,n.jsx)(s.li,{children:"Audit & Logging"}),"\n"]}),"\n",(0,n.jsxs)(s.p,{children:["All elide models (once loaded or created) are wrapped in a ",(0,n.jsx)(s.code,{children:"PersistentResource"}),".  All attribute and relationship access\n(read & write) occur through this abstraction allowing a central place to enforce business rules."]}),"\n",(0,n.jsxs)(s.p,{children:["In addition to invoking security checks and lifecycle hooks, the ",(0,n.jsx)(s.code,{children:"PersistentResource"})," is also responsible for reading\nand writing the model and its fields to the persistence layer."]}),"\n",(0,n.jsx)(s.h3,{id:"persistence-layer",children:"Persistence Layer"}),"\n",(0,n.jsx)(s.p,{children:"The persistence layer consists of two abstractions and their concrete implementations:"}),"\n",(0,n.jsxs)(s.ul,{children:["\n",(0,n.jsxs)(s.li,{children:["A ",(0,n.jsx)(s.code,{children:"DataStore"})," which is responsible for telling Elide which models it manages and creating ",(0,n.jsx)(s.code,{children:"DataStoreTransaction"}),"\nobjects."]}),"\n",(0,n.jsxs)(s.li,{children:["A ",(0,n.jsx)(s.code,{children:"DataStoreTransaction"})," which is created per request and is responsible for saving, loading, and deleting Elide\nmodels. Each request's interactions with the persistence layer should occur atomically."]}),"\n"]}),"\n",(0,n.jsxs)(s.p,{children:["Elide comes bundled with a number of ",(0,n.jsx)(s.code,{children:"DataStore"})," ",(0,n.jsx)(s.a,{href:"datastores",children:"implementations"}),". The most notable are the JPA, Search,\nand Aggregation stores."]}),"\n",(0,n.jsx)(s.h3,{id:"client-request-model",children:"Client Request Model"}),"\n",(0,n.jsxs)(s.p,{children:["The primary object in the client request model is the ",(0,n.jsx)(s.code,{children:"EntityProjection"}),".  It represents the entire model graph\nrequested by the client.  The entity projection consists of ",(0,n.jsx)(s.code,{children:"Attribute"})," objects (model fields), ",(0,n.jsx)(s.code,{children:"Relationship"})," objects\n(named entity projections), and also whether the projection should be filtered, sorted, or paginated. ",(0,n.jsx)(s.code,{children:"Attribute"}),"\nobjects can take ",(0,n.jsx)(s.code,{children:"Argument"})," objects as parameters."]}),"\n",(0,n.jsx)(s.h3,{id:"metadata-and-configuration",children:"Metadata and Configuration"}),"\n",(0,n.jsxs)(s.p,{children:["Elide is configured either with Spring Boot or the Elide Standalone module. Application settings for Spring and\nStandalone are mapped to an internal ",(0,n.jsx)(s.code,{children:"ElideSettings"})," object that configures the Elide framework (denoted by the ",(0,n.jsx)(s.code,{children:"Elide"}),"\nobject)."]}),"\n",(0,n.jsxs)(s.p,{children:["All static metadata about Elide models is extracted at service boot and stored in the ",(0,n.jsx)(s.code,{children:"EntityDictionary"}),". This class is\nused throughout Elide whenever a model must be read from or written to by the ",(0,n.jsx)(s.code,{children:"PersistentResource"}),"."]}),"\n",(0,n.jsxs)(s.p,{children:["While earlier versions of Elide represented models as JVM classes, Elide 5.x introduced its own ",(0,n.jsx)(s.code,{children:"Type"})," system.  This\nallows Elide to register and use dynamic models that are not JVM classes."]}),"\n",(0,n.jsx)(s.h3,{id:"modeling",children:"Modeling"}),"\n",(0,n.jsx)(s.p,{children:"CRUD models in Elide are created from JVM classes whereas analytic models are created either from JVM classes or HJSON\nconfiguration files.  In either case, Elide annotations are used to add the metadata Elide needs to perform persistence\nand business rules. All Elide annotations are documented here"}),"\n",(0,n.jsx)(s.h2,{id:"security-subsystem",children:"Security Subsystem"}),"\n",(0,n.jsx)(s.p,{children:"Coming Soon."}),"\n",(0,n.jsx)(s.h2,{id:"analytic-query-subsystem",children:"Analytic Query Subsystem"}),"\n",(0,n.jsx)(s.p,{children:"Coming Soon."})]})}function h(e={}){const{wrapper:s}={...(0,i.R)(),...e.components};return s?(0,n.jsx)(s,{...e,children:(0,n.jsx)(c,{...e})}):c(e)}},2688:(e,s,t)=>{t.d(s,{A:()=>n});const n=t.p+"assets/images/high-level-design-fabdd0eefe231bbe936ac5930cdeec11.png"},8453:(e,s,t)=>{t.d(s,{R:()=>d,x:()=>l});var n=t(6540);const i={},r=n.createContext(i);function d(e){const s=n.useContext(r);return n.useMemo((function(){return"function"==typeof e?e(s):{...s,...e}}),[s,e])}function l(e){let s;return s=e.disableParentContext?"function"==typeof e.components?e.components(i):e.components||i:d(e.components),n.createElement(r.Provider,{value:s},e.children)}}}]);