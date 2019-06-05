plugins {
  id("me.champeau.gradle.jmh") version "0.4.8"
}

val depVersions: Map<String, String> by extra

jmh {
  jmhVersion = depVersions["jmh"]
}

dependencies {
  implementation(project(":url-builder"))
  // let the IDE see the JMH types
  implementation("org.openjdk.jmh:jmh-core:${depVersions["jmh"]}")
  implementation("com.google.guava:guava:27.1-jre")

}
