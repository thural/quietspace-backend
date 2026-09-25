plugins {
    id("quietspace.domain-conventions")
}

dependencies {
    implementation(project(":core:core-shared"))
    implementation(project(":core:core-data"))
    // NOTE: no domain dependencies — photo is a leaf. User access goes
    // through the shared-kernel UserProfilePort (implemented by domain-user).
    implementation("net.coobird:thumbnailator:0.4.20")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Test-only: exception advice for controller tests (core-web has no domain deps).
    testImplementation(project(":core:core-web"))
}
