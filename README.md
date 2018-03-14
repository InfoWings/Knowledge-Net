# Knowledge EcoSystem

[Описание системы в confluence](https://iwings.atlassian.net/wiki/spaces/CHR/pages/219381761/Knowledge+Master+catalogue+-+product+requirements)

## Code style 

[официальный kotlin code-style](http://kotlinlang.org/docs/reference/coding-conventions.html)

## Build & Deploy

OrientDB docker для версии 3.0.0RC1:

`docker run -d --name orientdb -p 2424:2424 -p 2480:2480   -e ORIENTDB_ROOT_PASSWORD= <пароль> orientdb:3.0.0RC2`

application.properties файл должен быть в папке с backend.jar (или в user.dir) в нем должны быть прописаны все 
свойства оставленные пустыми в backend/resources/application.properties

backend run: 
`gradle bootRun` 

frontend run: 
`npm run serve` 