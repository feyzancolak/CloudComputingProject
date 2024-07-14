#!/bin/bash

# Check if the Hadoop cluster is running
if ! hdfs dfs -test -d /; then
    echo "Hadoop cluster is not running"
    exit 1
fi

# Local project directory
project_dir=$(dirname $0)
hdfs_project_dir="CC_project"
printf "HDFS Project directory %s\n" "${hdfs_project_dir}"

input_dir="${hdfs_project_dir}/data/input"

echo "Test with inMapper algorithm and 1 reducers"
test_number=$(cat "${project_dir}/test_number.txt")
test_number_format=$(printf "%03d" "${test_number}")
printf "Test number %s\n" "${test_number_format}"

output_dir="${hdfs_project_dir}/data/output/${test_number_format}"
local_output_dir="${project_dir}/data/output/${test_number_format}"

hdfs dfs -mkdir -p "${output_dir}"
printf "HDFS Output directory for frequency %s\n" "${output_dir}/frequency"

mkdir -p ${local_output_dir}/performance
mkdir -p ${local_output_dir}/frequency

# Check if FILE is not empty
if [[ ! -z "CC_project/data/input/Seagul_Italian.txt" ]]; then
        # Extract the file name from the file path
        FILE_NAME=$(basename "CC_project/data/input/Seagul_Italian.txt" .txt)

        if [[ "$FILE_NAME" =~ [Tt]urkish ]]; then
            language="trk"
        elif [[ "$FILE_NAME" =~ [Ii]talian ]]; then
            language="it"
        else
            language="en"
        fi

        echo "Processing file: CC_project/data/input/Seagul_Italian.txt"
        hadoop jar ${project_dir}/frequencyAlgorithms/Combiner-1.0-SNAPSHOT.jar \
        it.unipi.hadoop.RunProcess \
        "CC_project/data/input/Seagul_Italian.txt" \
        $language \
        ${output_dir}/notFormatted/${FILE_NAME} \
	${output_dir}/formatted/${FILE_NAME}.txt \
        3 > ${local_output_dir}/performance/${FILE_NAME}.txt 2>&1
fi

# Copy the final file from the HDFS to local
hdfs dfs -copyToLocal "${output_dir}/formatted/*" "${local_output_dir}/frequency"

# Increment the run number and save it to the file
echo $((test_number + 1)) > "${project_dir}/test_number.txt"
echo "test number incremented"

# Add file text to specify parameters used
echo "algorithm: Combiner" >> ${local_output_dir}/settings.txt
echo "n_reducers: 3" >> ${local_output_dir}/settings.txt
