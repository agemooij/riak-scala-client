#!/bin/sh -ex

DIR=$(basename $0 | xargs dirname)
TMP=$DIR/tmp

(
    mkdir -p $TMP
    cd $TMP
    curl -O https://raw.githubusercontent.com/spawngrid/kerl/master/kerl
    chmod a+x ./kerl
     (for i in 1 2 3 4 5 ; do sleep 120; echo $(date); echo "Installing erlang..."; done) &
    ./kerl build R15B01 r15b01
    ./kerl install r15b01 $(pwd)/r15b01
    . r15b01/activate
    curl -O http://s3.amazonaws.com/downloads.basho.com/riak/2.0/2.0.2/riak-2.0.2.tar.gz
    tar zxvf riak-2.0.2.tar.gz
    cd riak-2.0.2
    make rel
)