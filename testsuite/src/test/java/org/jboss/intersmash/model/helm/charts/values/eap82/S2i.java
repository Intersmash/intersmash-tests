
package org.jboss.intersmash.model.helm.charts.values.eap82;

import java.util.HashMap;
import java.util.LinkedHashMap;
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
 * Configuration specific to S2I Build (applicable only if build mode is set to s2i)
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "kind",
    "jdk",
    "jdk21",
    "jdk25",
    "buildApplicationImage",
    "builderKind",
    "runtimeKind",
    "featurePacks",
    "galleonDir",
    "galleonLayers",
    "channels"
})
@Generated("jsonschema2pojo")
public class S2i {

    /**
     * Determines the type of images for S2I Builder and Runtime images
     * 
     */
    @JsonProperty("kind")
    @JsonPropertyDescription("Determines the type of images for S2I Builder and Runtime images")
    private Kind kind = Kind.fromValue("DockerImage");
    /**
     * JDK Version of the EAP S2I images
     * 
     */
    @JsonProperty("jdk")
    @JsonPropertyDescription("JDK Version of the EAP S2I images")
    private Jdk jdk = Jdk.fromValue("21");
    /**
     * EAP S2I images for JDK 21
     * 
     */
    @JsonProperty("jdk21")
    @JsonPropertyDescription("EAP S2I images for JDK 21")
    private Jdk21 jdk21;
    /**
     * EAP S2I images for JDK 25
     * 
     */
    @JsonProperty("jdk25")
    @JsonPropertyDescription("EAP S2I images for JDK 25")
    private Jdk25 jdk25;
    /**
     * Determine if the application image must be built. If false, the Helm release will  build the first artifact image (with the name of the Helm release)
     * 
     */
    @JsonProperty("buildApplicationImage")
    @JsonPropertyDescription("Determine if the application image must be built. If false, the Helm release will  build the first artifact image (with the name of the Helm release)")
    private Boolean buildApplicationImage = true;
    /**
     * Determines the type of images for S2I Builder image. If omitted, the value of the kind properties is used
     * 
     */
    @JsonProperty("builderKind")
    @JsonPropertyDescription("Determines the type of images for S2I Builder image. If omitted, the value of the kind properties is used")
    private BuilderKind builderKind;
    /**
     * Determines the type of images for S2I Runtime image. If omitted, the value of the kind properties is used
     * 
     */
    @JsonProperty("runtimeKind")
    @JsonPropertyDescription("Determines the type of images for S2I Runtime image. If omitted, the value of the kind properties is used")
    private RuntimeKind runtimeKind;
    /**
     * List of Galleon feature-packs identified by Maven coordinates (`<groupId>:<artifactId>:<version>`). Deprecated, the recommended way to provision EAP is to use the eap-maven-plugin in the application pom.xml
     * 
     */
    @JsonProperty("featurePacks")
    @JsonPropertyDescription("List of Galleon feature-packs identified by Maven coordinates (`<groupId>:<artifactId>:<version>`). Deprecated, the recommended way to provision EAP is to use the eap-maven-plugin in the application pom.xml")
    private String featurePacks;
    /**
     * Directory relative to the root directory for the build that contains custom content for Galleon.
     * 
     */
    @JsonProperty("galleonDir")
    @JsonPropertyDescription("Directory relative to the root directory for the build that contains custom content for Galleon.")
    private String galleonDir;
    /**
     * List of Galleon Layers to provision. If galleonLayers are configured, the featurePacks that provides the layers must be specified (including EAP feature pack). Deprecated, the recommended way to provision EAP is to use the eap-maven-plugin in the application pom.xml
     * 
     */
    @JsonProperty("galleonLayers")
    @JsonPropertyDescription("List of Galleon Layers to provision. If galleonLayers are configured, the featurePacks that provides the layers must be specified (including EAP feature pack). Deprecated, the recommended way to provision EAP is to use the eap-maven-plugin in the application pom.xml")
    private String galleonLayers;
    /**
     * List of Channels identified by Maven coordinates (`<groupId>:<artifactId>`). If featurePacks are configured without any versioning, the channels that provides the latest feature packs can be specified. Deprecated, the recommended way to provision EAP is to use the eap-maven-plugin in the application pom.xml
     * 
     */
    @JsonProperty("channels")
    @JsonPropertyDescription("List of Channels identified by Maven coordinates (`<groupId>:<artifactId>`). If featurePacks are configured without any versioning, the channels that provides the latest feature packs can be specified. Deprecated, the recommended way to provision EAP is to use the eap-maven-plugin in the application pom.xml")
    private String channels;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Determines the type of images for S2I Builder and Runtime images
     * 
     */
    @JsonProperty("kind")
    public Kind getKind() {
        return kind;
    }

