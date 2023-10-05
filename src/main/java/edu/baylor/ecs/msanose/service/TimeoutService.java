package edu.baylor.ecs.msanose.service;

import edu.baylor.ecs.jparser.component.context.AnalysisContext;
import edu.baylor.ecs.jparser.component.Component;
import edu.baylor.ecs.jparser.component.impl.AnnotationComponent;
import edu.baylor.ecs.jparser.component.impl.ClassComponent;
import edu.baylor.ecs.jparser.component.impl.MethodInfoComponent;
import edu.baylor.ecs.jparser.model.AnnotationValuePair;
import edu.baylor.ecs.rad.context.RequestContext;
import edu.baylor.ecs.rad.service.ResourceService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static edu.baylor.ecs.msanose.service.PersistencyService.forEachValue;

@Service
@AllArgsConstructor
public class TimeoutService {

    private final ResourceService resourceService;

    public boolean hasDefinedTimeout(RequestContext request) {
        List<String> resourcePaths = resourceService.getResourcePaths(request.getPathToCompiledMicroservices());
        if(!checkAgainstPropertiesFile(resourcePaths)){
            return checkAgainstConfigurationClasses(request);
        }
        return true;
    }

    private boolean checkAgainstPropertiesFile(List<String> resourcePaths) {
        for (String path : resourcePaths) {
            Set<Properties> properties = resourceService.getProperties(path, null);
            if (!properties.isEmpty()) {

                Properties prop = properties.iterator().next();
                if (prop.containsKey("spring.httpclient.read-timeout") || prop.containsKey("spring.httpclient.connection-timeout")) {
                    return true;
                }
            } else{
                Map<String, Map<String, Object>> yamls = resourceService.getYamls(path, null);
                if (!yamls.isEmpty()) {
                    for(Map.Entry<String, Map<String, Object>> entry : yamls.entrySet()){
                        Map<String, Object> yamlProperties = new HashMap<>();
                        forEachValue(entry.getValue(), "", yamlProperties::put);
                        if(yamlProperties.get("spring.httpclient.read-timeout") != null || yamlProperties.get("spring.httpclient.connection-timeout") != null){
                            return true;
                        }
                    }
                }
            }

        }
        return false;
    }

    public boolean checkAgainstConfigurationClasses(RequestContext request) {
        String microservicesPath = request.getPathToCompiledMicroservices();

        AnalysisContext analysisContext = JParserService.createContextFromPath(microservicesPath);
        List<ClassComponent> classes = analysisContext.getClasses();
        for (ClassComponent clazz : classes) {
            List<AnnotationComponent> annotationComponents = clazz.getAnnotations().stream().map(Component::asAnnotationComponent).collect(Collectors.toList());
            for (AnnotationComponent annotationComponent : annotationComponents) {
                String annotation = annotationComponent.getAsString();
                if (annotation.matches("@Configuration")) {
                    List<MethodInfoComponent> methods = clazz.getMethods().stream().map(Component::asMethodInfoComponent).collect(Collectors.toList());
                    for (MethodInfoComponent method : methods) {
                        if(method.getReturnType().equals("RestTemplate") && method.getRawSource().contains("setReadTimeout") && method.getRawSource().contains("setConnectTimeout")){
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

}
