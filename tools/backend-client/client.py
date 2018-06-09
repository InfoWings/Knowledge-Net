from configparser import ConfigParser
import requests
import argparse
import uuid
from collections import namedtuple

Subject = namedtuple('Subject', ['id', 'name'])
Aspect = namedtuple('Aspect', ['id', 'name', 'measure', 'base_type', 'version'])

class Config:
    def __init__(self):
        parser = argparse.ArgumentParser(description='knnet load testing tool')
        parser.add_argument('--config', type=str, nargs=1, help='name of config file')
        args = parser.parse_args()
        config = ConfigParser()
        config.read(args.config)
        cfg_backend = config['backend']
        self.host = cfg_backend['host']
        self.port = cfg_backend['port']
        self.username = cfg_backend['username']
        self.password = cfg_backend['password']

        self.path_signin = '/api/access/signIn'


    def url_path(self, path):
        return 'http://{0}:{1}{2}'.format(self.host, self.port, path)

    def data_signin(self):
        return {'username': self.username, 'password': self.password}

    def url_signin(self):
        return self.url_path(self.path_signin)


class Session:
    def __init__(self, cfg):
        self.cfg = cfg
        self.url_signin = cfg.url_signin()
        self.data_signin = cfg.data_signin()

    def login(self):
        r = requests.post(self.url_signin, json = self.data_signin)
        json_response = r.json()
        access_token = json_response['accessToken']
        self.jar = requests.cookies.RequestsCookieJar()
        self.jar.set("x-access-authorization", 'Bearer%20' + access_token)

    def _check_response(self, resp):
        if resp.status_code != 200:
            raise Exception('got unexpected code: {0}. {1}'.format(resp.status_code, resp.text))

    def get(self, path):
        resp = requests.get(self.cfg.url_path(path), cookies = self.jar)
        self._check_response(resp)
        return resp

    def post(self, path, data):
        response = requests.post(self.cfg.url_path(path), json = data, cookies = self.jar)
        self._check_response(response)
        return response


class SubjectManager:
    def __init__(self, session):
        self.session = session

    def get_all(self):
        resp = self.session.get('/api/subject/all')
        if resp.status_code == 200:
            json_resp = resp.json()
            elems = json_resp['subject']
            return [Subject(id = e['id'], name = e['name']) for e in elems]

    def create(self, name):
        return self.session.post('/api/subject/create', {'name': name})

    def delete(self, subject):
        return self.session.post('/api/subject/remove', {'id': subject.id, 'name': subject.name})


class AspectManager:
    def __init__(self, session):
        self.session = session

    def get_all(self):
        resp = self.session.get('/api/aspect/all?orderFields=&direct=')
        if resp.status_code == 200:
            json_resp = resp.json()
            elems = json_resp['aspects']
            return [Aspect(id = e['id'], name = e['name'], measure = e['measure'], base_type = e['baseType'], version = e['version']) for e in elems]

    def create(self, name, base_type):
        return self.session.post('/api/aspect/create', {'name': name, 'baseType': base_type})

    def delete(self, aspect):
        return self.session.post('/api/aspect/remove', {'id': aspect.id, 'name': aspect.name, 'version': aspect.version})


class Beans():
    def __init__(self):
        cfg = Config()
        session = Session(cfg)
        session.login()
        self.subject_manager = SubjectManager(session)
        self.aspect_manager = AspectManager(session)

try:
    BEANS = Beans()

    for s in BEANS.subject_manager.get_all():
        print(s)

    aspects = BEANS.aspect_manager.get_all()
    print(aspects)

    #r = BEANS.aspect_manager.delete(aspects[-1])

    #for i in range(1000):
    #    r = BEANS.subject_manager.create('subj-' + uuid.uuid4().hex)
    #    print(i, r)
except requests.exceptions.ConnectionError:
    print("Could not connect to backend server")
