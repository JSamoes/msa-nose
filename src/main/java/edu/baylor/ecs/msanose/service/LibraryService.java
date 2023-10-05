package edu.baylor.ecs.msanose.service;

import edu.baylor.ecs.msanose.model.SharedLibrary;
import edu.baylor.ecs.msanose.model.context.SharedLibraryContext;
import edu.baylor.ecs.rad.context.RequestContext;
import edu.baylor.ecs.rad.service.ResourceService;
import lombok.AllArgsConstructor;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@AllArgsConstructor
public class LibraryService {

    private ResourceService resourceService;

    private static final List<String> GATEWAY_LIBRARIES = Collections.unmodifiableList(
            new ArrayList<String>() {{
                add("org.springframework.cloud:spring-cloud-starter-gateway");
                add("com.google.cloud:google-cloud-service-control");
                add("com.google.cloud:google-cloud-api-gateway");
                add("software.amazon.awssdk:bom");
                add("software.amazon.awssdk:apigatewaymanagementapi");
                add("software.amazon.awssdk:apigateway");
                add("software.amazon.awssdk:apigatewayv2");
                add("io.gravitee.gateway:gravitee-gateway-api");
                add("org.apache.camel:camel-gateway");
                add("org.apache.camel:camel-gateway-http");
                add("org.wso2.carbon.apimgt:org.wso2.carbon.apimgt.gateway");
            }});


    public SharedLibraryContext getSharedLibraries(RequestContext request) throws IOException, XmlPullParserException {

        List<String> fileNames = resourceService.getPomXML(request.getPathToCompiledMicroservices());
        SharedLibraryContext sharedLibraryContext = new SharedLibraryContext();
        MavenXpp3Reader reader = new MavenXpp3Reader();
        for(int i = 0; i < fileNames.size() - 1; i++){
            for (int j = i + 1; j < fileNames.size(); j++) {

                Model modelA = reader.read(new FileReader(fileNames.get(i)));
                Model modelB = reader.read(new FileReader(fileNames.get(j)));

                for(Dependency dependencyA : modelA.getDependencies()){
                    boolean matched = false;
                    for (Dependency dependencyB : modelB.getDependencies()) {
                        if(dependencyA.getArtifactId().equals(dependencyB.getArtifactId()) && dependencyA.getGroupId().equals(dependencyB.getGroupId())){
                            matched = true;
                            String msaA = modelA.getGroupId() + ":" + modelA.getArtifactId();
                            String msaB = modelB.getGroupId() + ":" + modelB.getArtifactId();

                            String library = dependencyA.getGroupId() + ":" + dependencyA.getArtifactId();
                            SharedLibrary sharedLibrary = sharedLibraryContext.getOrDefault(library);
                            sharedLibrary.add(msaA, msaB);
                            sharedLibraryContext.addSharedLibrary(sharedLibrary);
                            break;
                        }
                    }

                    if(matched){
                        break;
                    }
                }
            }
        }

        return sharedLibraryContext;
    }

    public boolean hasGateway(RequestContext request) throws IOException, XmlPullParserException {
        List<String> fileNames = resourceService.getPomXML(request.getPathToCompiledMicroservices());
        MavenXpp3Reader reader = new MavenXpp3Reader();
        for(int i = 0; i < fileNames.size() - 1; i++){
            Model modelA = reader.read(new FileReader(fileNames.get(i)));
            List<String> dependencies = modelA.getDependencies().stream().map(x -> String.format("%s:%s", x.getGroupId(), x.getArtifactId())).collect(Collectors.toList());
            List<String> gatewayDep = dependencies.stream().filter(GATEWAY_LIBRARIES::contains).collect(Collectors.toList());
            if(!gatewayDep.isEmpty()){
                return true;
            }
        }
        List<String> resourcePaths = resourceService.getResourcePaths(request.getPathToCompiledMicroservices());
        for(String path : resourcePaths){
            Set<Properties> propertiesSet = resourceService.getProperties(path, request.getOrganizationPath());
            for (Properties properties: propertiesSet) {
                String gateway = properties.getProperty("spring.security.oauth2.client.registration.wso2");
                if(gateway != null){
                    return true;
                }
            }
        }
        List<String> envoyGateway = resourceService.getAllYamlFilesInPath(request.getPathToCompiledMicroservices()).stream().filter(yaml -> yaml.contains("envoy.yaml") || yaml.contains("envoy.yml")).collect(Collectors.toList());
        if(!envoyGateway.isEmpty()){
            return true;
        }
        List<String> jsonGatewayConfFiles = resourceService.getAllJsonFilesInPath(request.getPathToCompiledMicroservices()).stream().filter(json -> json.contains("envoy.json") || json.contains("krakend.json")).collect(Collectors.toList());
        if(!jsonGatewayConfFiles.isEmpty()){
            return true;
        }
        List<String> nginxConfFiles = resourceService.getAllExtensionFilesInPath(".conf", request.getPathToCompiledMicroservices()).stream().filter(conf -> conf.contains("nginx.conf")).collect(Collectors.toList());
        if(!nginxConfFiles.isEmpty()){
            return true;
        }
        List<String> haProxyConfFiles = resourceService.getAllExtensionFilesInPath(".cfg", request.getPathToCompiledMicroservices()).stream().filter(conf -> conf.contains("haproxy.cfg")).collect(Collectors.toList());
        if(!haProxyConfFiles.isEmpty()){
            return true;
        }
        List<String> dockerComposes = resourceService.getAllYamlFilesInPath(request.getPathToCompiledMicroservices()).stream().filter(yaml -> yaml.contains("docker-compose.yaml") || yaml.contains("docker-compose.yml")).collect(Collectors.toList());
        if(!dockerComposes.isEmpty()){
            for (String dockerCompose : dockerComposes) {
                List<String> lines = resourceService.readAllLines(dockerCompose);
                for (String line : lines) {
                    if(line.contains("kong") || line.contains("traefik") ){
                        return true;
                    }
                }

            }
        }

        return false;
    }
}