    /**
     * Determines the type of images for S2I Builder and Runtime images
     * 
     */
    @JsonProperty("kind")
    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public S2i withKind(Kind kind) {
        this.kind = kind;
        return this;
    }

    /**
     * JDK Version of the EAP S2I images
     * 
     */
    @JsonProperty("jdk")
    public Jdk getJdk() {
        return jdk;
    }

    /**
     * JDK Version of the EAP S2I images
     * 
     */
    @JsonProperty("jdk")
    public void setJdk(Jdk jdk) {
        this.jdk = jdk;
    }

    public S2i withJdk(Jdk jdk) {
        this.jdk = jdk;
        return this;
    }

    /**
     * EAP S2I images for JDK 21
     * 
     */
    @JsonProperty("jdk21")
    public Jdk21 getJdk21() {
        return jdk21;
    }

    /**
     * EAP S2I images for JDK 21
     * 
     */
    @JsonProperty("jdk21")
    public void setJdk21(Jdk21 jdk21) {
        this.jdk21 = jdk21;
    }

    public S2i withJdk21(Jdk21 jdk21) {
        this.jdk21 = jdk21;
        return this;
    }

    /**
     * EAP S2I images for JDK 25
     * 
     */
    @JsonProperty("jdk25")
    public Jdk25 getJdk25() {
        return jdk25;
    }

    /**
     * EAP S2I images for JDK 25
     * 
     */
    @JsonProperty("jdk25")
    public void setJdk25(Jdk25 jdk25) {
        this.jdk25 = jdk25;
    }

    public S2i withJdk25(Jdk25 jdk25) {
        this.jdk25 = jdk25;
        return this;
    }

    /**
     * Determine if the application image must be built. If false, the Helm release will  build the first artifact image (with the name of the Helm release)
     * 
     */
    @JsonProperty("buildApplicationImage")
    public Boolean getBuildApplicationImage() {
        return buildApplicationImage;
    }

    /**
     * Determine if the application image must be built. If false, the Helm release will  build the first artifact image (with the name of the Helm release)
     * 
     */
    @JsonProperty("buildApplicationImage")
    public void setBuildApplicationImage(Boolean buildApplicationImage) {
        this.buildApplicationImage = buildApplicationImage;
    }

    public S2i withBuildApplicationImage(Boolean buildApplicationImage) {
        this.buildApplicationImage = buildApplicationImage;
        return this;
    }

    /**
     * Determines the type of images for S2I Builder image. If omitted, the value of the kind properties is used
     * 
     */
    @JsonProperty("builderKind")
    public BuilderKind getBuilderKind() {
        return builderKind;
    }

    /**
     * Determines the type of images for S2I Builder image. If omitted, the value of the kind properties is used
     * 
     */
    @JsonProperty("builderKind")
    public void setBuilderKind(BuilderKind builderKind) {
        this.builderKind = builderKind;
    }

    public S2i withBuilderKind(BuilderKind builderKind) {
        this.builderKind = builderKind;
        return this;
    }

    /**
     * Determines the type of images for S2I Runtime image. If omitted, the value of the kind properties is used
     * 
     */
    @JsonProperty("runtimeKind")
    public RuntimeKind getRuntimeKind() {
        return runtimeKind;
    }

    /**
     * Determines the type of images for S2I Runtime image. If omitted, the value of the kind properties is used
     * 
     */
    @JsonProperty("runtimeKind")
    public void setRuntimeKind(RuntimeKind runtimeKind) {
        this.runtimeKind = runtimeKind;
    }

    public S2i withRuntimeKind(RuntimeKind runtimeKind) {
        this.runtimeKind = runtimeKind;
        return this;
    }

    /**
     * List of Galleon feature-packs identified by Maven coordinates (`<groupId>:<artifactId>:<version>`). Deprecated, the recommended way to provision EAP is to use the eap-maven-plugin in the application pom.xml
     * 
     */
    @JsonProperty("featurePacks")
    public String getFeaturePacks() {
        return featurePacks;
    }

    /**
     * List of Galleon feature-packs identified by Maven coordinates (`<groupId>:<artifactId>:<version>`). Deprecated, the recommended way to provision EAP is to use the eap-maven-plugin in the application pom.xml
     * 
     */
    @JsonProperty("featurePacks")
    public void setFeaturePacks(String featurePacks) {
        this.featurePacks = featurePacks;
    }

