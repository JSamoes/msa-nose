package edu.baylor.ecs.msanose.service;

import edu.baylor.ecs.jparser.component.Component;
import edu.baylor.ecs.jparser.component.context.AnalysisContext;
import edu.baylor.ecs.jparser.component.impl.AnnotationComponent;
import edu.baylor.ecs.jparser.component.impl.ClassComponent;
import edu.baylor.ecs.jparser.component.impl.MethodInfoComponent;
import edu.baylor.ecs.jparser.model.AnnotationValuePair;
import edu.baylor.ecs.msanose.model.MicroserviceHealthcheckDTO;
import edu.baylor.ecs.msanose.model.context.APIContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class APIService {

    public List<APIContext> getAPIs(String path){
        List<APIContext> apis = new ArrayList<>();

        AnalysisContext analysisContext = JParserService.createContextFromPath(path);
        List<ClassComponent> classes = analysisContext.getClasses();
        classes.forEach(clazz -> {
            List<AnnotationComponent> annotationComponents = clazz.getAnnotations().stream().map(Component::asAnnotationComponent).collect(Collectors.toList());
            for(AnnotationComponent annotationComponent : annotationComponents) {
                getPathsFromClass(clazz, apis, annotationComponent);
            }
        });

        return apis;
    }

    public Map<String, List<APIContext>> getApisPerMicroservice(String path){
        Map<String, List<APIContext>> apisPerMicro = new HashMap<>();

        AnalysisContext analysisContext = JParserService.createContextFromPath(path);
        List<ClassComponent> classes = analysisContext.getClasses();
        classes.forEach(clazz -> {
            List<AnnotationComponent> annotationComponents = clazz.getAnnotations().stream().map(Component::asAnnotationComponent).collect(Collectors.toList());

            for(AnnotationComponent annotationComponent : annotationComponents) {
                List<APIContext> apis = getPathsFromClass(clazz, new ArrayList<>(), annotationComponent);
                if(!apis.isEmpty()){
                    if(apisPerMicro.containsKey(clazz.getPackageName())){
                        long numberOfClasses = apisPerMicro.keySet().stream().filter(x -> x.equals(clazz.getPackageName())).count() + 1;
                        apisPerMicro.put(clazz.getPackageName() + numberOfClasses, apis);
                    }else{
                        apisPerMicro.put(clazz.getPackageName(), apis);
                    }
                }
            }
        });
        apisPerMicro.entrySet().stream().map(x -> new MicroserviceHealthcheckDTO(x.getKey(), x.getValue().stream().map(APIContext::getPath).anyMatch(this::hasHealthcheckEndpoint)));
        for (Map.Entry<String, List<APIContext>> a: apisPerMicro.entrySet()) {

        }
        return apisPerMicro;
    }

    private List<APIContext> getPathsFromClass(ClassComponent clazz, List<APIContext> apis, AnnotationComponent annotationComponent) {
        String annotation = annotationComponent.getAsString();
        if (annotation.matches("@RequestMapping|@RestController")) {
            String classLevelPath = "";
            if(annotationComponent.getAnnotationValue() != null){
                if(!annotationComponent.getAnnotationValue().equals("RestController")){
                    classLevelPath = annotationComponent.getAnnotationValue();
                }
            }
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
                                    String apiPath = (classLevelPath + valuePair.getValue()).replace("\"", "");
                                    apis.add(new APIContext(apiPath));
                                }
                            }
                        } else {
                            String apiPath = (classLevelPath + methodLevelPath).replace("\"", "");
                            apis.add(new APIContext(apiPath));
                        }
                    }
                }
            }
        }
        return apis;
    }

    public boolean isVersioned(String api){
        return api.matches("/?api/v[0-9]+.*");
    }

    public boolean hasHealthcheckEndpoint(String api){
        return api.matches("/?(health|healthcheck|health-check)+");
    }

}
