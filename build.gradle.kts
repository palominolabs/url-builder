plugins {
    id("com.github.ben-manes.versions") version "0.21.0"
}

subprojects {
    apply(plugin = "java-library")

    configure<JavaPluginConvention> {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    repositories {
        jcenter()
    }

    @Suppress("UNUSED_VARIABLE") val depVersions by extra {
        mapOf(
                "slf4j" to "1.7.26",
                "jmh" to "1.21"
        )
    }
}

