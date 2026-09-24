plugins {
    id("quietspace.domain-conventions")
}

dependencies {
    implementation(project(":core:core-shared"))
    implementation(project(":core:core-data"))
    implementation(project(":core:core-messaging"))
    implementation(project(":domain:domain-user"))
    implementation(project(":domain:domain-chat"))
    implementation(project(":domain:domain-photo"))
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework:spring-messaging")
}
