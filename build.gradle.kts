group = "com.hitchhikerprod"
version = "1.3"

plugins {
    java
    application
    alias(libs.plugins.modularity)
    alias(libs.plugins.javafx)
    alias(libs.plugins.jlink)
}

repositories {
    mavenCentral()
}


java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

val mockitoAgent = configurations.create("mockitoAgent")

application {
    mainModule.set("com.hitchhikerprod.dragonjars")
    mainClass.set("com.hitchhikerprod.dragonjars.DragonWarsApp")
    applicationDefaultJvmArgs = listOf(
        "--enable-native-access=javafx.graphics", // ,javafx.media,javafx.web
        "--sun-misc-unsafe-memory-access=allow" // should be unnecessary in JFX25
    )
}

// Always check your release notes: https://github.com/openjdk/jfx/tree/master/doc-files
javafx {
    version = "24"
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    implementation(libs.jackson)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
    testImplementation(libs.mockito)
    mockitoAgent(libs.mockito) { isTransitive = false }
}

// Explicitly add the Mockito java agent on the command line so it stops complaining.
// https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html#0.3
abstract class MockitoAgentProvider : CommandLineArgumentProvider {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val agentJar: ConfigurableFileCollection

    override fun asArguments(): Iterable<String?> = listOf("-javaagent:${agentJar.asPath}")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Test> {
    jvmArgumentProviders.add(
        objects.newInstance<MockitoAgentProvider>().apply {
            agentJar.from(mockitoAgent)
        }
    )
    useJUnitPlatform {
        includeEngines("junit-jupiter")
    }
}

tasks.register("generateProperties") {
    doLast {
        val outputDir = file("${projectDir}/src/main/resources/com/hitchhikerprod/dragonjars/")
        val configFile = File(outputDir, "build.properties")
        outputDir.mkdirs()
        configFile.writeText("app.version=${version}")
    }
}

tasks.named("compileJava") {
    dependsOn("generateProperties")
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
    icon = "icon-256.png"
    if (org.gradle.internal.os.OperatingSystem.current().isLinux) {
        installerOptions = listOf("--linux-shortcut", "--linux-deb-maintainer", "cordes.ben@gmail.com")
    } else if (org.gradle.internal.os.OperatingSystem.current().isWindows) {
        icon = "icon-256.ico"
        installerOptions = listOf("--win-per-user-install", "--win-menu", "--win-menu-group", "Entertainment",
            "--win-dir-chooser", "--win-shortcut-prompt")
    }
}
