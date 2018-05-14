master_pid=$(head -n 1 "master.pid")
replica_pid=$(head -n 1 "replica.pid")
app_pid=$(head -n 1 "db_work_pid.txt")

echo "master pid " $master_pid 
kill -9 $master_pid
echo "master killed"

echo "replica pid " $replica_pid
kill -9 $replica_pid
echo "replica killed"

echo "app pid " $app_pid
kill -9 $app_pid
echo "app killed"
