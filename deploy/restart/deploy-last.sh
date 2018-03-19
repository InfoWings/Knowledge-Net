if diff $1/LAST $1/RUN
then
    echo "Nothing to change"
else
    id=`cat $1/LAST`
    bash $1/prepare.sh $1 $id
    bash $1/restart.sh $1 $id
    bash $1/register.sh $1 $id
fi
