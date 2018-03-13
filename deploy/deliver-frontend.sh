bbd_url="https://${BB_AUTH_STRING}@api.bitbucket.org/2.0/repositories/${BITBUCKET_REPO_OWNER}/${BITBUCKET_REPO_SLUG}/downloads"
ts=`date +'%F-%H_%M_%S'`

ver=`./gradlew properties | grep '^version: ' | sed 's/^version: //'` 
gb=`git rev-parse --abbrev-ref HEAD`

front_jar=frontend-$ts-$ver_$gb.jar

cd frontend/build
jar cf ../$front_jar web
cd ..

echo "timestamp: $ts"
echo "Going to deliver $back_jar and $front_jar"
ls -l $front_jar

scp -i ~/.ssh/config $front_jar bitbucket@catalog.reflexia.com:$front_jar

curl -X POST $bbd_url --form files=@"$front_jar"
