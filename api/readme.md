***Knowledge net kotlin API.***

Wrapper over http api to simplify usage of KN from Java/Kotlin application

for examples check example project

Usage: 

run (no maven repo provided so far) to publish api to maven local repository 
```bash
gradle :api:publishToMavenLocal
```

Add to build.gradle `kotlinx` repository required for 
[kotlinx.serialization](https://www.google.com/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&uact=8&ved=2ahUKEwjGpYj4u97dAhWshqYKHRFhDsIQFjAAegQICRAB&url=https%3A%2F%2Fgithub.com%2FKotlin%2Fkotlinx.serialization&usg=AOvVaw2mRyjLFB5sOwgzRudKjYAM)

```groovy
repositories {
    ...
    mavenLocal()
    maven { url "https://kotlin.bintray.com/kotlinx" }
    ...
}
``` 

add dependency

```groovy
dependencies {
    ...
    compile "com.infowings.catalog:api:version"
    ...
}
``` 

and use
```kotlin
    val api = KnowledgeNet(server,port, username, password)
```