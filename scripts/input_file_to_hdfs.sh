#!/bin/bash

if ! hdfs dfs -test -d /; then
        echo "Hadoop cluster is not running"
        exit 1
else
        echo "Hadoop cluster is running"
fi

set -e

project_dir=$(dirname "$0")
input_dir="${project_dir}/data/input"
hdfs_input_dir="CC_project/data/input"
for file in "${input_dir}"/*; do
    filename=$(basename $file)
	printf "%s\n" $file
    if ! hdfs dfs -test -e "${hdfs_input_dir}"/"${filename}"; then
        hdfs dfs -put $file "${hdfs_input_dir}"
    fi
    printf "HDFS Input file %s\n" "${hdfs_input_dir}"/"${filename}"
done


hdfs dfs -ls hdfs_input_dir
