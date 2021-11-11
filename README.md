# LRSListener

This system is used as a mediator between a LMS e.g. Moodle and the Gamification Framework to bring gamification functionalities to the LMS.

## Prequisites
- Java 14 or newer
- Gradle 6.8 (exactly)
- PostgreSQL

## Building
In the root folder execute ./gradlew build to build the project and ./gradlew clean to delete all artificats. The LRSListener and ListenerConfigurator have seperate gradle.build and gradle.properties files to allow manipulation of building behaviour and customization. Dependencies are stated in the gradle.build files.

## Starting 
Before the deployment of components the PostgreSQL >configurator has to be existing. [dbcreation.sql](ListenerConfigurator/psql)
