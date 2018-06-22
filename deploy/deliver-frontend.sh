bbd_url="https://${BB_AUTH_STRING}@api.bitbucket.org/2.0/repositories/${BITBUCKET_REPO_OWNER}/${BITBUCKET_REPO_SLUG}/downloads"
ts=`date +'%F-%H-%M-%S'`

ver=`./gradlew properties | grep '^version: ' | sed 's/^version: //' | sed 's|_|-|g'` 
gb=`git rev-parse --abbrev-ref HEAD | sed 's|/|-|g' | sed 's|_|-|g'`

version=${ts}_${ver}_${gb}

front_jar=frontend_${version}.jar

echo $version > frontend/build/web/version.txt
echo $version > frontend/build/web/version_${version}.txt

cd frontend/build
jar cf ../$front_jar web
cd ..

echo "timestamp: $ts"
echo "Going to deliver $front_jar"
ls -l $front_jar

scp -i ~/.ssh/config $front_jar bitbucket@catalog.reflexia.com:$front_jar

curl -X POST $bbd_url --form files=@"$front_jar"
