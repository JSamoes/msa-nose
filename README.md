# MSANose

This library is built using Spring Boot to detect 14 code smells in microservice applications.

## Getting Started

These instructions will get you a copy of the project up and running.

### Prerequisites

There are a couple prerequisites to cover before running.

#### Installing ws4j

1. [Download the Jar](https://code.google.com/archive/p/ws4j/downloads)
2. Add the jar to your local mvn repository

```
 mvn install:install-file -Dfile=<path-to-jar> -DgroupId=com.sciss -DartifactId=ws4j -Dversion=1.0.1 -Dpackaging=jar 
```
#### Lombok

MSANose uses [Lombok](https://projectlombok.org/) for generating getters/setters. If your IDE is not configured for annotation processing then you might see the IDE reporting errors on getters/setters such as "method not found". The application should still run successfully, but you will need to follow the directions for your IDE to install the necessary plugins/annotation processor to get rid of the errors.

## Endpoints

In this section, there are brief overviews of each endpoint.

### /api/v1/report 

This endpoint will run all of the other endpoints and aggregate them into a single report object. Additionally, it will time each code-smell detection and report their times.

### /api/v1/apis 

This endpoint will find all of the unversioned APIs and report them as a list.

### /api/v1/sharedLibraries 

This endpoint will find all of the shared libraries and report them as a list.

### /api/v1/wrongCuts 

This endpoint will detect any microservice that is cut wrongly. It will also find the number of entities in each microservice.

### /api/v1/cyclicDependency 

This endpoint will give a boolean that is true if any cycles are detected in the microservice dependency graph.

### /api/v1/sharedPersistency 

This endpoint will find any shared persistencies and report a list of the offending microservices and their shared persistency.

### /api/v1/esbUsage 

This endpoint will return a list of potential ESB microservices.

### /api/v1/noAPIGateway 

This endpoint will return true if an API gateway is used.

### /api/v1/inappropriateServiceIntimacy 

This endpoint will return a list of inappropriately similar microservices along with their similarity scores.

### /api/v1/tooManyStandards 

This endpoint will return the standards used by the application for the presentation, business and data layers.

### /api/v1/microservicesGreedy 

This endpoint will return a list of greedy microservices.

### /api/v1/timeout

This endpoint will return true if the microservice has timeout defined.

### /api/v1/health-check

This endpoint will return a list of microservices controllers that do not have a health check endpoint.

### /api/v1/microserviceSize

This endpoint will return a list of microservices controllers name that either are too big (mega service) or too short (nano service) based on the total of microservice lines.

## Using the Endpoints

Each of the endpoints is called using a POST operation with the body as follows:

```
{
    "pathToCompiledMicroservices": "/<path-to-microservices>/",
    "organizationPath": "",
    "outputPath": ""
}
```

\* At the moment `outputPath` is a WIP.

## Authors

* [**Andrew Walker**](https://github.com/walker76)
* [**João Samões**](https://github.com/JSamoes)

## Acknowledgments

This material is based upon work supported by the National Science Foundation under Grant No. 1854049 and a grant from [Red Hat Research](https://research.redhat.com).
The improvements done in this project were made to support a master thesis in the [Instituto Superior de Engenharia do Porto](https://www.ipp.pt/?set_language=en).
