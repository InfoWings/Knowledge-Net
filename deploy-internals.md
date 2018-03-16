# ��� �������� ��������

## ������-������������

�������� ��� � ��������. ��� �������� �� ����� ��������� �� ���� ����� ��� �������.

* bitbucket - � ��� /home �������������� ��������� ���������� ����������
* orientdb - ��� ������� �������
* knnet - ��-��� ���� ����������� ������

## ��������������� �������

��� ����������� � ���������. �������� ������� ������������� �� ���� �������������.
��� �������� �� ����� ��������� �� ���� ����� ���������� � ���������. 

### Orient

������ 3.0RC � orient-labs. ������� � Gremlin � ThinkerPop.

/opt/orientdb/orientdb-community-gremlin-3.0.0RC2 - ���������� ����
/opt/orientdb/orientdb - ���� �� ���������
/opt/orientdb/orient-300rc.tgz - ��������� �����

### nginx

�� ��������� ������ ���� 80. 
��� ���� ������� nginx. 

� /etc/nginx/nginx.conf ����������� ����� ��������:

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

### lighttpd

� /etc/lighttpd/lighttpd.conf �����������:

* �� ����� ����� �������
* ��� ����� ����������� �������
* ���� ���������� ������ �� ������

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

## ����� ����������

� /home/knnet ��������������� ������ bundler.py � ����������� ��� bundler.sh.

� ���������������� crontab ������������ knnet �������:

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

�������� ��������� ������� ������������ � /home/knnet/bundles/backend, ��������� - � /home/knnet/bundles/frontend

## ������

������� ��� ������� ����� � ���� ������������ � deploy/restart. �� ��������� deploy-last.sh ����� �
/home/knnet/bundles, ��� ��������� �����������, �������� ������������� ���������.

��� ������� ���� ���������� (�����/������ � ����� ������) ������������ ��� ������ - ��������������, ������
� �����������. ��� ������� ������� �������� ���� ������� �������� ��� ������ ������.

��� ����������� � ���� ������ ����� crontab:

```
8 * * * * bash /root/deploy-knnet-last.sh >> /root/deploy-knnet-last.out 2>> /root/deploy-knnet-last.err
18 * * * * bash /root/deploy-knnet-last.sh >> /root/deploy-knnet-last.out 2>> /root/deploy-knnet-last.err
28 * * * * bash /root/deploy-knnet-last.sh >> /root/deploy-knnet-last.out 2>> /root/deploy-knnet-last.err
38 * * * * bash /root/deploy-knnet-last.sh >> /root/deploy-knnet-last.out 2>> /root/deploy-knnet-last.err
48 * * * * bash /root/deploy-knnet-last.sh >> /root/deploy-knnet-last.out 2>> /root/deploy-knnet-last.err
58 * * * * bash /root/deploy-knnet-last.sh >> /root/deploy-knnet-last.out 2>> /root/deploy-knnet-last.err
```

� ��, ��� � deploy/restart-root, ���� �������� � root.

�� ����� ���� ������ ������������ ��������, ��� ������ ����������� �� ����� knnet.

## deploy-status

���� ������ ����������� ������, ������� ���������� ������ �������� � ��������� ������.
� ������, ����� ������� ���-���������, �������� ����� ����� ������� "������ �����-�� ������ ������-�� �������� ����-��"
� �������� ���������� ������������.

� ������������ ����� � deploy/control

�� ��������� ����������� ������ �� ����� 9998. 
