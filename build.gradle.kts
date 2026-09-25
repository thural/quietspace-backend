plugins {
    id("org.springframework.boot") version "4.1.0" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("com.github.spotbugs") version "6.2.1" apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "jacoco")
    apply(plugin = "com.github.spotbugs")
    apply(plugin = "pmd")
    apply(plugin = "checkstyle")

    group = "dev.thural.quietspace"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-Amapstruct.defaultComponentModel=spring")
        // Compile with Java 21, target Java 25
        options.release = 21
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    configure<com.github.spotbugs.snom.SpotBugsExtension> {
        toolVersion = "4.9.0"
        ignoreFailures = false
        showStackTraces = true
        excludeFilter = file("$rootDir/config/spotbugs/exclude.xml")
    }

    tasks.named<com.github.spotbugs.snom.SpotBugsTask>("spotbugsMain") {
        dependsOn("compileJava")
    }

    tasks.named<com.github.spotbugs.snom.SpotBugsTask>("spotbugsTest") {
        dependsOn("compileTestJava")
    }

    configure<PmdExtension> {
        toolVersion = "6.55.0"
    }

    configure<CheckstyleExtension> {
        toolVersion = "10.17.0"
        configFile = file("$rootDir/config/checkstyle/checkstyle.xml")
    }

    tasks.named<Checkstyle>("checkstyleMain") {
        configFile = file("$rootDir/config/checkstyle/checkstyle.xml")
    }

    tasks.named<Checkstyle>("checkstyleTest") {
        configFile = file("$rootDir/config/checkstyle/checkstyle.xml")
    }

    tasks.named("check") {
        dependsOn("spotbugsMain", "pmdMain", "checkstyleMain")
    }

    // Only the deployable app module produces a bootable jar; library and
    // intermediate container projects must not attempt bootJar resolution.
    tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
        enabled = project.name == "quietspace-app"
    }
}

allprojects {
    repositories {
        mavenCentral()
    }
}
