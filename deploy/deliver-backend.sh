back_local=backend/build/libs/backend.jar
bbd_url="https://${BB_AUTH_STRING}@api.bitbucket.org/2.0/repositories/${BITBUCKET_REPO_OWNER}/${BITBUCKET_REPO_SLUG}/downloads"
ts=`date +'%F-%H-%M-%S'`

ver=`./gradlew properties | grep '^version: ' | sed 's/^version: //' | sed 's|_|-|g'` 
gb=`git rev-parse --abbrev-ref HEAD | sed 's|/|-|g' | sed 's|/|-|g'`

version=${ts}_${ver}_${gb}

back_jar=backend_${version}.jar

ls -l $back_local
cp $back_local $back_jar

echo "version" $ver
echo "timestamp: $ts"
echo "Going to deliver $back_jar"
ls -l $back_jar

scp -i ~/.ssh/config $back_jar bitbucket@catalog.reflexia.com:$back_jar

curl -X POST $bbd_url --form files=@"$back_jar"
