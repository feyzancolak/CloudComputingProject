

# 📦 Cloud Computing Project — Letter Frequency MapReduce Hadoop

This repository contains the implementation of a **MapReduce** application for counting *letter frequencies* over large text datasets, designed as part of a **cloud computing** course project. The implementation uses **Hadoop** and supports both **Mapper / Reducer** and **Combiner / in-mapper aggregation** variations.

---

## 📝 Overview

The goal of this project is to demonstrate the principles of **distributed data processing** using the **MapReduce paradigm**. We implement a letter frequency counter that can scale to large volumes of text, handling input files in HDFS and aggregating results across distributed nodes.

Key objectives:

* Design and implement **Mappers** that emit individual letters as keys
* Implement **Reducers** that sum counts for each letter
* Introduce **Combiners** or **in-mapper aggregation** to reduce intermediate data transfer
* Deploy and run jobs on **Hadoop clusters**
* Compare performance among different aggregation strategies

---

## ✨ Key Features & Strategies

* **Standard MapReduce Flow**: Mapper → Shuffle/Sort → Reducer
* **Combiner Support**: Use a Combiner to partially aggregate results on the map side
* **In-Mapper Aggregation**: Accumulate counts within the Mapper before emitting (to reduce number of key emissions)
* **Scalability**: Designed to handle large input datasets distributed over HDFS
* **Performance Comparison**: Ability to benchmark time and I/O overhead between vanilla vs combiner vs in-mapper approaches
* **Scripted Automation**: Shell / Python scripts to launch jobs, collect results, and compute metrics

---

## 🏗️ System Architecture & Components

1. **Hadoop MapReduce Jobs**

   * Mapper class (Java) for splitting input lines into letters
   * Reducer class for summing counts
   * Optional Combiner class (same as reducer)
   * In-mapper aggregation variation (maintains an internal map before output)

2. **Driver / Job Runner**

   * Java main class (or script) to configure jobs (input path, output path, number of reducers, etc.)
   * Options to enable or disable combiners or to select in-mapper mode

3. **Cluster / HDFS Setup**

   * Input files are uploaded to HDFS
   * Jobs are submitted to the Hadoop cluster
   * Output is written to HDFS and collected for local analysis

4. **Benchmark & Analysis Scripts**

   * Scripts to run multiple configurations (e.g. with vs without combiner)
   * Collect and parse execution times, I/O stats
   * Generate comparison charts or reports

---

## 📁 Repository Structure

Here’s a typical directory layout (adjust names if your actual project differs):

```
CloudComputingProject/
├── datasets/                  # Sample text files / input data
├── scripts/                   # Shell or Python scripts to run jobs and gather metrics
├── src/                       # Java source code (Mapper, Reducer, Driver, Combiner)
├── output/                    # Job outputs / aggregated results
├── benchmarks/                # Performance measurement data & charts
├── documentation/             # Design report, notes, configuration guides
├── .gitignore
└── README.md
```

---

## 🛠 Development Environment & Technologies

| Component              | Technology / Tool                |
| ---------------------- | -------------------------------- |
| Programming Language   | Java                             |
| Framework              | Hadoop MapReduce                 |
| Execution Environment  | Hadoop cluster (local or remote) |
| Scripting / Automation | Bash, Python                     |
| Data Storage / Input   | HDFS                             |
| Version Control        | Git, GitHub                      |

---

## ⚙ System Configuration & Setup

### 1. Hadoop & HDFS Setup

* Ensure a Hadoop cluster is installed (single-node or multi-node).
* Start HDFS services (NameNode, DataNode) and YARN / MapReduce services.
* Upload your input dataset to HDFS, e.g.:

  ```bash
  hdfs dfs -put local_input.txt /user/youruser/input/
  ```

### 2. Compile & Build

* Use `mvn` or `ant` (or your preferred build tool) to compile Java classes and package your MapReduce job into a `.jar`.

  ```bash
  mvn clean package
  ```
* Ensure dependencies are packaged or available on cluster.

### 3. Launch MapReduce Jobs

* Submit a job with combiner disabled:

  ```bash
  hadoop jar yourproject.jar <DriverClass> /user/youruser/input /user/youruser/output_no_combiner
  ```

* Submit a job with combiner enabled:

  ```bash
  hadoop jar yourproject.jar <DriverClass> /user/youruser/input /user/youruser/output_with_combiner -D mapreduce.job.combiner.class=<YourCombinerClass>
  ```

* Submit with in-mapper aggregation mode (if implemented):

  ```bash
  hadoop jar yourproject.jar <DriverClass> /user/youruser/input /user/youruser/output_inmapmode -D your.custom.mode=inmapper
  ```

* After completion, fetch output locally:

  ```bash
  hdfs dfs -get /user/youruser/output_no_combiner ./local_output
  ```

### 4. Benchmark & Compare

* Use provided scripts to parse logs, extract execution times, and produce comparative charts or tables.
* Compare metrics like total runtime, map output size, shuffle data volume, etc.

---

## 📊 Evaluation & Results

* Measure execution time, throughput, and resource usage for each mode (vanilla, combiner, in-mapper).
* Analyze and compare how much the **combiner** reduces intermediate data size and network traffic.
* Evaluate whether **in-mapper aggregation** yields further improvements over a combiner, considering memory overhead and emission frequency.
* Visualize comparisons (bar charts, line plots) showing tradeoffs between complexity, speed, and resource consumption.

---

## 🚀 Conclusion & Future Work

* The project demonstrates the benefit of **data aggregation techniques** (combiner, in-mapper) in reducing network load and speeding up MapReduce jobs.
* Future enhancements could include:

  * Extending letter frequency to **n-grams** (bigrams, trigrams)
  * Processing multilingual / Unicode text
  * Adapting code for **other MapReduce platforms** (e.g. Spark)
  * Automating auto-tuning (deciding whether to use combiner or in-mapper based on data size)
  * Integrating with cloud-managed Hadoop / big data services

---

## 👥 Contributors & Credits

* **Author:** Feyzan Çolak & Flavio Messina & Noemi Cherchi
* **Course:** Cloud Computing 
* **Supervisor / Instructor:** Carlo Vallati

---


