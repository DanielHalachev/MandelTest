#!/bin/bash

# Check if the file path argument is provided
if [ -z "$1" ]; then
  echo "Please provide the path of the output CSV file as an argument."
  exit 1
fi

# Define the output CSV file path
output_file="$1"

# Write the header row to the CSV file
echo "p;g;output1;output2;output3" > "$output_file"

# Array of p values
p_values=(1 2 4 8 12 16 24 32)
# p_values=(1 2)

# Array of g values
# g_values=(1 4 16)
g_values=(1 4 16)

# javac -cp lib/commons-cli-1.5.0.jar:lib/commons-math3-3.6.1.jar src/balancing/constant/*

# Iterate over p and g values
for g in "${g_values[@]}"; do
  for p in "${p_values[@]}"; do
    # Create a variable to store all three outputs
    outputs=""
    
    # Run the Java program three times for each p and g combination
    for i in {1..3}; do
      # Execute the Java program and capture the output
      echo "p=$p;g=$g;i=$i"
      output=$(java -cp lib/commons-cli-1.5.0.jar:lib/commons-math3-3.6.1.jar:src executor.ExecutorServiceMandelTest -p "$p" -g "$g")
      
      # Wait for the previous instance to halt completely
      wait
      
      # Append the output to the variable
      outputs="$outputs;$output"
    done
    
    # Append p, g, and the three outputs to the CSV file
    echo "$p;$g;$outputs" >> "$output_file"
  done
done

echo "Execution complete."

