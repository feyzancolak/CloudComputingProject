#!/bin/bash

# Check if the Hadoop cluster is running
if ! hdfs dfs -test -d /; then
    	echo "Hadoop cluster is not running"
    	exit 1
fi

# Check if two arguments are provided
if [ $# -ne 2 ]; then
 	echo "Usage: $0 <start> <end>"
  	exit 1
fi

# Assign the arguments to variables
start=$1
end=$2

# Check if both arguments are integers
if ! [[ "$start" =~ ^-?[0-9]+$ ]] || ! [[ "$end" =~ ^-?[0-9]+$ ]]; then
	echo "Both arguments must be integers."
	exit 1
fi

# Check if the start is less than the end
if [ "$start" -ge "$end" ]; then
	echo "The first argument must be less than the second argument."
	exit 1
fi

local_output_dir="$(dirname $0)/data/output"
hdfs_output_dir="CC_project/data/output"
hdfs dfs -test -e $hdfs_output_dir/011

# Loop through the range and remove directories with names that are numbers within the range
for ((i=start; i<=end; i++)); do
	dir_name=$(printf "%03d" "$i")
	if [ -d "$local_output_dir/$dir_name" ]; then
		echo "Removing directory $local_output_dir/$dir_name"
		rm -r "$local_output_dir/$dir_name"
  	fi
	if ! hdfs dfs -test -e "$hdfs_output_dir/$dir_name"; then
		echo "Removing directory $hdfs_output_dir/$dir_name"
		rm -r "$hdfs_output_dir/$dir_name"
	fi
done

echo "Done."
