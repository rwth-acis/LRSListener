# LRSListener

This system is used as a mediator between a LMS e.g. Moodle and the Gamification Framework to bring gamification functionalities to the LMS.

## Prequisites
- Java 14 or newer
- Gradle 6.8 (exactly)
- PostgreSQL

## Building
In the root folder execute ./gradlew build to build the project and ./gradlew clean to delete all artificats. The LRSListener and ListenerConfigurator have seperate gradle.build and gradle.properties files to allow manipulation of building behaviour and customization. Dependencies are stated in the gradle.build files.

## Starting 
Before the deployment of components the PostgreSQL >configurator has to be existing. [dbcreation.sql](ListenerConfigurator/psql/dbcreation.sql) creates the >configurator role, the >configuration database and assigns it to the configurator. [db.sql](ListenerConfigurator/psql/db.sql) creates the required schemas and tables to operate the ListenerConfigurator and the LRSListener.

Each component can be started seperatly, by navigationg to the the component’s project directory and executing *./etc/bin./start_network.sh*. *It is very important, that the script is executed from the components project directory*. Once one component is running, the any other las2peer service, which has to run in the same network has to be >bootstrapped to the network. A component can join the network, by exuting ./etc/bin/join_network.sh *from the components project directory*

## Configuration
For the LRSListener there is a [properties file](LRSListener/etc/i5.las2peer.services.gamification.listener.LRSListener.properties) to provide configure different LRS, LMS and Configurators. Just be with trailing slashes carefull, when configuring urls.
