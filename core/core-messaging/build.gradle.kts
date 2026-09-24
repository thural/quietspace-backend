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
    implementation(project(":core:core-security"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.security:spring-security-messaging")
    implementation("org.springframework:spring-messaging")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.projectlombok:lombok:1.18.46")

    implementation("org.mapstruct:mapstruct:1.6.3")

    annotationProcessor("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers:1.21.4")
    testImplementation("org.testcontainers:rabbitmq:1.21.4")
    testImplementation("org.testcontainers:junit-jupiter:1.21.4")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter:1.21.4")
}
