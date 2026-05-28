
package org.jboss.intersmash.model.helm.charts.values.eap82;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * Configuration to deploy the application
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "annotations",
    "enabled",
    "replicas",
    "labels",
    "resources",
    "env",
    "envFrom",
    "route",
    "tls",
    "livenessProbe",
    "readinessProbe",
    "startupProbe",
    "volumeMounts",
    "volumes",
    "initContainers",
    "extraContainers",
    "imagePullSecrets:"
})
@Generated("jsonschema2pojo")
public class Deploy {

    /**
     * Annotations that are applied to the deployed application and its pods
     * 
     */
    @JsonProperty("annotations")
    @JsonPropertyDescription("Annotations that are applied to the deployed application and its pods")
    private Annotations annotations;
    /**
     * Enable/Disable deploying the application image
     * 
     */
    @JsonProperty("enabled")
    @JsonPropertyDescription("Enable/Disable deploying the application image")
    private Boolean enabled = true;
    /**
     * Number of pod replicas to deploy
     * 
     */
    @JsonProperty("replicas")
    @JsonPropertyDescription("Number of pod replicas to deploy")
    private Integer replicas;
    /**
     * Labels that are applied to the deployed application and its pods
     * 
     */
    @JsonProperty("labels")
    @JsonPropertyDescription("Labels that are applied to the deployed application and its pods")
    private Labels labels;
    /**
     * Freeform resources requirements to deploy the application image
     * 
     */
    @JsonProperty("resources")
    @JsonPropertyDescription("Freeform resources requirements to deploy the application image")
    private Object resources;
    /**
     * List of environment variables to set in the container. Cannot be updated.
     * 
     */
    @JsonProperty("env")
    @JsonPropertyDescription("List of environment variables to set in the container. Cannot be updated.")
    private List<Env__1> env = new ArrayList<Env__1>();
    /**
     * List of sources to populate environment variables in the container. The keys defined within a source must be a C_IDENTIFIER. All invalid keys will be reported as an event when the container is starting. When a key exists in multiple sources, the value associated with the last source will take precedence. Values defined by an Env with a duplicate key will take precedence. Cannot be updated.
     * 
     */
    @JsonProperty("envFrom")
    @JsonPropertyDescription("List of sources to populate environment variables in the container. The keys defined within a source must be a C_IDENTIFIER. All invalid keys will be reported as an event when the container is starting. When a key exists in multiple sources, the value associated with the last source will take precedence. Values defined by an Env with a duplicate key will take precedence. Cannot be updated.")
    private List<EnvFrom> envFrom = new ArrayList<EnvFrom>();
    /**
     * Route configuration
     * 
     */
    @JsonProperty("route")
    @JsonPropertyDescription("Route configuration")
    private Route route;
    /**
     * TLS Configuration
     * 
     */
    @JsonProperty("tls")
    @JsonPropertyDescription("TLS Configuration")
    private Tls__1 tls;
    /**
     * Freeform livenessProbe configuration
     * 
     */
    @JsonProperty("livenessProbe")
    @JsonPropertyDescription("Freeform livenessProbe configuration")
    private Object livenessProbe;
    /**
     * Freeform readinessProbe configuration
     * 
     */
    @JsonProperty("readinessProbe")
    @JsonPropertyDescription("Freeform readinessProbe configuration")
    private Object readinessProbe;
    /**
     * Freeform startupProbe configuration
     * 
     */
    @JsonProperty("startupProbe")
    @JsonPropertyDescription("Freeform startupProbe configuration")
    private Object startupProbe;
    /**
     * Freeform array of volumeMounts
     * 
     */
    @JsonProperty("volumeMounts")
    @JsonPropertyDescription("Freeform array of volumeMounts")
    private List<VolumeMount> volumeMounts = new ArrayList<VolumeMount>();
    /**
     * Freeform array of volumes
     * 
     */
    @JsonProperty("volumes")
    @JsonPropertyDescription("Freeform array of volumes")
    private List<Object> volumes = new ArrayList<Object>();
    /**
     * Freeform array of initContainers
     * 
     */
    @JsonProperty("initContainers")
    @JsonPropertyDescription("Freeform array of initContainers")
    private List<Object> initContainers = new ArrayList<Object>();
    /**
     * Freeform array of extra containers
     * 
     */
    @JsonProperty("extraContainers")
    @JsonPropertyDescription("Freeform array of extra containers")
    private List<Object> extraContainers = new ArrayList<Object>();
    /**
     * ImagePullSecrets is a list of references to secrets in the same namespace to use for pulling the application image
     * 
     */
    @JsonProperty("imagePullSecrets:")
    @JsonPropertyDescription("ImagePullSecrets is a list of references to secrets in the same namespace to use for pulling the application image")
    private List<ImagePullSecrets> imagePullSecrets = new ArrayList<ImagePullSecrets>();
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Annotations that are applied to the deployed application and its pods
     * 
     */
    @JsonProperty("annotations")
    public Annotations getAnnotations() {
        return annotations;
    }

