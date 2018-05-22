python3 auto-backup.py $1 > outputdb.txt 2>&1 &
FOO_PID=$!
echo $FOO_PID > db_work_pid.txt

