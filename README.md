[![Build Status](https://travis-ci.com/InfoWings/Knowledge-Net.svg?branch=master)](https://travis-ci.com/InfoWings/Knowledge-Net)

# Knowledge EcoSystem

## Build

 To upload docker images (skip this step if you're just deploying):
  - put `gradle.properties` file next to `build.gradle` with hub.docker.com credentials:
     ```
     dockerUser=user
     dockerPassword=password
     dockerEmail=email
     ```
  - run `gradle dockerPushImage`

## Deploy

 - prepare orient configuration files:
    - run new docker container with Orient DB `docker run -d --name orientdb orientdb`
    - copy configuration files `docker cp orientdb:/orientdb/config/ <where to>`
    - edit files if necessary
    - put files to folder supposed to be mapped as orient image volume (see next step)
 - edit volumes paths and ports if necessary in `docker-compose.yml`
 - edit backend/fronend image versions
 - `docker-compose up`

### Restore from backup

Since remote storage restore available only on Orient Enterprise you have to create plocal storage and then just copy files from one instance to another.
 - Create %ORIENT_DATA% folder for separate Orient instance
 - run
```
docker run --rm -v <folder with backup>/:/orientdb/backups -v %ORIENT_DATA%/databases:/orientdb/databases  orientdb sh -c '/orientdb/bin/console.sh "create database plocal:/orientdb/databases/knet admin admin;restore database /orientdb/backups/<backup file>.zip;disconnect" '
```
 - shutdown running Orient with `docker-compose down` and remove `<volumes>/databases/knet`
 - copy `%ORIENT_DATA%/databases/knet` to `<volumes>/databases/knet`
 - `docker-compose up`