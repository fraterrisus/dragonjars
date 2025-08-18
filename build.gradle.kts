plugins {
    java
    application
    id("org.javamodularity.moduleplugin") version "1.8.12"
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("org.beryx.jlink") version "2.25.0"
}

group = "com.hitchhikerprod"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val junitVersion = "5.10.2"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

val mockitoAgent = configurations.create("mockitoAgent")

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

application {
    mainModule.set("com.hitchhikerprod.dragonjars")
    mainClass.set("com.hitchhikerprod.dragonjars.DragonWarsApp")
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testImplementation("org.mockito:mockito-core:5.+")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
    mockitoAgent("org.mockito:mockito-core:5.+") { isTransitive = false }
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs?.add("-javaagent:${mockitoAgent.asPath}")
    jvmArgs?.add("-XX+EnableDynamicAgentLoading")
}

jlink {
    imageZip.set(layout.buildDirectory.file("/distributions/app-${javafx.platform.classifier}.zip"))
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    launcher {
        name = "app"
    }
}
