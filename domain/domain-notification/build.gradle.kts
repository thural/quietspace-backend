plugins {
    id("quietspace.domain-conventions")
}

dependencies {
    implementation(project(":core:core-shared"))
    implementation(project(":core:core-data"))
    implementation(project(":core:core-messaging"))
    // NOTE: no domain dependencies — notification is a sink. Cross-domain data
    // arrives via consumer-owned ports (adapters live in provider modules).
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework:spring-messaging")
}
