package com.thesis.backend.kubernetes.config;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KubernetesConfig {

    @Value("${kubernetes.master:https://kubernetes.default.svc}")
    private String masterUrl;

    @Value("${kubernetes.namespace:default}")
    private String namespace;

    @Value("${kubernetes.auth.token:}")
    private String token;

    @Value("${kubernetes.auth.certs.ca.file:/var/run/secrets/kubernetes.io/serviceaccount/ca.crt}")
    private String caCertFile;

    @Value("${kubernetes.trust.certificates:true}")
    private Boolean trustCertificates;

    @Bean
    public KubernetesClient kubernetesClient() {
        Config config = new ConfigBuilder()
                .withMasterUrl(masterUrl)
                .withNamespace(namespace)
                .withOauthToken(token)
                .withCaCertFile(caCertFile)
                .withTrustCerts(trustCertificates)
                .build();

        return new KubernetesClientBuilder().withConfig(config).build();
    }
}
