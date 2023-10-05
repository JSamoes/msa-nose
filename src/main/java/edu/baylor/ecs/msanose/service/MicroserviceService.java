package edu.baylor.ecs.msanose.service;

import edu.baylor.ecs.jparser.component.Component;
import edu.baylor.ecs.jparser.component.context.AnalysisContext;
import edu.baylor.ecs.jparser.component.impl.AnnotationComponent;
import edu.baylor.ecs.jparser.component.impl.ClassComponent;
import edu.baylor.ecs.jparser.component.impl.MethodInfoComponent;
import edu.baylor.ecs.jparser.model.AnnotationValuePair;
import edu.baylor.ecs.msanose.model.context.MicroserviceSizeContext;
import edu.baylor.ecs.rad.context.RequestContext;
import edu.baylor.ecs.rad.service.ResourceService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class MicroserviceService {

    private final ResourceService resourceService;
    private static final Integer MAX_MICROSERVICE_LINE_SIZE = 2000;

    public MicroserviceSizeContext getMicroserviceSize(RequestContext request) {
        String microservicesPath = request.getPathToCompiledMicroservices();
        Set<String> apis = new HashSet<>();

        AnalysisContext analysisContext = JParserService.createContextFromPath(microservicesPath);
        List<ClassComponent> classes = analysisContext.getClasses();
        classes.forEach(clazz -> {
            List<AnnotationComponent> annotationComponents = clazz.getAnnotations().stream().map(Component::asAnnotationComponent).collect(Collectors.toList());
            for(AnnotationComponent annotationComponent : annotationComponents) {
                String annotation = annotationComponent.getAsString();
                if (annotation.matches("@RequestMapping|@RestController")) {
                    List<MethodInfoComponent> methods = clazz.getMethods().stream().map(Component::asMethodInfoComponent).collect(Collectors.toList());
                    for(MethodInfoComponent method : methods){
                        List<AnnotationComponent> methodAnnotationComponents = method.getAnnotations().stream().map(Component::asAnnotationComponent).collect(Collectors.toList());
                        for(AnnotationComponent methodAnnotationComponent : methodAnnotationComponents) {
                            if (methodAnnotationComponent.getAsString().contains("Mapping")) {
                                String methodLevelPath = null;
                                if(methodAnnotationComponent.getAnnotationValue() != null){
                                    methodLevelPath =  methodAnnotationComponent.getAnnotationValue().endsWith("Mapping") ? "" : methodAnnotationComponent.getAnnotationValue();
                                }

                                if(methodLevelPath == null) {
                                    List<AnnotationValuePair> annotationValuePairList = methodAnnotationComponent.getAnnotationValuePairList();
                                    for (AnnotationValuePair valuePair : annotationValuePairList) {
                                        if (valuePair.getKey().equals("path")) {
                                            apis.add(clazz.getPath());
                                        }
                                    }
                                } else {
                                    apis.add(clazz.getPath());
                                }
                            }
                        }
                    }
                }
            }
        });
        Set<String> pathCodeMicroservices = apis.stream().map(x -> x.substring(0, x.indexOf("main\\")) + "main").collect(Collectors.toSet());

        HashMap<String, Integer> map = countNumberOfLinesInPath(pathCodeMicroservices);

        List<String> megaServices = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (entry.getValue() > MAX_MICROSERVICE_LINE_SIZE) {
                megaServices.add(entry.getKey());
            }
        }

        return new MicroserviceSizeContext(megaServices);
    }

    private HashMap<String, Integer> countNumberOfLinesInPath(Set<String> pathCodeMicroservices) {
        HashMap<String, Integer> map = new HashMap<>();
        for (String path : pathCodeMicroservices) {
            int count = 0;
            List<String> files = resourceService.getAllFiles(path);
            for (String file : files) {
                count += resourceService.countLines(file);
            }
            map.put(path, count);
        }
        return map;
    }
}
