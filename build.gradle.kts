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
    group = "dev.thural.quietspace"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    // NOTE: intermediate container projects (:app, :core, :domain) hold no
    // sources. Tooling is applied to leaf (source-owning) projects only so
    // empty parents don't gain tasks like bootJar that can never resolve.
    if (childProjects.isEmpty()) {
        apply(plugin = "java")
        apply(plugin = "org.springframework.boot")
        apply(plugin = "io.spring.dependency-management")
        apply(plugin = "jacoco")
        apply(plugin = "com.github.spotbugs")
        apply(plugin = "pmd")
        apply(plugin = "checkstyle")

        tasks.withType<JavaCompile> {
            options.compilerArgs.add("-Amapstruct.defaultComponentModel=spring")
        }

        tasks.withType<Test> {
            useJUnitPlatform()
        }

        // Only the deployable app module produces a bootable jar; library
        // modules must not attempt bootJar (no resolvable main class).
        if (project.name != "quietspace-app") {
            tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
                enabled = false
            }
        }

        configure<com.github.spotbugs.snom.SpotBugsExtension> {
            // 4.9.0 bundles ASM without Java 25 (major 69) support; 4.9.8+ parses it.
            toolVersion = "4.9.8"
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
    }
}

allprojects {
    repositories {
        mavenCentral()
    }
}
