[fabrician.org](http://fabrician.org/)
==========================================================================
Dynamic Variable Provider
==========================================================================
 
Introduction
--------------------------------------
This is a Web based implementation of a Dynamic Variable Provider of TIBCO Silver Fabric 
5.5. The project consists of a "Dynamic Variable Provider" running inside Silver Fabric Broker 
and a stand-alone "Web Application" that provides variables. At run time the "Dynamic Variable 
Provider" queries the "Web Application" for variables via an HTTP request. The "Web Application" 
supports multiple variable providers and users can manage them using the Web UI.

The variable rules are maintained in a table layout, for example:
```
[primary key]   [secondary key]  [variable name]   [variable value]
 *               *                PRODUCTION_MODE   true
 example         *                PRODUCTION_MODE   false
```

The "Dynamic Variable Provider" implements the DynamicVariableProvider interface of Silver Fabric 5.5. 
It can pick up two Silver Fabric runtime properties as primary and secondary 
keys. At run time these keys are evaluated inside the Broker and the variable request is submitted to the 
the "Web Server" via an HTTP connection. The "Web Server" evaluates the variable rules and returns a generated 
set of variable values.

Servlet Container
--------------------------------------
* Requires a Serlvet 3.0 container such as Apache Tomcat 7.
* Requires Java 1.7 to run the Servelt Container.

Supported Platforms
--------------------------------------
* Silver Fabric 5.5
* Windows, Linux
* Currently the Web UI is not supported on IE6, IE7 and IE8
 
Required 3rd Libraries before Build
--------------------------------------
* Download SilverFabricSDK.jar from Silver Fabric 5.5, copy it to the current directory. See property "fabric-location" in the pom.xml

Build
--------------------------------------
Needs JDK 1.7 to build the package
```
mvn package
```
look into target/dynamic.variable.provider-1.0-SNAPSHOT.zip
   
Installation
--------------------------------------
Unzip target/dynamic.variable.provider-1.0-SNAPSHOT.zip.
* ./variableProviders contains the "Dynamic Variable Provider" implementation.  It needs to be deployed into a Silver Fabric 5.5 Broker.
* ./webapps contains a '.war' file for the "Web Application".  It must be deployed to a web server that supports the Servlet 3.0 API.
* ./hsqldb contains the sample hsqldb database.  To start it just call either "run.bat" or "run.sh".
 
Configure Dynamic Variable Provider
--------------------------------------
You need to configure the "name", "serverURL", "primaryKey" and "secondaryKey" properties in dynamic_variable_provider.xml.
Here is an example:
```
<dynamicVariableProvider class="org.fabrican.extension.variable.provider.VariableProviderProxy">
    <property name="name" value="DynamicVariableProvider"/>
    <property name="description" value="Dynamic Variable Provider from Fabrician.org"/>
    <property name="enabled" value="true"/>
    <property name="serverUrl" value="http://localhost:8080/dvp/variables/test"/>
    <!--  commons-jexl expressions, must be started with either "engineInfo.", "stackInfo." or "componentInfo." -->
    <property name="primaryKey" value="engineInfo.username"/>
    <!--  commons-jexl expressions, must be started with either "engineInfo.", "stackInfo." or "componentInfo." -->
    <property name="secondaryKey" value="stackInfo.name"/>
</dynamicVariableProvider>
```
In this example:
* you need to create a variable provider "test" in the "Web Application"
* the context of "Web Application" is "dvp"
* you can configure "primaryKey" and "secondaryKey" using "commons-jexl" expressions. The expression must be started 
with either "engineInfo.", "stackInfo." or "componentInfo.

Start Database
--------------------------------------
There is a sample HSQLDB database. To start, run either "hsqldb/run.bat" or "hsqldb/run.sh". Its default listen port is "5000".
The initial user name and password is admin/admin.
Reference hsqldb/vprovider.script if you need to run it on other database servers.

Running the Web Application
--------------------------------------
The URL of "Web Application" is 
```
http://<host>:<port>/<context-path>
```
Since I have deployed "Dynamic Variable Provider" with name=test. I need to create a new Variable Provider with name "test"
in the "Web Application" UI.

At the first time the "Web Application" is started, you need to configure JDBC connection using the "Configure" menu item.
for example:
```
Url=jdbc:hsqldb:hsql://localhost:5000/vprovider
Driver=org.hsqldb.jdbcDriver
Username=admin
Password=admin
```
The connection properties are stored using Java Preference APIs. With proper Server security policy it can be retrievable when
the web server restarts. 

The webapp can be also run locally with Jetty.  The application URL with Jetty will be ``` http://<host>:<port> ```.
```
mvn jetty:run-war
```

Variable Rules
--------------------------------------
The variable rules are maintained in a table layout, for example:
```
[primary key]   [secondary key]  [variable name]   [variable value]
 *               *                PRODUCTION_MODE   true
 example         *                PRODUCTION_MODE   false 
``` 
The cells in [primary key] and [secondary key] columns can be:
* '*'            -- matches key value
*  String literal -- matches if it's contained in the key value
*  [5, 100)       -- numerical range, 5 <= x < 100. (,100) and [5, ] are also accepted

Where there are multiple matches on the same variable name: 
* rules with more specific (that is, none *) cell values takes the precedence;
* rules with more specific value in the primary key cell takes the precedence over rules with none * value in secondary;
* otherwise, the last rule takes precedence. 
key cell. 
  

Using the Variables
--------------------------------------
Create a variable in your Component or Enabler that retrieves the value from the provider.
```
${<name_of_provider>.getProperty('<name_of_property>')}
```
For example:
```
${DynamicVariableProvider.getProperty('PRODUCTION_MODE')}
```
