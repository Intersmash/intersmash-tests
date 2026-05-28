
package org.jboss.intersmash.model.helm.charts.values.eap82;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;


/**
 * Configuration to build the application image
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "enabled",
    "mode",
    "uri",
    "ref",
    "contextDir",
    "sourceSecret",
    "pullSecret",
    "output",
    "env",
    "resources",
    "images",
    "triggers",
    "s2i",
    "bootableJar"
})
@Generated("jsonschema2pojo")
public class Build {

    /**
     * Enable/Disable building the application image
     * 
     */
    @JsonProperty("enabled")
    @JsonPropertyDescription("Enable/Disable building the application image")
    private Boolean enabled = true;
    /**
     * Which mode to use to build the application
     * 
     */
    @JsonProperty("mode")
    @JsonPropertyDescription("Which mode to use to build the application")
    private Mode mode = Mode.fromValue("s2i");
    /**
     * URI of GitHub repository
     * 
     */
    @JsonProperty("uri")
    @JsonPropertyDescription("URI of GitHub repository")
    private String uri;
    /**
     * Git reference
     * 
     */
    @JsonProperty("ref")
    @JsonPropertyDescription("Git reference")
    private String ref;
    /**
     * Context directory within your Git repo to use as the root for the build
     * 
     */
    @JsonProperty("contextDir")
    @JsonPropertyDescription("Context directory within your Git repo to use as the root for the build")
    private String contextDir;
    /**
     * Name of the Secret to use when cloning Git source project
     * 
     */
    @JsonProperty("sourceSecret")
    @JsonPropertyDescription("Name of the Secret to use when cloning Git source project")
    private String sourceSecret;
    /**
     * Name of the Pull Secret
     * 
     */
    @JsonProperty("pullSecret")
    @JsonPropertyDescription("Name of the Pull Secret")
    private String pullSecret;
    /**
     * Configuration for the built application image
     * 
     */
    @JsonProperty("output")
    @JsonPropertyDescription("Configuration for the built application image")
    private Output output;
    /**
     * List of environment variables to set in the container. Cannot be updated.
     * 
     */
    @JsonProperty("env")
    @JsonPropertyDescription("List of environment variables to set in the container. Cannot be updated.")
    private List<Env> env = new ArrayList<Env>();
    /**
     * Freeform resources field. More information: https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/
     * 
     */
    @JsonProperty("resources")
    @JsonPropertyDescription("Freeform resources field. More information: https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/")
    private Object resources;
    /**
     * Freeform images injected in the source during build.
     * 
     */
    @JsonProperty("images")
    @JsonPropertyDescription("Freeform images injected in the source during build.")
    private Object images;
    /**
     * Webhooks to trigger building the application image
     * 
     */
    @JsonProperty("triggers")
    @JsonPropertyDescription("Webhooks to trigger building the application image")
    private Triggers triggers;
    /**
     * Configuration specific to S2I Build (applicable only if build mode is set to s2i)
     * 
     */
    @JsonProperty("s2i")
    @JsonPropertyDescription("Configuration specific to S2I Build (applicable only if build mode is set to s2i)")
    private S2i s2i;
    /**
     * Configuration specific to Bootable Jar Build (applicable only if build mode is set to bootable-jar)
     * 
     */
    @JsonProperty("bootableJar")
    @JsonPropertyDescription("Configuration specific to Bootable Jar Build (applicable only if build mode is set to bootable-jar)")
    private BootableJar bootableJar;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Enable/Disable building the application image
     * 
     */
    @JsonProperty("enabled")
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * Enable/Disable building the application image
     * 
     */
    @JsonProperty("enabled")
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Build withEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Which mode to use to build the application
     * 
     */
    @JsonProperty("mode")
    public Mode getMode() {
        return mode;
    }

    /**
     * Which mode to use to build the application
     * 
     */
    @JsonProperty("mode")
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public Build withMode(Mode mode) {
        this.mode = mode;
        return this;
    }

    /**
     * URI of GitHub repository
     * 
     */
    @JsonProperty("uri")
    public String getUri() {
        return uri;
    }

    /**
     * URI of GitHub repository
     * 
     */
    @JsonProperty("uri")
    public void setUri(String uri) {
        this.uri = uri;
    }

    public Build withUri(String uri) {
        this.uri = uri;
        return this;
    }

    /**
     * Git reference
     * 
     */
    @JsonProperty("ref")
    public String getRef() {
        return ref;
    }

    /**
     * Git reference
     * 
     */
    @JsonProperty("ref")
    public void setRef(String ref) {
        this.ref = ref;
    }

    public Build withRef(String ref) {
        this.ref = ref;
        return this;
    }

    /**
     * Context directory within your Git repo to use as the root for the build
     * 
     */
    @JsonProperty("contextDir")
    public String getContextDir() {
        return contextDir;
    }

    /**
     * Context directory within your Git repo to use as the root for the build
     * 
     */
    @JsonProperty("contextDir")
    public void setContextDir(String contextDir) {
        this.contextDir = contextDir;
    }

    public Build withContextDir(String contextDir) {
        this.contextDir = contextDir;
        return this;
    }

    /**
     * Name of the Secret to use when cloning Git source project
     * 
     */
    @JsonProperty("sourceSecret")
    public String getSourceSecret() {
        return sourceSecret;
    }

    /**
     * Name of the Secret to use when cloning Git source project
     * 
     */
    @JsonProperty("sourceSecret")
    public void setSourceSecret(String sourceSecret) {
        this.sourceSecret = sourceSecret;
    }

    public Build withSourceSecret(String sourceSecret) {
        this.sourceSecret = sourceSecret;
        return this;
    }

    /**
     * Name of the Pull Secret
     * 
     */
    @JsonProperty("pullSecret")
    public String getPullSecret() {
        return pullSecret;
    }

    /**
     * Name of the Pull Secret
     * 
     */
    @JsonProperty("pullSecret")
    public void setPullSecret(String pullSecret) {
        this.pullSecret = pullSecret;
    }

    public Build withPullSecret(String pullSecret) {
        this.pullSecret = pullSecret;
        return this;
    }

    /**
     * Configuration for the built application image
     * 
     */
    @JsonProperty("output")
    public Output getOutput() {
        return output;
    }

    /**
     * Configuration for the built application image
     * 
     */
    @JsonProperty("output")
    public void setOutput(Output output) {
        this.output = output;
    }

    public Build withOutput(Output output) {
        this.output = output;
        return this;
    }

    /**
     * List of environment variables to set in the container. Cannot be updated.
     * 
     */
    @JsonProperty("env")
    public List<Env> getEnv() {
        return env;
    }

    /**
     * List of environment variables to set in the container. Cannot be updated.
     * 
     */
    @JsonProperty("env")
    public void setEnv(List<Env> env) {
        this.env = env;
    }

    public Build withEnv(List<Env> env) {
        this.env = env;
        return this;
    }

    /**
     * Freeform resources field. More information: https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/
     * 
     */
    @JsonProperty("resources")
    public Object getResources() {
        return resources;
    }

    /**
     * Freeform resources field. More information: https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/
     * 
     */
    @JsonProperty("resources")
    public void setResources(Object resources) {
        this.resources = resources;
    }

    public Build withResources(Object resources) {
        this.resources = resources;
        return this;
    }

    /**
     * Freeform images injected in the source during build.
     * 
     */
    @JsonProperty("images")
    public Object getImages() {
        return images;
    }

    /**
     * Freeform images injected in the source during build.
     * 
     */
    @JsonProperty("images")
    public void setImages(Object images) {
        this.images = images;
    }

    public Build withImages(Object images) {
        this.images = images;
        return this;
    }

    /**
     * Webhooks to trigger building the application image
     * 
     */
    @JsonProperty("triggers")
    public Triggers getTriggers() {
        return triggers;
    }

    /**
     * Webhooks to trigger building the application image
     * 
     */
    @JsonProperty("triggers")
    public void setTriggers(Triggers triggers) {
        this.triggers = triggers;
    }

    public Build withTriggers(Triggers triggers) {
        this.triggers = triggers;
        return this;
    }

    /**
     * Configuration specific to S2I Build (applicable only if build mode is set to s2i)
     * 
     */
    @JsonProperty("s2i")
    public S2i getS2i() {
        return s2i;
    }

    /**
     * Configuration specific to S2I Build (applicable only if build mode is set to s2i)
     * 
     */
    @JsonProperty("s2i")
    public void setS2i(S2i s2i) {
        this.s2i = s2i;
    }

    public Build withS2i(S2i s2i) {
        this.s2i = s2i;
        return this;
    }

    /**
     * Configuration specific to Bootable Jar Build (applicable only if build mode is set to bootable-jar)
     * 
     */
    @JsonProperty("bootableJar")
    public BootableJar getBootableJar() {
        return bootableJar;
    }

    /**
     * Configuration specific to Bootable Jar Build (applicable only if build mode is set to bootable-jar)
     * 
     */
    @JsonProperty("bootableJar")
    public void setBootableJar(BootableJar bootableJar) {
        this.bootableJar = bootableJar;
    }

    public Build withBootableJar(BootableJar bootableJar) {
        this.bootableJar = bootableJar;
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

    public Build withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Build.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("enabled");
        sb.append('=');
        sb.append(((this.enabled == null)?"<null>":this.enabled));
        sb.append(',');
        sb.append("mode");
        sb.append('=');
        sb.append(((this.mode == null)?"<null>":this.mode));
        sb.append(',');
        sb.append("uri");
        sb.append('=');
        sb.append(((this.uri == null)?"<null>":this.uri));
        sb.append(',');
        sb.append("ref");
        sb.append('=');
        sb.append(((this.ref == null)?"<null>":this.ref));
        sb.append(',');
        sb.append("contextDir");
        sb.append('=');
        sb.append(((this.contextDir == null)?"<null>":this.contextDir));
        sb.append(',');
        sb.append("sourceSecret");
        sb.append('=');
        sb.append(((this.sourceSecret == null)?"<null>":this.sourceSecret));
        sb.append(',');
        sb.append("pullSecret");
        sb.append('=');
        sb.append(((this.pullSecret == null)?"<null>":this.pullSecret));
        sb.append(',');
        sb.append("output");
        sb.append('=');
        sb.append(((this.output == null)?"<null>":this.output));
        sb.append(',');
        sb.append("env");
        sb.append('=');
        sb.append(((this.env == null)?"<null>":this.env));
        sb.append(',');
        sb.append("resources");
        sb.append('=');
        sb.append(((this.resources == null)?"<null>":this.resources));
        sb.append(',');
        sb.append("images");
        sb.append('=');
        sb.append(((this.images == null)?"<null>":this.images));
        sb.append(',');
        sb.append("triggers");
        sb.append('=');
        sb.append(((this.triggers == null)?"<null>":this.triggers));
        sb.append(',');
        sb.append("s2i");
        sb.append('=');
        sb.append(((this.s2i == null)?"<null>":this.s2i));
        sb.append(',');
        sb.append("bootableJar");
        sb.append('=');
        sb.append(((this.bootableJar == null)?"<null>":this.bootableJar));
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
        result = ((result* 31)+((this.sourceSecret == null)? 0 :this.sourceSecret.hashCode()));
        result = ((result* 31)+((this.images == null)? 0 :this.images.hashCode()));
        result = ((result* 31)+((this.resources == null)? 0 :this.resources.hashCode()));
        result = ((result* 31)+((this.s2i == null)? 0 :this.s2i.hashCode()));
        result = ((result* 31)+((this.env == null)? 0 :this.env.hashCode()));
        result = ((result* 31)+((this.triggers == null)? 0 :this.triggers.hashCode()));
        result = ((result* 31)+((this.uri == null)? 0 :this.uri.hashCode()));
        result = ((result* 31)+((this.enabled == null)? 0 :this.enabled.hashCode()));
        result = ((result* 31)+((this.contextDir == null)? 0 :this.contextDir.hashCode()));
        result = ((result* 31)+((this.mode == null)? 0 :this.mode.hashCode()));
        result = ((result* 31)+((this.output == null)? 0 :this.output.hashCode()));
        result = ((result* 31)+((this.ref == null)? 0 :this.ref.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.pullSecret == null)? 0 :this.pullSecret.hashCode()));
        result = ((result* 31)+((this.bootableJar == null)? 0 :this.bootableJar.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Build) == false) {
            return false;
        }
        Build rhs = ((Build) other);
        return ((((((((((((((((this.sourceSecret == rhs.sourceSecret)||((this.sourceSecret!= null)&&this.sourceSecret.equals(rhs.sourceSecret)))&&((this.images == rhs.images)||((this.images!= null)&&this.images.equals(rhs.images))))&&((this.resources == rhs.resources)||((this.resources!= null)&&this.resources.equals(rhs.resources))))&&((this.s2i == rhs.s2i)||((this.s2i!= null)&&this.s2i.equals(rhs.s2i))))&&((this.env == rhs.env)||((this.env!= null)&&this.env.equals(rhs.env))))&&((this.triggers == rhs.triggers)||((this.triggers!= null)&&this.triggers.equals(rhs.triggers))))&&((this.uri == rhs.uri)||((this.uri!= null)&&this.uri.equals(rhs.uri))))&&((this.enabled == rhs.enabled)||((this.enabled!= null)&&this.enabled.equals(rhs.enabled))))&&((this.contextDir == rhs.contextDir)||((this.contextDir!= null)&&this.contextDir.equals(rhs.contextDir))))&&((this.mode == rhs.mode)||((this.mode!= null)&&this.mode.equals(rhs.mode))))&&((this.output == rhs.output)||((this.output!= null)&&this.output.equals(rhs.output))))&&((this.ref == rhs.ref)||((this.ref!= null)&&this.ref.equals(rhs.ref))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.pullSecret == rhs.pullSecret)||((this.pullSecret!= null)&&this.pullSecret.equals(rhs.pullSecret))))&&((this.bootableJar == rhs.bootableJar)||((this.bootableJar!= null)&&this.bootableJar.equals(rhs.bootableJar))));
    }


    /**
     * Which mode to use to build the application
     * 
     */
    @Generated("jsonschema2pojo")
    public enum Mode {

        S_2_I("s2i"),
        BOOTABLE_JAR("bootable-jar");
        private final String value;
        private final static Map<String, Mode> CONSTANTS = new HashMap<String, Mode>();

        static {
            for (Mode c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Mode(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static Mode fromValue(String value) {
            Mode constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
