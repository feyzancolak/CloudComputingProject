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

algorithms=("inMapper" "Combiner")
reducers=(1 2 3)

for algorithm in "${algorithms[@]}"; do
    for reducer in "${reducers[@]}"; do
        echo "Test with $algorithm algorithm and $reducer reducers"
        test_number=$(cat "${project_dir}/test_number.txt")
        test_number_format=$(printf "%03d" "${test_number}")
        printf "Test number %s\n" "${test_number_format}"

        output_dir="${hdfs_project_dir}/data/output/${test_number_format}"
        local_output_dir="${project_dir}/data/output/${test_number_format}"

        hdfs dfs -mkdir -p "${output_dir}"
        printf "HDFS Output directory for frequency %s\n" "${output_dir}"

        mkdir -p ${local_output_dir}/performance
	mkdir ${local_output_dir}/frequency

        # List all files in the HDFS directory
        hdfs dfs -ls "${input_dir}" | awk '{print $8}' | while read -r FILE
        do
            # Check if FILE is not empty
            if [[ ! -z "$FILE" ]]; then
                # Extract the file name from the file path
                FILE_NAME=$(basename "$FILE" .txt)

                if [[ "$FILE_NAME" =~ [Tt]urkish ]]; then
                    language="trk"
                elif [[ "$FILE_NAME" =~ [Ii]talian ]]; then
                    language="it"
                else
                    language="en"
                fi

                echo "Processing file: ${FILE}"
                hadoop jar ${project_dir}/frequencyAlgorithms/${algorithm}-1.0-SNAPSHOT.jar \
                it.unipi.hadoop.RunProcess \
                $FILE \
                $language \
                ${output_dir}/notFormatted/${FILE_NAME} \
		${output_dir}/formatted/${FILE_NAME}.txt \
                $reducer > ${local_output_dir}/performance/${FILE_NAME}.txt 2>&1
            	echo "Letter frequency algorithm completed"
		fi
        done

        hdfs dfs -copyToLocal "${output_dir}/formatted/*" "${local_output_dir}/frequency"
	echo "Letter frequency result files copied in local file system"

	# Increment the run number and save it to the file
        echo $((test_number + 1)) > "${project_dir}/test_number.txt"
        echo "test number incremented"

        # Add file text to specify parameters used
        echo "algorithm: $algorithm" >> ${local_output_dir}/settings.txt
        echo "n_reducers: $reducer" >> ${local_output_dir}/settings.txt
    done
done
