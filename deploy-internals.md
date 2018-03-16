#Deployment in details

##Pseudo-users

Заведены раз и навсегда. При выкладке на новую виртуалку их надо будет там завести.

* bitbucket - в его /home битбакетовские пайплайны складывают результаты
* orientdb - для запуска ориента
* knnet - из-под него запускается бекенд

##Supplementary services

Уже установлены и настроены. Возможны разовые перенастройки по мере необходимости.
При выкладке на новую виртуалку их надо будет установить и настроить. 

###Orient

Скачан 3.0RC с orient-labs. Вариант с Gremlin и ThinkerPop.

/opt/orientdb/orientdb-community-gremlin-3.0.0RC2 - устанволен сюда
/opt/orientdb/orientdb - линк на установку
/opt/orientdb/orient-300rc.tgz - скачанный архив

###nginx

На виртуалке открыт порт 80. 
Это порт слушает nginx. 

В /etc/nginx/nginx.conf добавляется такой фрагмент:

```
     server {
        listen          80;
        server_name     catalog.reflexia.com;


        location ~ /deploy-status$  {
           proxy_pass  http://127.0.0.1:9988;
        }

        location / {
           proxy_pass http://127.0.0.1:8888;
        }

```

###lighttpd

В /etc/lighttpd/lighttpd.conf настраиваем:

* на каком порту слушаем
* где берем статический контент
* куда редиректим ссылки на бекенд

```
server.port                 = 8888
server.document-root        = "/home/knnet/bundles/frontend/web"

$HTTP["url"] =~ "^/api/" {
    proxy.server = (
        "" => ( (
            "host" => "127.0.0.1",
            "port" => 9997
        ) )
    )
}

```

##Accepting artefacts

В /home/knnet устанавливается скрипт bundler.py и запускающий его bundler.sh.

В пользовательский crontab пользователя knnet внесено:

```
0 * * * * bash /home/knnet/bundler.sh >> /home/knnet/bundler.out 2>> /home/knnet/bundler.err
5 * * * * bash /home/knnet/bundler.sh >> /home/knnet/bundler.out 2>> /home/knnet/bundler.err
10 * * * * bash /home/knnet/bundler.sh >> /home/knnet/bundler.out 2>> /home/knnet/bundler.err
15 * * * * bash /home/knnet/bundler.sh >> /home/knnet/bundler.out 2>> /home/knnet/bundler.err
20 * * * * bash /home/knnet/bundler.sh >> /home/knnet/bundler.out 2>> /home/knnet/bundler.err
25 * * * * bash /home/knnet/bundler.sh >> /home/knnet/bundler.out 2>> /home/knnet/bundler.err
30 * * * * bash /home/knnet/bundler.sh >> /home/knnet/bundler.out 2>> /home/knnet/bundler.err
35 * * * * bash /home/knnet/bundler.sh >> /home/knnet/bundler.out 2>> /home/knnet/bundler.err
40 * * * * bash /home/knnet/bundler.sh >> /home/knnet/bundler.out 2>> /home/knnet/bundler.err
45 * * * * bash /home/knnet/bundler.sh >> /home/knnet/bundler.out 2>> /home/knnet/bundler.err
50 * * * * bash /home/knnet/bundler.sh >> /home/knnet/bundler.out 2>> /home/knnet/bundler.err
55 * * * * bash /home/knnet/bundler.sh >> /home/knnet/bundler.out 2>> /home/knnet/bundler.err
```

Принятые артефакты бекенда складываются в /home/knnet/bundles/backend, фронтенда - в /home/knnet/bundles/frontend

##Restart

Скрипты для запуска лежат в этом пространстве в deploy/restart. На виртуалке deploy-last.sh лежит в
/home/knnet/bundles, все остальное переносится, сохраняя иерархическую структуру.

Для каждого типа артефактов (фронт/бекенд в нашем случае) определяются три стадии - приготоволение, запуск
и регистрация. Для каждого сервиса задаются свой вариант действий для каждой стадии.

Для автозапуска у рута сделан такой crontab:

```
8 * * * * bash /root/deploy-knnet-last.sh >> /root/deploy-knnet-last.out 2>> /root/deploy-knnet-last.err
18 * * * * bash /root/deploy-knnet-last.sh >> /root/deploy-knnet-last.out 2>> /root/deploy-knnet-last.err
28 * * * * bash /root/deploy-knnet-last.sh >> /root/deploy-knnet-last.out 2>> /root/deploy-knnet-last.err
38 * * * * bash /root/deploy-knnet-last.sh >> /root/deploy-knnet-last.out 2>> /root/deploy-knnet-last.err
48 * * * * bash /root/deploy-knnet-last.sh >> /root/deploy-knnet-last.out 2>> /root/deploy-knnet-last.err
58 * * * * bash /root/deploy-knnet-last.sh >> /root/deploy-knnet-last.out 2>> /root/deploy-knnet-last.err
```

И то, что в deploy/restart-root, надо положить в root.

От имени рута только переключение делается, сам бекенд запускается от имени knnet.

##deploy-status

Пока совсем простенький скрипт, который показывает статус выкладки и состояние портов.
В идеале, здесь видится веб-интерфейс, которому можно будет сказать "выложи такую-то версию такого-то продукта туда-то"
и смотреть результаты выкладывания.

В пространстве лежит в deploy/control

На виртуалке запускается руками на порту 9998. 
