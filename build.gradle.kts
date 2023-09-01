plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.15.0"
}

group = "com.dbb"
version = "1.8.2"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.28")
    annotationProcessor("org.projectlombok:lombok:1.18.28")
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2020.2")
    type.set("IU") // Target IDE Platform
    plugins.set(listOf("java"))
    sandboxDir.set("${rootProject.rootDir}/idea-sandbox")
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
    patchPluginXml {
        sinceBuild.set("202")
        untilBuild.set("232.*")
    }
}
