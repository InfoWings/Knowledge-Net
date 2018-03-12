back_local=backend/build/libs/backend.jar
bbd_url="https://${BB_AUTH_STRING}@api.bitbucket.org/2.0/repositories/${BITBUCKET_REPO_OWNER}/${BITBUCKET_REPO_SLUG}/downloads"
ts=`date +'%F-%H_%M_%S'`
back_jar=backend-$ts.jar

ls -l $back_local
cp $back_local $back_jar

echo "timestamp: $ts"
echo "Going to deliver $back_jar"
ls -l $back_jar

scp -i ~/.ssh/config $back_jar bitbucket@catalog.reflexia.com:$back_jar

curl -X POST $bbd_url --form files=@"$back_jar"