    public S2i withFeaturePacks(String featurePacks) {
        this.featurePacks = featurePacks;
        return this;
    }

    /**
     * Directory relative to the root directory for the build that contains custom content for Galleon.
     * 
     */
    @JsonProperty("galleonDir")
    public String getGalleonDir() {
        return galleonDir;
    }

    /**
     * Directory relative to the root directory for the build that contains custom content for Galleon.
     * 
     */
    @JsonProperty("galleonDir")
    public void setGalleonDir(String galleonDir) {
        this.galleonDir = galleonDir;
    }

    public S2i withGalleonDir(String galleonDir) {
        this.galleonDir = galleonDir;
        return this;
    }

    /**
     * List of Galleon Layers to provision. If galleonLayers are configured, the featurePacks that provides the layers must be specified (including EAP feature pack). Deprecated, the recommended way to provision EAP is to use the eap-maven-plugin in the application pom.xml
     * 
     */
    @JsonProperty("galleonLayers")
    public String getGalleonLayers() {
        return galleonLayers;
    }

    /**
     * List of Galleon Layers to provision. If galleonLayers are configured, the featurePacks that provides the layers must be specified (including EAP feature pack). Deprecated, the recommended way to provision EAP is to use the eap-maven-plugin in the application pom.xml
     * 
     */
    @JsonProperty("galleonLayers")
    public void setGalleonLayers(String galleonLayers) {
        this.galleonLayers = galleonLayers;
    }

    public S2i withGalleonLayers(String galleonLayers) {
        this.galleonLayers = galleonLayers;
        return this;
    }

    /**
     * List of Channels identified by Maven coordinates (`<groupId>:<artifactId>`). If featurePacks are configured without any versioning, the channels that provides the latest feature packs can be specified. Deprecated, the recommended way to provision EAP is to use the eap-maven-plugin in the application pom.xml
     * 
     */
    @JsonProperty("channels")
    public String getChannels() {
        return channels;
    }

    /**
     * List of Channels identified by Maven coordinates (`<groupId>:<artifactId>`). If featurePacks are configured without any versioning, the channels that provides the latest feature packs can be specified. Deprecated, the recommended way to provision EAP is to use the eap-maven-plugin in the application pom.xml
     * 
     */
    @JsonProperty("channels")
    public void setChannels(String channels) {
        this.channels = channels;
    }

