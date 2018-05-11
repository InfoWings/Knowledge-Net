import subprocess
import threading
import os
import time
import shutil
import sys
from time import sleep
from subprocess import call
from multiprocessing.pool import ThreadPool
from abc import ABC, abstractmethod
from datetime import datetime, timedelta

arguments = sys.argv[1:]
count = len(arguments)

DATABASE="test"
USER="admin"
PASSWORD="admin"
if count == 0:
    SLEEP_MINUTES=60
else:
    SLEEP_MINUTES=int(sys.argv[1])
    print("Interval ", str(SLEEP_MINUTES), "minutes")

def start_master():
    logfile = open("master-log.txt", 'ab')
    proc = subprocess.Popen(os.getcwd() + "/bin/dserver.sh", stdout=logfile, stderr=logfile, bufsize=1)
    print("Start master with id ", proc.pid)
    pid_file = open("master.pid", "w")
    pid_file.write(str(proc.pid) + '\n')
    pid_file.close()
    sleep(30)


def start_replica():
    proc = subprocess.Popen(os.getcwd() + "/dst/bin/dserver.sh", stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    print("Start replica with id ", proc.pid)
    pid_file = open("replica.pid", "w")
    pid_file.write(str(proc.pid) + '\n')
    pid_file.close() 
    sleep(30)
    return proc.pid
    

class BackupWriter:
    def __init__(self, backup_folder, time_to_folder_template, time_to_full_path_template):
        self.time_to_folder_template = time_to_folder_template
        self.time_to_full_path_template = time_to_full_path_template
        self.backup_folder = backup_folder
        
    def write(self, current_time, backup_file):
        folder = self.backup_folder + time.strftime(self.time_to_folder_template, current_time)
        if not os.path.exists(folder):
            os.makedirs(folder)
        full_path = self.backup_folder + time.strftime(self.time_to_full_path_template, current_time) + ".zip"
        shutil.copy(backup_file, full_path)


class BackupCleaner:
    def __init__(self, backup_folder, time_to_folder_template):
        self.backup_folder = backup_folder
        self.time_to_folder_template = time_to_folder_template
    
    def clean(self, current_time):
        folder = self.backup_folder + time.strftime(self.time_to_folder_template, current_time)
        if os.path.exists(folder):
            shutils.rmtree(folder)


class WorkerFacade:
    def __init__(self, backup_folder):
        # init writers
        self.month_writer = BackupWriter(backup_folder, "%y", "%y/full-%B")
        self.day_writer = BackupWriter(backup_folder, "%y/%B", "%y/%B/full-%d") 
        self.hour_writer = BackupWriter(backup_folder, "%y/%B/%d", "%y/%B/%d/%H-%M")
        
        #init cleaners
        self.day_cleaner = BackupCleaner(backup_folder, "%y/%B/%d")
        self.month_cleaner = BackupCleaner(backup_folder, "%y/%B")
        
        self.backup_folder = backup_folder

    def process(self, current_time):
        backup_file = self.backup_folder + "last.zip"
        year, month, day, hour, minute = map(lambda x: int(x), time.strftime("%Y,%m,%d,%H,%M").split(','))
        dt = datetime(year, month, day, hour, minute) 

        self.hour_writer.write(current_time, backup_file)
        if hour == 23:
            self.day_writer.write(current_time, backup_file)
            self.day_cleaner.clean(dt - timedelta(days=1))

        tomorrow = dt + timedelta(days=1)
        if dt.month != tomorrow.month:
            self.month_writer.write(current_time, backup_file)
            self.month_cleaner.clean(dt - timedelta(months=1))

    def backup(self, current_time, last_pid): 
        call(["kill", "-9", str(last_pid)])
        sleep(15)
        call([os.getcwd() + "/dst/bin/backup.sh", f"plocal:../databases/{DATABASE}", USER, PASSWORD, self.backup_folder + "last.zip"])
        return start_replica()


def do_work(last_pid, worker_facade):
    current_time = time.gmtime()
    new_pid = worker_facade.backup(current_time, last_pid)
    worker_facade.process(current_time)
    return new_pid


start_master()
last_pid = start_replica()
pool = ThreadPool(processes=1)
async_result = None

BACKUP_PREFIX = os.getcwd() + "/backups/"
if not os.path.exists(BACKUP_PREFIX):
    os.makedirs(BACKUP_PREFIX)

worker_facade = WorkerFacade(BACKUP_PREFIX)

while(True):
    sleep(SLEEP_MINUTES * 60)
    
    if (async_result != None):
        last_pid = async_result.get()
    
    async_result = pool.apply_async(do_work, (last_pid, worker_facade))
