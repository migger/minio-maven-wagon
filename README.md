Minio maven wagon
=================
This extension allows use [Minio][minio] server as artifact storage for maven

# How to enable
To enable minio wagon make 3 simple steps:

## 1. add extension to your pom.xml
```xml
<project>
  ...
  <build>
    <extensions>
      <extension>
        <groupId>ru.migger</groupId>
        <artifactId>minio-maven-wagon</artifactId>
        <version>${minio-maven-wagon}</version>
      </extension>
    </extensions>
  </build>
  ...
</project>
```
## 2. Add distribution model to you pom.xml
```xml
    <distributionManagement>
        <repository>
            <id>minio-repo</id>
            <url>minio://bundle>/bundle-path</url>
        </repository>
    </distributionManagement>
```

## 3. Set up user name and password
Please provide user name in form of `<accesskey>@<minio-server<:port>>`
 
There is 3 way how to pass user and password:
* Environment variables: `MINIO_USER` and `MINIO_PASSWORD` 
* System variables (-D...): `minio.user` and `minio.password` 
* Settings xml:
    ```xml
    <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
      <servers>
        <server>
          <id>mino-repo</id>
          <username>minio@minio.ru</username>
          <password>12312312309898123</password>
        </server>
      </servers>
    </settings>
    
    ```


[minio]: https://github.com/minio/minio
