
package com.thesis.backend.service;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.*;
import org.springframework.stereotype.Service;

@Service
public class KubernetesService {

    private final KubernetesClient client = new KubernetesClientBuilder().build();

    public String createContainer(String image, String username) {
        String podName = "pod-" + username + "-" + System.currentTimeMillis();
        Pod pod = new PodBuilder()
                .withNewMetadata()
                .withName(podName)
                .endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withName("main")
                .withImage(image)
                .endContainer()
                .withRestartPolicy("Never")
                .endSpec()
                .build(); // build the Pod object

        client.pods().inNamespace("default").create(pod);
        return pod.getMetadata().getName();
    }
}
