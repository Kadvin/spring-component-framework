#!/bin/sh

function cal_next() {

    major=`echo $1 | awk -F . '{print $1}'`
    minor=`echo $1 | awk -F . '{print $2}'`
    macro=`echo $1 | awk -F . '{print $3}'`
    macro=`expr $macro + 1`
    echo "$major.$minor.$macro-SNAPSHOT"
}

function element_value() {
  echo `sed -En "/<$1>(.*)<\/$1>/{p;q;}" pom.xml | awk -F '>|<' '{print $3}'`
}

if [ "$1" == "" ]; then
    # 完全根据配置文件来读取
    name=`element_value name`
    if [ `uname -s` == "Darwin" ]; then
      current=`cat pom.xml | grep -Eo '(\d+\.\d+\.\d+-SNAPSHOT)' | head -2 | tail -1`
    else
      current=`cat pom.xml | grep '(?<=<version>)(.*-SNAPSHOT)(?=</version>)' | head -2 | tail -1`
    fi
    release=${current%%-SNAPSHOT}
else
    # 完全根据指定, 第一个参数是当前项目发布版本，第二个参数是parent当前发布版本
    release=$1
    current="$release-SNAPSHOT"
fi

next=`cal_next $release`

echo "#############################################"
echo "### Releasing $name #########################"
echo "#############################################"
echo ""
echo "### $name Current Version = $current"
echo "### $name Release Version = $release"
echo "### $name Next    Version = $next"


OPTIONS="-DreleaseVersion=$release  \
         -DdevelopmentVersion=$next \
         -P dnt with-children -Darguments='-N -DskipTests'"

mvn release:clean

mvn release:prepare $OPTIONS

mvn release:perform $OPTIONS