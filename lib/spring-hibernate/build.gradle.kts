dependencies {
    implementation(project(path = ":lib:core"))
    implementation(group = "org.springframework", name = "spring-tx", version = "5.3.5")
    implementation(group = "org.springframework", name = "spring-aop", version = "5.3.5")
    implementation(group = "org.aspectj", name = "aspectjrt", version = "1.9.6")

    implementation(group = "org.hibernate", name = "hibernate-core", version = "5.4.29.Final")
}