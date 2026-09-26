plugins {
    id("quietspace.domain-conventions")
}

dependencies {
    implementation(project(":core:core-shared"))
    implementation(project(":core:core-data"))
    implementation(project(":domain:domain-user"))
    implementation(project(":domain:domain-notification"))
    // Test-only: ReactionRepositoryTest exercises Post association.
    testImplementation(project(":domain:domain-post"))
}