    public S2i withChannels(String channels) {
        this.channels = channels;
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

    public S2i withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(S2i.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("kind");
        sb.append('=');
        sb.append(((this.kind == null)?"<null>":this.kind));
        sb.append(',');
        sb.append("jdk");
        sb.append('=');
        sb.append(((this.jdk == null)?"<null>":this.jdk));
        sb.append(',');
        sb.append("jdk21");
        sb.append('=');
        sb.append(((this.jdk21 == null)?"<null>":this.jdk21));
        sb.append(',');
        sb.append("jdk25");
        sb.append('=');
        sb.append(((this.jdk25 == null)?"<null>":this.jdk25));
        sb.append(',');
        sb.append("buildApplicationImage");
        sb.append('=');
        sb.append(((this.buildApplicationImage == null)?"<null>":this.buildApplicationImage));
        sb.append(',');
        sb.append("builderKind");
        sb.append('=');
        sb.append(((this.builderKind == null)?"<null>":this.builderKind));
        sb.append(',');
        sb.append("runtimeKind");
        sb.append('=');
        sb.append(((this.runtimeKind == null)?"<null>":this.runtimeKind));
        sb.append(',');
        sb.append("featurePacks");
        sb.append('=');
        sb.append(((this.featurePacks == null)?"<null>":this.featurePacks));
        sb.append(',');
        sb.append("galleonDir");
        sb.append('=');
        sb.append(((this.galleonDir == null)?"<null>":this.galleonDir));
        sb.append(',');
        sb.append("galleonLayers");
        sb.append('=');
        sb.append(((this.galleonLayers == null)?"<null>":this.galleonLayers));
        sb.append(',');
        sb.append("channels");
        sb.append('=');
        sb.append(((this.channels == null)?"<null>":this.channels));
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
        result = ((result* 31)+((this.jdk == null)? 0 :this.jdk.hashCode()));
        result = ((result* 31)+((this.jdk25 == null)? 0 :this.jdk25 .hashCode()));
        result = ((result* 31)+((this.galleonDir == null)? 0 :this.galleonDir.hashCode()));
        result = ((result* 31)+((this.galleonLayers == null)? 0 :this.galleonLayers.hashCode()));
        result = ((result* 31)+((this.featurePacks == null)? 0 :this.featurePacks.hashCode()));
        result = ((result* 31)+((this.jdk21 == null)? 0 :this.jdk21 .hashCode()));
        result = ((result* 31)+((this.channels == null)? 0 :this.channels.hashCode()));
        result = ((result* 31)+((this.runtimeKind == null)? 0 :this.runtimeKind.hashCode()));
        result = ((result* 31)+((this.kind == null)? 0 :this.kind.hashCode()));
        result = ((result* 31)+((this.buildApplicationImage == null)? 0 :this.buildApplicationImage.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.builderKind == null)? 0 :this.builderKind.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof S2i) == false) {
            return false;
        }
        S2i rhs = ((S2i) other);
        return (((((((((((((this.jdk == rhs.jdk)||((this.jdk!= null)&&this.jdk.equals(rhs.jdk)))&&((this.jdk25 == rhs.jdk25)||((this.jdk25 != null)&&this.jdk25 .equals(rhs.jdk25))))&&((this.galleonDir == rhs.galleonDir)||((this.galleonDir!= null)&&this.galleonDir.equals(rhs.galleonDir))))&&((this.galleonLayers == rhs.galleonLayers)||((this.galleonLayers!= null)&&this.galleonLayers.equals(rhs.galleonLayers))))&&((this.featurePacks == rhs.featurePacks)||((this.featurePacks!= null)&&this.featurePacks.equals(rhs.featurePacks))))&&((this.jdk21 == rhs.jdk21)||((this.jdk21 != null)&&this.jdk21 .equals(rhs.jdk21))))&&((this.channels == rhs.channels)||((this.channels!= null)&&this.channels.equals(rhs.channels))))&&((this.runtimeKind == rhs.runtimeKind)||((this.runtimeKind!= null)&&this.runtimeKind.equals(rhs.runtimeKind))))&&((this.kind == rhs.kind)||((this.kind!= null)&&this.kind.equals(rhs.kind))))&&((this.buildApplicationImage == rhs.buildApplicationImage)||((this.buildApplicationImage!= null)&&this.buildApplicationImage.equals(rhs.buildApplicationImage))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.builderKind == rhs.builderKind)||((this.builderKind!= null)&&this.builderKind.equals(rhs.builderKind))));
    }


    /**
     * Determines the type of images for S2I Builder image. If omitted, the value of the kind properties is used
     * 
     */
    @Generated("jsonschema2pojo")
    public enum BuilderKind {

        IMAGE_STREAM_TAG("ImageStreamTag"),
        DOCKER_IMAGE("DockerImage"),
        IMAGE_STREAM_IMAGE("ImageStreamImage");
        private final String value;
        private final static Map<String, BuilderKind> CONSTANTS = new HashMap<String, BuilderKind>();

        static {
            for (BuilderKind c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        BuilderKind(String value) {
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
        public static BuilderKind fromValue(String value) {
            BuilderKind constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }


    /**
     * JDK Version of the EAP S2I images
     * 
     */
    @Generated("jsonschema2pojo")
    public enum Jdk {

        _21("21"),
        _25("25");
        private final String value;
        private final static Map<String, Jdk> CONSTANTS = new HashMap<String, Jdk>();

        static {
            for (Jdk c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Jdk(String value) {
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
        public static Jdk fromValue(String value) {
            Jdk constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }


    /**
     * Determines the type of images for S2I Builder and Runtime images
     * 
     */
    @Generated("jsonschema2pojo")
    public enum Kind {

        IMAGE_STREAM_TAG("ImageStreamTag"),
        DOCKER_IMAGE("DockerImage"),
        IMAGE_STREAM_IMAGE("ImageStreamImage");
        private final String value;
        private final static Map<String, Kind> CONSTANTS = new HashMap<String, Kind>();

        static {
            for (Kind c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Kind(String value) {
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
        public static Kind fromValue(String value) {
            Kind constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }


    /**
     * Determines the type of images for S2I Runtime image. If omitted, the value of the kind properties is used
     * 
     */
    @Generated("jsonschema2pojo")
    public enum RuntimeKind {

        IMAGE_STREAM_TAG("ImageStreamTag"),
        DOCKER_IMAGE("DockerImage"),
        IMAGE_STREAM_IMAGE("ImageStreamImage");
        private final String value;
        private final static Map<String, RuntimeKind> CONSTANTS = new HashMap<String, RuntimeKind>();

        static {
            for (RuntimeKind c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        RuntimeKind(String value) {
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
        public static RuntimeKind fromValue(String value) {
            RuntimeKind constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
