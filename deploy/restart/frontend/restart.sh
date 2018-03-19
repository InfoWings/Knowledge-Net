base_dir=$1
id=$2
version_id=`basename $2 .jar | sed 's/frontend-//'`

web_name=$base_dir/$version_id/web
web_link=$base_dir/web

ls -l $jar_name
systemctl stop lighttpd.service
rm $web_link
ln -s $web_name $web_link
echo STARTING $web_name at `date`
systemctl start lighttpd.service
