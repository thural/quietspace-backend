plugins {
    id("quietspace.domain-conventions")
}

dependencies {
    implementation(project(":core:core-shared"))
    implementation(project(":core:core-data"))
    implementation(project(":domain:domain-user"))
    implementation(project(":domain:domain-photo"))
    implementation(project(":domain:domain-reaction"))
    implementation(project(":domain:domain-comment"))
    implementation(project(":domain:domain-notification"))
}