    /**
     * Annotations that are applied to the deployed application and its pods
     * 
     */
    @JsonProperty("annotations")
    public void setAnnotations(Annotations annotations) {
        this.annotations = annotations;
    }

    public Deploy withAnnotations(Annotations annotations) {
        this.annotations = annotations;
        return this;
    }

    /**
     * Enable/Disable deploying the application image
     * 
     */
    @JsonProperty("enabled")
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * Enable/Disable deploying the application image
     * 
     */
    @JsonProperty("enabled")
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Deploy withEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Number of pod replicas to deploy
     * 
     */
    @JsonProperty("replicas")
    public Integer getReplicas() {
        return replicas;
    }

    /**
     * Number of pod replicas to deploy
     * 
     */
    @JsonProperty("replicas")
    public void setReplicas(Integer replicas) {
        this.replicas = replicas;
    }

    public Deploy withReplicas(Integer replicas) {
        this.replicas = replicas;
        return this;
    }

    /**
     * Labels that are applied to the deployed application and its pods
     * 
     */
    @JsonProperty("labels")
    public Labels getLabels() {
        return labels;
    }

    /**
     * Labels that are applied to the deployed application and its pods
     * 
     */
    @JsonProperty("labels")
    public void setLabels(Labels labels) {
        this.labels = labels;
    }

    public Deploy withLabels(Labels labels) {
        this.labels = labels;
        return this;
    }

    /**
     * Freeform resources requirements to deploy the application image
     * 
     */
    @JsonProperty("resources")
    public Object getResources() {
        return resources;
    }

    /**
     * Freeform resources requirements to deploy the application image
     * 
     */
    @JsonProperty("resources")
    public void setResources(Object resources) {
        this.resources = resources;
    }

    public Deploy withResources(Object resources) {
        this.resources = resources;
        return this;
    }

    /**
     * List of environment variables to set in the container. Cannot be updated.
     * 
     */
    @JsonProperty("env")
    public List<Env__1> getEnv() {
        return env;
    }

    /**
     * List of environment variables to set in the container. Cannot be updated.
     * 
     */
    @JsonProperty("env")
    public void setEnv(List<Env__1> env) {
        this.env = env;
    }

    public Deploy withEnv(List<Env__1> env) {
        this.env = env;
        return this;
    }

    /**
     * List of sources to populate environment variables in the container. The keys defined within a source must be a C_IDENTIFIER. All invalid keys will be reported as an event when the container is starting. When a key exists in multiple sources, the value associated with the last source will take precedence. Values defined by an Env with a duplicate key will take precedence. Cannot be updated.
     * 
     */
    @JsonProperty("envFrom")
    public List<EnvFrom> getEnvFrom() {
        return envFrom;
    }

    /**
     * List of sources to populate environment variables in the container. The keys defined within a source must be a C_IDENTIFIER. All invalid keys will be reported as an event when the container is starting. When a key exists in multiple sources, the value associated with the last source will take precedence. Values defined by an Env with a duplicate key will take precedence. Cannot be updated.
     * 
     */
    @JsonProperty("envFrom")
    public void setEnvFrom(List<EnvFrom> envFrom) {
        this.envFrom = envFrom;
    }

