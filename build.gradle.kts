group = "com.hitchhikerprod"
version = "1.1"

plugins {
    java
    application
    id("org.javamodularity.moduleplugin") version "2.0.1"
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.beryx.jlink") version "4.1.1"
}

repositories {
    mavenCentral()
}

val jacksonVersion = "2.21.4"
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
    testImplementation(platform("org.junit:junit-bom:${junitVersion}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.+")
    mockitoAgent("org.mockito:mockito-core:5.+") { isTransitive = false }
}

tasks.withType<Test> {
    useJUnitPlatform {
        includeEngines("junit-jupiter")
    }
}

jlink {
    imageZip.set(layout.buildDirectory.file("distributions/dragonjars-${version}-${javafx.platform.classifier}.zip"))
    options.set(listOf("--strip-debug", "--no-header-files", "--no-man-pages"))
    launcher {
        name = "dragonjars"
    }
}

jpackage {
    imageName = "dragonjars"
    if (org.gradle.internal.os.OperatingSystem.current().isLinux) {
        icon = "icon-256.png"
        installerOptions = listOf("--linux-shortcut", "--linux-deb-maintainer", "cordes.ben@gmail.com")
    } else if (org.gradle.internal.os.OperatingSystem.current().isWindows) {
        icon = "icon-256.ico"
        installerOptions = listOf("--win-per-user-install", "--win-menu", "--win-menu-group", "Entertainment",
            "--win-dir-chooser", "--win-shortcut-prompt")
    }
}
