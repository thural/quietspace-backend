plugins {
    id("quietspace.domain-conventions")
}

dependencies {
    implementation(project(":core:core-shared"))
    implementation(project(":core:core-data"))
    implementation(project(":core:core-messaging"))
    // One-way edge: user reads photo views; photo never depends on user.
    implementation(project(":domain:domain-photo"))
    // One-way edge: user implements notification-owned ports; notification is a sink.
    implementation(project(":domain:domain-notification"))

    // Test-only: exception advice for web-slice tests (core-web has no domain deps).
    testImplementation(project(":core:core-web"))
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework:spring-messaging")
}
