#!/bin/bash

echo "100% write 10,000 keys 100,000 transactions"
time ./benchmarks/run_benchmarks -h 192.168.99.100 -k 10000 -t 100000

echo "100% write 10,000 keys 100,000 transactions 100 threads"
time ./benchmarks/run_benchmarks -h 192.168.99.100 -k 10000 -t 100000 -z 100

#echo "50% read 50% write"
#time ./benchmarks/run_benchmarks -h 192.168.99.100 -k 100 -w RW,50

#echo "50% read 50% update"
#time ./benchmarks/run_benchmarks -h 192.168.99.100 -k 100 -w RU,50