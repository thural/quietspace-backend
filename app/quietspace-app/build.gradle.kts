plugins {
    id("java")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("jacoco")
    id("com.github.spotbugs")
    id("pmd")
    id("checkstyle")
}

dependencies {
    implementation(project(":core:core-shared"))
    implementation(project(":core:core-data"))
    implementation(project(":core:core-web"))
    implementation(project(":core:core-security"))
    implementation(project(":core:core-messaging"))
    implementation(project(":domain:domain-user"))
    implementation(project(":domain:domain-post"))
    implementation(project(":domain:domain-photo"))
    implementation(project(":domain:domain-comment"))
    implementation(project(":domain:domain-reaction"))
    implementation(project(":domain:domain-chat"))
    implementation(project(":domain:domain-message"))
    implementation(project(":domain:domain-notification"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // Phase 8: Observability
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.zipkin.reporter2:zipkin-reporter-brave")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.security:spring-security-messaging")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-mysql")

    implementation("org.mapstruct:mapstruct:1.6.3")
    implementation("net.coobird:thumbnailator:0.4.20")

    implementation("com.fasterxml.jackson.core:jackson-core")

    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

    runtimeOnly("com.mysql:mysql-connector-j")
    runtimeOnly("com.h2database:h2")
    developmentOnly("org.springframework.boot:spring-boot-h2console")

    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

    testCompileOnly("org.projectlombok:lombok:1.18.46")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.46")
    testAnnotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
    testAnnotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("com.fasterxml.jackson.core:jackson-databind")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Testcontainers
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers:1.21.4")
    testImplementation("org.testcontainers:mysql:1.21.4")
    testImplementation("org.testcontainers:rabbitmq:1.21.4")
    testImplementation("org.testcontainers:junit-jupiter:1.21.4")

    // WireMock
    testImplementation("org.wiremock:wiremock-standalone:3.13.2")

    // ArchUnit
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
}

springBoot {
    mainClass.set("dev.thural.quietspace.QuietspaceApplication")
}

tasks.jar {
    enabled = false
}

tasks.bootJar {
    layered {
        enabled = true
    }
    archiveFileName.set("quietspace-${project.version}.jar")
}