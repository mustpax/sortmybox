#!/bin/bash
# Sync contents of the static file directory with S3
s3put -p `pwd` -b static.sortmybox.com public/

