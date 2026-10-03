plugins {
    id("quietspace.domain-conventions")
}

dependencies {
    implementation(project(":core:core-shared"))
    implementation(project(":core:core-data"))
    implementation(project(":domain:domain-user"))
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework:spring-messaging")

    // Test-only: JwtFilter deps for web-slice tests (core-security has no domain deps).
    testImplementation(project(":core:core-security"))
}
