import sys
import os
import os.path
from shutil import copyfile
from collections import namedtuple
import re

STORAGE_DIR="/home/knnet/bundles"
BB_BUFFER="/home/bitbucket"

SUFFIX = ".jar"

SERVICE = sys.argv[1]
SERVICE_PREFIX = SERVICE + "-"

Artefact = namedtuple('Artefact', ['ts', 'version', 'branch', 'name'])

RE_TS = re.compile(r"[0-9]{4}-[0-9]{2}-[0-9]{2}-[0-9]{2}_[0-9]{2}_[0-9]{2}")

def parse_artefact(name):
    if not name.startswith(SERVICE_PREFIX):
        return None
    if not name.endswith(SUFFIX):
        return None

    artefact_id = drop_both(name, SERVICE_PREFIX, SUFFIX)
    ts_match = re.match(RE_TS, artefact_id)

    if ts_match == None:
        return None

    tail = artefact_id[ts_match.span()[1]:]
    ts_span = ts_match.span()
    ts = artefact_id[ts_span[0]: ts_span[1]]

    if tail == "":
        return Artefact(ts, None, None, name)

    tail_parts = tail[1:].split('_')

    if len(tail_parts) < 2:
        return None

    return Artefact(ts, tail_parts[0], tail_parts[1], name)



def subdir(parent, name):
    return os.path.join(parent, name)

def drop_both(st, prefix, suffix):
    return drop_suffix(drop_prefix(st, prefix), suffix)

def drop_prefix(st, prefix):
    sz = len(prefix)
    if st.startswith(prefix):
        return st[sz:]
    else:
        return st


def drop_suffix(st, suffix):
    sz = len(suffix)
    if st.endswith(suffix):
        return st[:-sz]
    else:
        return st


def read_1(name):
    try:
        with open(name) as f:
            return f.readline()
    except FileNotFoundError:
        return ""

def write_1(name, s):
    with open(name, "w") as f:
        f.write(s + '\n')


def process_instance(artefact):
    if artefact.version is None:
        dir_name = artefact.ts
    else:
        dir_name = artefact.ts + '-' + artefact.version + '_' + artefact.branch
    inst_dir = subdir(SERVICE_DIR, dir_name)
    os.mkdir(inst_dir)
    copyfile(subdir(BB_BUFFER, artefact.name),
             subdir(inst_dir, artefact.name))

SERVICE_DIR = subdir(STORAGE_DIR, SERVICE)
LAST_NAME = subdir(SERVICE_DIR, "LAST")

last = parse_artefact(read_1(LAST_NAME).rstrip())

if last is None:
    print(LAST_NAME + " is broken")
    sys.exit(1)

artefacts = [v for v in [parse_artefact(f) for f in os.listdir(BB_BUFFER)]
               if v is not None]

fresh = [a for a in artefacts if a.ts > last.ts]

fresh_sorted = sorted(fresh, key = lambda e: e.ts)

for a in fresh_sorted:
    print("processing " + str(a))
    process_instance(a)

if len(fresh_sorted) > 0:
    write_1(LAST_NAME, fresh_sorted[-1].name)
else:
    print("Nothing new for " + SERVICE)
