import java.util.Date

plugins {
    id("com.jfrog.bintray") version "1.8.4"
    id("com.github.spotbugs") version "2.0.0"
    id("groovy")
    id("maven-publish")
}

val depVersions: Map<String, String> by extra

dependencies {
    compile("com.google.code.findbugs:jsr305:3.0.2")

    testRuntime("org.slf4j:slf4j-simple:${depVersions["slf4j"]}")
    testRuntime("org.slf4j:log4j-over-slf4j:${depVersions["slf4j"]}")
    testRuntime("org.slf4j:jcl-over-slf4j:${depVersions["slf4j"]}")
    testCompile("org.slf4j:jul-to-slf4j:${depVersions["slf4j"]}")

    testCompile("junit:junit:4.12")

    testCompile("org.codehaus.groovy:groovy-all:2.4.4")
}

tasks {
    register<Jar>("sourceJar") {
        from(project.the<SourceSetContainer>()["main"].allJava)
        archiveClassifier.set("sources")
    }

    register<Jar>("docJar") {
        from(project.tasks["javadoc"])
        archiveClassifier.set("javadoc")
    }
}

group = "com.palominolabs.http"
version = "1.1.2-SNAPSHOT"

configure<PublishingExtension> {
    publications {
        register<MavenPublication>("bintray") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])
            artifact(tasks["sourceJar"])
            artifact(tasks["docJar"])
        }
    }
}

bintray {
    user = rootProject.findProperty("bintrayUser")?.toString()
    key = rootProject.findProperty("bintrayApiKey")?.toString()
    setPublications("bintray")

    with(pkg) {
        repo = "maven"
        setLicenses("Copyfree")
        vcsUrl = "https://github.com/palominolabs/url-builder"
        name = "com.palominolabs.http:url-builder"

        with(version) {
            name = project.version.toString()
            released = Date().toString()
            vcsTag = "v" + project.version
        }
    }
}

spotbugs {
    // don't findbugs the tests
    sourceSets = listOf(project.sourceSets.named("main").get())
}
