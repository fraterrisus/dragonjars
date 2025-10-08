plugins {
    java
    application
    id("org.javamodularity.moduleplugin") version "1.8.12"
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("org.beryx.jlink") version "2.25.0"
}

group = "com.hitchhikerprod"
version = "1.0"

repositories {
    mavenCentral()
}

val jacksonVersion = "2.20.0"
val junitVersion = "5.10.2"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

val mockitoAgent = configurations.create("mockitoAgent")

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

// Always check your release notes: https://github.com/openjdk/jfx/tree/master/doc-files
application {
    mainModule.set("com.hitchhikerprod.dragonjars")
    mainClass.set("com.hitchhikerprod.dragonjars.DragonWarsApp")
    applicationDefaultJvmArgs = listOf(
        "--enable-native-access=javafx.graphics", // ,javafx.media,javafx.web
        "--sun-misc-unsafe-memory-access=allow" // should be unnecessary in JFX25
    )
}

javafx {
    version = "24"
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    implementation("com.fasterxml.jackson.jr:jackson-jr-objects:${jacksonVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testImplementation("org.mockito:mockito-core:5.+")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
    mockitoAgent("org.mockito:mockito-core:5.+") { isTransitive = false }
}

tasks.withType<Test> {
    useJUnitPlatform()
    // jvmArgs?.add("-javaagent:${mockitoAgent.asPath}")
    // jvmArgs?.add("-XX+EnableDynamicAgentLoading")
}

jlink {
    imageZip.set(layout.buildDirectory.file("distributions/dragonjars-${version}-${javafx.platform.classifier}.zip"))
    options.set(listOf("--strip-debug", "--no-header-files", "--no-man-pages"))
    launcher {
        name = "dragonjars"
    }
}