    public Deploy withEnvFrom(List<EnvFrom> envFrom) {
        this.envFrom = envFrom;
        return this;
    }

    /**
     * Route configuration
     * 
     */
    @JsonProperty("route")
    public Route getRoute() {
        return route;
    }

    /**
     * Route configuration
     * 
     */
    @JsonProperty("route")
    public void setRoute(Route route) {
        this.route = route;
    }

    public Deploy withRoute(Route route) {
        this.route = route;
        return this;
    }

    /**
     * TLS Configuration
     * 
     */
    @JsonProperty("tls")
    public Tls__1 getTls() {
        return tls;
    }

    /**
     * TLS Configuration
     * 
     */
    @JsonProperty("tls")
    public void setTls(Tls__1 tls) {
        this.tls = tls;
    }

    public Deploy withTls(Tls__1 tls) {
        this.tls = tls;
        return this;
    }

    /**
     * Freeform livenessProbe configuration
     * 
     */
    @JsonProperty("livenessProbe")
    public Object getLivenessProbe() {
        return livenessProbe;
    }

    /**
     * Freeform livenessProbe configuration
     * 
     */
    @JsonProperty("livenessProbe")
    public void setLivenessProbe(Object livenessProbe) {
        this.livenessProbe = livenessProbe;
    }

    public Deploy withLivenessProbe(Object livenessProbe) {
        this.livenessProbe = livenessProbe;
        return this;
    }

    /**
     * Freeform readinessProbe configuration
     * 
     */
    @JsonProperty("readinessProbe")
    public Object getReadinessProbe() {
        return readinessProbe;
    }

    /**
     * Freeform readinessProbe configuration
     * 
     */
    @JsonProperty("readinessProbe")
    public void setReadinessProbe(Object readinessProbe) {
        this.readinessProbe = readinessProbe;
    }

    public Deploy withReadinessProbe(Object readinessProbe) {
        this.readinessProbe = readinessProbe;
        return this;
    }

    /**
     * Freeform startupProbe configuration
     * 
     */
    @JsonProperty("startupProbe")
    public Object getStartupProbe() {
        return startupProbe;
    }

    /**
     * Freeform startupProbe configuration
     * 
     */
    @JsonProperty("startupProbe")
    public void setStartupProbe(Object startupProbe) {
        this.startupProbe = startupProbe;
    }

    public Deploy withStartupProbe(Object startupProbe) {
        this.startupProbe = startupProbe;
        return this;
    }

    /**
     * Freeform array of volumeMounts
     * 
     */
    @JsonProperty("volumeMounts")
    public List<VolumeMount> getVolumeMounts() {
        return volumeMounts;
    }

    /**
     * Freeform array of volumeMounts
     * 
     */
    @JsonProperty("volumeMounts")
    public void setVolumeMounts(List<VolumeMount> volumeMounts) {
        this.volumeMounts = volumeMounts;
    }

    public Deploy withVolumeMounts(List<VolumeMount> volumeMounts) {
        this.volumeMounts = volumeMounts;
        return this;
    }

    /**
     * Freeform array of volumes
     * 
     */
    @JsonProperty("volumes")
    public List<Object> getVolumes() {
        return volumes;
    }

    /**
     * Freeform array of volumes
     * 
     */
    @JsonProperty("volumes")
    public void setVolumes(List<Object> volumes) {
        this.volumes = volumes;
    }

    public Deploy withVolumes(List<Object> volumes) {
        this.volumes = volumes;
        return this;
    }

    /**
     * Freeform array of initContainers
     * 
     */
    @JsonProperty("initContainers")
    public List<Object> getInitContainers() {
        return initContainers;
    }

    /**
     * Freeform array of initContainers
     * 
     */
    @JsonProperty("initContainers")
    public void setInitContainers(List<Object> initContainers) {
        this.initContainers = initContainers;
    }

