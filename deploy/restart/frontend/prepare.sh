base_dir=$1
version_id=`basename $2 .jar | sed 's/frontend-//'`

cd $base_dir/$version_id/ ; ls -l ; whoami
sudo -u knnet jar xfv $2
