#!/bin/bash
# Sync contents of the static file directory with S3
ver=$(grep version war/default/WEB-INF/appengine-web.xml | cut -d">" -f2 | cut -d"<" -f1)
echo Uploading to S3 with prefix: $ver
build/s3put -e "$ver" -p `pwd` -b static.sortmybox.com public/

