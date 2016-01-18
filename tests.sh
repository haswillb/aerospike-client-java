#!/bin/bash

export SERVER_HOST=ec2-54-84-63-185.compute-1.amazonaws.com

#echo "100% write 10,000 keys 100,000 transactions"
#time ./benchmarks/run_benchmarks -h $SERVER_HOST -k 10000 -t 100000

#echo "100% write 10,000 keys 100,000 transactions 100 threads"
#time ./benchmarks/run_benchmarks -h $SERVER_HOST -k 10000 -t 100000 -z 100

#echo "50% read 50% update 10,000 keys 100,000 transactions"
#time ./benchmarks/run_benchmarks -h $SERVER_HOST -k 10000 -t 100000  -w RU,50

echo "BIG TEST 50% read update 50% 1,000,000 keys 1,000,000 transactions"
time ./benchmarks/run_benchmarks -h $SERVER_HOST -k 1000000 -t 1000000  -w RU,50

echo "BIGER TEST 50% read update 50% 20,000,000 keys 2,000,000 transactions"
time ./benchmarks/run_benchmarks -h $SERVER_HOST -k 20000000 -t 2000000  -w RU,50

exit 0
