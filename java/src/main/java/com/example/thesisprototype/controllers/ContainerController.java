package com.example.thesisprototype.controllers;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1ContainerPort;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.kubernetes.client.util.Config;
import org.springframework.web.bind.annotation.*;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.util.*;

@RestController
public class ContainerController {

    @PostMapping("/containers/createFromYamlString")
    public String createContainerFromYamlString(@RequestBody Map<String, String> requestBody) {
        String yamlString = requestBody.get("yamlData");
        Yaml yaml = new Yaml();
        Map<String, Object> yamlMap = yaml.load(yamlString);

        if (!yamlMap.containsKey("services")) {
            throw new IllegalArgumentException("YAML data does not contain 'services' information");
        }

        Map<String, Object> services = (Map<String, Object>) yamlMap.get("services");

        try {
            ApiClient client = Config.defaultClient();
            CoreV1Api api = new CoreV1Api(client);

            for (Map.Entry<String, Object> serviceEntry : services.entrySet()) {
                String serviceName = serviceEntry.getKey();
                Map<String, Object> serviceDetails = (Map<String, Object>) serviceEntry.getValue();

                String imageName = (String) serviceDetails.get("image");
                List<String> ports = (List<String>) serviceDetails.get("ports");

                V1Container container = new V1Container();
                container.setName(serviceName);
                container.setImage(imageName);

                List<V1ContainerPort> containerPorts = new ArrayList<>();
                for (String port : ports) {
                    String[] portParts = port.split(":");
                    int containerPort = Integer.parseInt(portParts[1]);

                    V1ContainerPort containerPortSpec = new V1ContainerPort();
                    containerPortSpec.setContainerPort(containerPort);
                    containerPorts.add(containerPortSpec);
                }
                container.setPorts(containerPorts);

                V1Pod pod = new V1Pod();
                V1PodSpec podSpec = new V1PodSpec();
                podSpec.setContainers(Collections.singletonList(container));
                V1PodTemplateSpec podTemplateSpec = new V1PodTemplateSpec();
                podTemplateSpec.setSpec(podSpec);

                V1ObjectMeta meta = new V1ObjectMeta();
                meta.setName(serviceName);
                pod.setMetadata(meta);
                pod.setSpec(podSpec);

                api.createNamespacedPod("default", pod, null, null, null);
            }
            return "All containers created and started successfully.";
        } catch (IOException | ApiException e) {
            e.printStackTrace();
            return "Failed to create containers: " + e.getMessage();
        }
    }
}