    public Deploy withInitContainers(List<Object> initContainers) {
        this.initContainers = initContainers;
        return this;
    }

    /**
     * Freeform array of extra containers
     * 
     */
    @JsonProperty("extraContainers")
    public List<Object> getExtraContainers() {
        return extraContainers;
    }

    /**
     * Freeform array of extra containers
     * 
     */
    @JsonProperty("extraContainers")
    public void setExtraContainers(List<Object> extraContainers) {
        this.extraContainers = extraContainers;
    }

    public Deploy withExtraContainers(List<Object> extraContainers) {
        this.extraContainers = extraContainers;
        return this;
    }

    /**
     * ImagePullSecrets is a list of references to secrets in the same namespace to use for pulling the application image
     * 
     */
    @JsonProperty("imagePullSecrets:")
    public List<ImagePullSecrets> getImagePullSecrets() {
        return imagePullSecrets;
    }

    /**
     * ImagePullSecrets is a list of references to secrets in the same namespace to use for pulling the application image
     * 
     */
    @JsonProperty("imagePullSecrets:")
    public void setImagePullSecrets(List<ImagePullSecrets> imagePullSecrets) {
        this.imagePullSecrets = imagePullSecrets;
    }

    public Deploy withImagePullSecrets(List<ImagePullSecrets> imagePullSecrets) {
        this.imagePullSecrets = imagePullSecrets;
        return this;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public Deploy withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Deploy.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("annotations");
        sb.append('=');
        sb.append(((this.annotations == null)?"<null>":this.annotations));
        sb.append(',');
        sb.append("enabled");
        sb.append('=');
        sb.append(((this.enabled == null)?"<null>":this.enabled));
        sb.append(',');
        sb.append("replicas");
        sb.append('=');
        sb.append(((this.replicas == null)?"<null>":this.replicas));
        sb.append(',');
        sb.append("labels");
        sb.append('=');
        sb.append(((this.labels == null)?"<null>":this.labels));
        sb.append(',');
        sb.append("resources");
        sb.append('=');
        sb.append(((this.resources == null)?"<null>":this.resources));
        sb.append(',');
        sb.append("env");
        sb.append('=');
        sb.append(((this.env == null)?"<null>":this.env));
        sb.append(',');
        sb.append("envFrom");
        sb.append('=');
        sb.append(((this.envFrom == null)?"<null>":this.envFrom));
        sb.append(',');
        sb.append("route");
        sb.append('=');
        sb.append(((this.route == null)?"<null>":this.route));
        sb.append(',');
        sb.append("tls");
        sb.append('=');
        sb.append(((this.tls == null)?"<null>":this.tls));
        sb.append(',');
        sb.append("livenessProbe");
        sb.append('=');
        sb.append(((this.livenessProbe == null)?"<null>":this.livenessProbe));
        sb.append(',');
        sb.append("readinessProbe");
        sb.append('=');
        sb.append(((this.readinessProbe == null)?"<null>":this.readinessProbe));
        sb.append(',');
        sb.append("startupProbe");
        sb.append('=');
        sb.append(((this.startupProbe == null)?"<null>":this.startupProbe));
        sb.append(',');
        sb.append("volumeMounts");
        sb.append('=');
        sb.append(((this.volumeMounts == null)?"<null>":this.volumeMounts));
        sb.append(',');
        sb.append("volumes");
        sb.append('=');
        sb.append(((this.volumes == null)?"<null>":this.volumes));
        sb.append(',');
        sb.append("initContainers");
        sb.append('=');
        sb.append(((this.initContainers == null)?"<null>":this.initContainers));
        sb.append(',');
        sb.append("extraContainers");
        sb.append('=');
        sb.append(((this.extraContainers == null)?"<null>":this.extraContainers));
        sb.append(',');
        sb.append("imagePullSecrets");
        sb.append('=');
        sb.append(((this.imagePullSecrets == null)?"<null>":this.imagePullSecrets));
        sb.append(',');
        sb.append("additionalProperties");
        sb.append('=');
        sb.append(((this.additionalProperties == null)?"<null>":this.additionalProperties));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.livenessProbe == null)? 0 :this.livenessProbe.hashCode()));
        result = ((result* 31)+((this.extraContainers == null)? 0 :this.extraContainers.hashCode()));
        result = ((result* 31)+((this.replicas == null)? 0 :this.replicas.hashCode()));
        result = ((result* 31)+((this.imagePullSecrets == null)? 0 :this.imagePullSecrets.hashCode()));
        result = ((result* 31)+((this.volumes == null)? 0 :this.volumes.hashCode()));
        result = ((result* 31)+((this.annotations == null)? 0 :this.annotations.hashCode()));
        result = ((result* 31)+((this.resources == null)? 0 :this.resources.hashCode()));
        result = ((result* 31)+((this.startupProbe == null)? 0 :this.startupProbe.hashCode()));
        result = ((result* 31)+((this.env == null)? 0 :this.env.hashCode()));
        result = ((result* 31)+((this.enabled == null)? 0 :this.enabled.hashCode()));
        result = ((result* 31)+((this.labels == null)? 0 :this.labels.hashCode()));
        result = ((result* 31)+((this.volumeMounts == null)? 0 :this.volumeMounts.hashCode()));
        result = ((result* 31)+((this.route == null)? 0 :this.route.hashCode()));
        result = ((result* 31)+((this.readinessProbe == null)? 0 :this.readinessProbe.hashCode()));
        result = ((result* 31)+((this.tls == null)? 0 :this.tls.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.envFrom == null)? 0 :this.envFrom.hashCode()));
        result = ((result* 31)+((this.initContainers == null)? 0 :this.initContainers.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Deploy) == false) {
            return false;
        }
        Deploy rhs = ((Deploy) other);
        return (((((((((((((((((((this.livenessProbe == rhs.livenessProbe)||((this.livenessProbe!= null)&&this.livenessProbe.equals(rhs.livenessProbe)))&&((this.extraContainers == rhs.extraContainers)||((this.extraContainers!= null)&&this.extraContainers.equals(rhs.extraContainers))))&&((this.replicas == rhs.replicas)||((this.replicas!= null)&&this.replicas.equals(rhs.replicas))))&&((this.imagePullSecrets == rhs.imagePullSecrets)||((this.imagePullSecrets!= null)&&this.imagePullSecrets.equals(rhs.imagePullSecrets))))&&((this.volumes == rhs.volumes)||((this.volumes!= null)&&this.volumes.equals(rhs.volumes))))&&((this.annotations == rhs.annotations)||((this.annotations!= null)&&this.annotations.equals(rhs.annotations))))&&((this.resources == rhs.resources)||((this.resources!= null)&&this.resources.equals(rhs.resources))))&&((this.startupProbe == rhs.startupProbe)||((this.startupProbe!= null)&&this.startupProbe.equals(rhs.startupProbe))))&&((this.env == rhs.env)||((this.env!= null)&&this.env.equals(rhs.env))))&&((this.enabled == rhs.enabled)||((this.enabled!= null)&&this.enabled.equals(rhs.enabled))))&&((this.labels == rhs.labels)||((this.labels!= null)&&this.labels.equals(rhs.labels))))&&((this.volumeMounts == rhs.volumeMounts)||((this.volumeMounts!= null)&&this.volumeMounts.equals(rhs.volumeMounts))))&&((this.route == rhs.route)||((this.route!= null)&&this.route.equals(rhs.route))))&&((this.readinessProbe == rhs.readinessProbe)||((this.readinessProbe!= null)&&this.readinessProbe.equals(rhs.readinessProbe))))&&((this.tls == rhs.tls)||((this.tls!= null)&&this.tls.equals(rhs.tls))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.envFrom == rhs.envFrom)||((this.envFrom!= null)&&this.envFrom.equals(rhs.envFrom))))&&((this.initContainers == rhs.initContainers)||((this.initContainers!= null)&&this.initContainers.equals(rhs.initContainers))));
    }

}
